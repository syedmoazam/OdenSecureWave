import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Platform } from 'react-native';
import DatabaseService from '@/services/databaseService';
import DeviceAdminManager from '@/services/DeviceAdminManager';
import NotificationService from '@/services/notificationService';
import { MonitoredDeviceData } from '@/types/device';
import { QUERY_KEYS, QUERY_CONFIG } from '@/constants/storageKeys';
import { navigationRef, reset } from '@/services/navigationService';
import routes from '@/constants/routes';

// Helper function to fetch device data
const fetchDeviceData = async (): Promise<MonitoredDeviceData | null> => {
  if (Platform.OS !== 'android') {
    throw new Error('Device monitoring is only available on Android devices');
  }
  
  // Get IMEI directly without caching
  const deviceAdminManager = DeviceAdminManager.getInstance();
  const imei = await deviceAdminManager.getDeviceIMEI();
  
  // Only cache database service calls
  const databaseService = DatabaseService.getInstance();
  return await databaseService.getMonitoredDeviceData(imei);
};

interface CompanyInfo {
  id: string;
  name: string;
  branch: string;
}

interface DeviceDataContextType {
  // State
  isMonitoringEnabled: boolean;
  deviceData: MonitoredDeviceData | null;
  loading: boolean;
  error: string | null;
  
  // Admin state
  isDeviceAdminEnabled: boolean;
  
  // Setup state
  isSettingUp: boolean;
  
  // Service state
  isServiceEnabled: boolean;
  isServiceConfigured: boolean;
  
  // Initialization state
  isInitialized: boolean;
  
  // Actions
  setupDevice: (company: CompanyInfo) => Promise<void>;
  refreshData: () => void;
}

const DeviceDataContext = createContext<DeviceDataContextType | undefined>(undefined);

interface DeviceDataProviderProps {
  children: ReactNode;
}

export const DeviceDataProvider: React.FC<DeviceDataProviderProps> = ({ children }) => {
  const [isSettingUp, setIsSettingUp] = useState<boolean>(false);
  const queryClient = useQueryClient();
  
  const databaseService = DatabaseService.getInstance();
  const deviceAdminManager = DeviceAdminManager.getInstance();
  const notificationService = NotificationService.getInstance();

  // Query to check if device admin is enabled
  const {
    data: isDeviceAdminEnabled,
    isLoading: isCheckingAdmin,
  } = useQuery({
    queryKey: ['device', 'admin', 'enabled'],
    queryFn: async () => {
      if (Platform.OS !== 'android') {
        return false;
      }
      return await deviceAdminManager.isDeviceAdminEnabled();
    },
    staleTime: QUERY_CONFIG.STALE_TIME.SHORT,
    gcTime: QUERY_CONFIG.CACHE_TIME.SHORT,
    retry: QUERY_CONFIG.RETRY.DEFAULT,
    enabled: Platform.OS === 'android',
  });

  // Query to check if WorkManager service is enabled
  const {
    data: serviceStatus,
    isLoading: isCheckingService,
  } = useQuery({
    queryKey: ['device', 'service', 'status'],
    queryFn: async () => {
      if (Platform.OS !== 'android') {
        return { enabled: false, workScheduled: false };
      }
      return await deviceAdminManager.isPeriodicServiceEnabled();
    },
    staleTime: QUERY_CONFIG.STALE_TIME.SHORT,
    gcTime: QUERY_CONFIG.CACHE_TIME.SHORT,
    retry: QUERY_CONFIG.RETRY.DEFAULT,
    enabled: Platform.OS === 'android' && isDeviceAdminEnabled === true,
  });

  // Query for device data (only if device admin is enabled and service is configured)
  const {
    data: deviceData,
    isLoading: isLoadingDeviceData,
    error: deviceError,
    refetch: refetchDeviceData,
  } = useQuery({
    queryKey: QUERY_KEYS.DEVICE_DATA,
    queryFn: fetchDeviceData,
    staleTime: QUERY_CONFIG.STALE_TIME.SHORT,
    gcTime: QUERY_CONFIG.CACHE_TIME.MEDIUM,
    retry: QUERY_CONFIG.RETRY.DEFAULT,
    enabled: Platform.OS === 'android' && 
             isDeviceAdminEnabled === true && 
             serviceStatus?.enabled === true,
    refetchInterval: QUERY_CONFIG.REFETCH_INTERVAL.DEVICE_STATUS
  });

  // Calculate derived state
  const isMonitoringEnabled = deviceData?.isEnabled ?? false;
  const isLoading = isCheckingAdmin || isCheckingService || isLoadingDeviceData;
  const error = deviceError?.message || null;
  
  // Service state
  const isServiceEnabled = serviceStatus?.enabled ?? false;
  const isServiceConfigured = serviceStatus?.workScheduled ?? false;
  
  // Calculate initialization and navigation state
  const isInitialized = !isLoading && 
                       isDeviceAdminEnabled !== undefined && 
                       (Platform.OS !== 'android' || serviceStatus !== undefined);

  // Mutation for setting up device
  const setupDeviceMutation = useMutation({
    mutationFn: async (company: CompanyInfo): Promise<MonitoredDeviceData> => {
      if (isMonitoringEnabled) {
        throw new Error('Device is already being monitored');
      }

      // Only proceed on Android devices
      if (Platform.OS !== 'android') {
        throw new Error('Device setup is only available on Android devices');
      }

      // Get device IMEI
      const imei = await deviceAdminManager.getDeviceIMEI();
      console.log('Device IMEI:', imei);

      // Get device info
      const deviceInfo = await deviceAdminManager.getDeviceInfo();
      console.log('Device Info:', deviceInfo);

      // Get FCM token
      const fcmToken = await notificationService.getFCMToken();
      if (!fcmToken) {
        throw new Error('Unable to get FCM token');
      }
      // Create monitored device data
      const monitoredDeviceData: MonitoredDeviceData = {
        createdAt: new Date().toISOString(),
        deviceId: imei,
        deviceName: deviceInfo.model,
        manufacturer: deviceInfo.manufacturer,
        fcmToken: fcmToken,
        isEnabled: true, // Default to enabled when setting up
        status: 'active', // Default status is active
        company: {
          id: company.id,
          name: company.name,
          branch: company.branch
        }
      };

      // Create record in Firebase
      await databaseService.createMonitoredDevice(monitoredDeviceData);

      // Configure Device Owner privileges for better service reliability
      try {
        await deviceAdminManager.configureDeviceOwnerPrivileges();
        console.log('Device Owner privileges configured successfully');
      } catch (privilegeError) {
        console.warn('Failed to configure Device Owner privileges:', privilegeError);
        // Don't throw here, as the main setup was successful
      }

      // Start the WorkManager periodic service
      try {
        await deviceAdminManager.startPeriodicService();
        console.log('Periodic service started successfully');
      } catch (serviceError) {
        console.error('Failed to start periodic service:', serviceError);
        // Don't throw here, as the main setup was successful
        // The user can manually enable the service later
      }

      return monitoredDeviceData;
    },
    onMutate: () => {
      setIsSettingUp(true);
    },
    onSuccess: (data) => {
      queryClient.setQueryData(QUERY_KEYS.DEVICE_DATA, data);
      // Invalidate service status to refresh the UI
      queryClient.invalidateQueries({ queryKey: ['device', 'service', 'status'] });
    },
    onError: (error) => {
      console.error('Error during device setup:', error);
    },
    onSettled: () => {
      setIsSettingUp(false);
    },
  });

  // Log device status for debugging
  useEffect(() => {
    // Only navigate if monitoring is enabled and we have device data
    if (isInitialized && deviceData && deviceData.isEnabled && navigationRef.current) {
      const currentStatus = deviceData.status;
      const currentRoute = navigationRef.current.getCurrentRoute()?.name;
      
      console.log('Device status:', currentStatus, 'Current route:', currentRoute);
      
      // Prevent infinite loops by checking if we're already on the correct screen
      const shouldNavigateToHome = currentStatus === 'active' && currentRoute !== routes.APP_STACK.HOME;
      const shouldNavigateToEmergency = currentStatus === 'lock' && currentRoute !== routes.APP_STACK.EMERGENCY;
      const deviceAdminManager = DeviceAdminManager.getInstance();

      if (shouldNavigateToHome) {
        console.log('Device is active - navigating to HomeScreen');
        reset(routes.APP_STACK.HOME, {});
        deviceAdminManager.unlockApp();
        deviceAdminManager.closeApp();

      } else if (shouldNavigateToEmergency) {
        console.log('Device is locked - navigating to EmergencyScreen');
        reset(routes.APP_STACK.EMERGENCY, {});
        deviceAdminManager.lockApp();
      }
    }
  }, [deviceData, isInitialized]);

  const setupDevice = async (company: CompanyInfo): Promise<void> => {
    await setupDeviceMutation.mutateAsync(company);
  };

  const refreshData = () => {
    refetchDeviceData();
  };

  const contextValue: DeviceDataContextType = {
    // State
    isMonitoringEnabled,
    deviceData: deviceData || null,
    loading: isLoading,
    error,
    
    // Admin state
    isDeviceAdminEnabled: isDeviceAdminEnabled ?? false,
    
    // Setup state
    isSettingUp: isSettingUp || setupDeviceMutation.isPending,
    
    // Service state
    isServiceEnabled,
    isServiceConfigured,
    
    // Initialization state
    isInitialized,
    
    // Actions
    setupDevice,
    refreshData,
  };

  return (
    <DeviceDataContext.Provider value={contextValue}>
      {children}
    </DeviceDataContext.Provider>
  );
};

// Custom hook to use the DeviceDataContext
export const useDeviceData = (): DeviceDataContextType => {
  const context = useContext(DeviceDataContext);
  if (context === undefined) {
    throw new Error('useDeviceData must be used within a DeviceDataProvider');
  }
  return context;
};

export default DeviceDataContext;
