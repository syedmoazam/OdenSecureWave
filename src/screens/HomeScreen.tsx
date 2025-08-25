import {FlatList, StyleSheet, Text, View, Alert, NativeMethods, NativeModules, TouchableOpacity} from 'react-native';
import SvgIcon from "@/components/SvgIcon.tsx";
import Icons from "@/constants/icons.ts";
import Metrics from "@/utils/Metrics.ts";
import {Colors} from "@/themes/Colors.ts";
import Fonts from "@/themes/Fonts.ts";
import AppButton from "@/components/AppButton.tsx";
import BottomSheet, {BottomSheetRef} from "@/components/BottomSheet.tsx";
import {useRef, useState, useEffect, useCallback} from "react";
import ScreenLayout from "@/layouts/ScreenLayout.tsx";
import { useCompanyData } from '@/hooks/useCompanyData';
import { useDeviceData } from '@/hooks/useDeviceData';
import DeviceAdminManager from '@/services/DeviceAdminManager';


interface ICompany {
  id: string;
  name: string;
  branch: string;
}

export function HomeScreen() {
  const { 
    isMonitoringEnabled, 
    isSettingUp, 
    setupDevice, 
    deviceData, 
    isServiceEnabled,
    isServiceConfigured 
  } = useDeviceData();
  const [company, setCompany] = useState<ICompany | null>(null);
  const bottomSheetRef = useRef<BottomSheetRef>(null);
  const [debugInfo, setDebugInfo] = useState<any>(null);
  const deviceAdminManager = DeviceAdminManager.getInstance();

  const onPressBranchCard = () => {
    if (bottomSheetRef.current && !isMonitoringEnabled && !isServiceEnabled) {
      bottomSheetRef.current.open();
    }
  }

  const onSelectCompany = (companyToSelect: ICompany) => {
    setCompany(companyToSelect);
    if (bottomSheetRef.current) {
      bottomSheetRef.current.close();
    }
  }

  const handleStartSetup = async () => {
    if (!company || isMonitoringEnabled || isServiceEnabled) {
      return;
    }

    try {
      await setupDevice(company);

      Alert.alert(
        'Setup Complete',
        'Device has been successfully registered for monitoring. Background service has been enabled.',
        [{ text: 'OK' }]
      );

    } catch (error: any) {
      console.error('Error during device setup:', error);
      Alert.alert(
        'Setup Failed',
        error.message || 'Failed to setup device monitoring. Please try again.',
        [{ text: 'OK' }]
      );
    }
  }

  // Debug methods for testing WorkManager
  const debugWorkManager = async () => {
    try {
      console.log('🔍 Starting WorkManager Debug...');
      
      // Test 1: Check if DeviceAdminModule is available
      console.log('DeviceAdminModule available:', !!DeviceAdminManager);
      
      // Test 2: Check current service status
      const serviceStatus = await deviceAdminManager.isPeriodicServiceEnabled();
      console.log('Service Status:', serviceStatus);
      
      // Test 3: Check device admin status
      const isDeviceAdmin = await deviceAdminManager.isDeviceAdminEnabled();
      console.log('Device Admin Enabled:', isDeviceAdmin);
      
      // Test 4: Check device owner status
      const isDeviceOwner = await deviceAdminManager.isDeviceOwner();
      console.log('Is Device Owner:', isDeviceOwner);
      
      // Test 5: Get detailed privilege status
      const privilegeStatus = await deviceAdminManager.getServicePrivilegeStatus();
      console.log('Privilege Status:', privilegeStatus);
      
      const debugData = {
        serviceStatus,
        isDeviceAdmin,
        isDeviceOwner,
        privilegeStatus,
        timestamp: new Date().toLocaleString()
      };
      
      setDebugInfo(debugData);
      
      Alert.alert('Debug Complete', 'Check console logs for detailed information');
      
    } catch (error: any) {
      console.error('Debug Error:', error);
      Alert.alert('Debug Failed', error.message || 'Unknown error occurred');
    }
  };

  const testStartService = async () => {
    try {
      console.log('🚀 Testing Service Start...');
      const result = await deviceAdminManager.startPeriodicService();
      console.log('Start Service Result:', result);
      Alert.alert('Service Start', result);
    } catch (error: any) {
      console.error('Start Service Error:', error);
      Alert.alert('Start Service Failed', error.message || 'Unknown error');
    }
  };

  const testStopService = async () => {
    try {
      console.log('⏹️ Testing Service Stop...');
      const result = await deviceAdminManager.stopPeriodicService();
      console.log('Stop Service Result:', result);
      Alert.alert('Service Stop', result);
    } catch (error: any) {
      console.error('Stop Service Error:', error);
      Alert.alert('Stop Service Failed', error.message || 'Unknown error');
    }
  };

  const testInitializeService = async () => {
    try {
      console.log('🔄 Testing Service Initialize...');
      const result = await deviceAdminManager.initializeServiceOnStartup();
      console.log('Initialize Service Result:', result);
      Alert.alert('Service Initialize', result);
    } catch (error: any) {
      console.error('Initialize Service Error:', error);
      Alert.alert('Initialize Service Failed', error.message || 'Unknown error');
    }
  };

  const testBootFirebaseCheck = async () => {
    try {
      console.log('🔄 Testing Boot Firebase Check...');
      
      const result = await deviceAdminManager.testBootFirebaseCheck();
      console.log('Boot Firebase Check Result:', result);
      Alert.alert('Boot Firebase Check', result);
    } catch (error: any) {
      console.error('Boot Firebase Check Error:', error);
      Alert.alert('Boot Firebase Check Failed', error.message || 'Unknown error');
    }
  };

  const simulateBootCompleted = async () => {
    try {
      console.log('🔄 Simulating Boot Completed...');
      
      const result = await deviceAdminManager.simulateBootCompleted();
      console.log('Simulate Boot Result:', result);
      Alert.alert('Boot Simulation', result);
    } catch (error: any) {
      console.error('Simulate Boot Error:', error);
      Alert.alert('Boot Simulation Failed', error.message || 'Unknown error');
    }
  };

  useEffect(() => {
    if (deviceData?.company) {
      setCompany(deviceData.company)
    }
  }, [deviceData])

  // Determine what to show based on service and monitoring status
  const showSetup = !isServiceEnabled && !isMonitoringEnabled;
  const showMonitoring = isServiceEnabled && isMonitoringEnabled;
  const showCompanySelection = !company && showSetup;

  return (
    <ScreenLayout>
      <View style={styles.container}>
        {showMonitoring ? (
          // Show monitoring active status
          <View style={[styles.card, styles.lightGreenBg]}>
            <View style={styles.iconTextContainer}>
              <View style={styles.iconContainer}>
                <SvgIcon
                  color={Colors.GREEN}
                  name={Icons.CIRCLE_CHECK}
                  size={Metrics.icons.small}
                />
              </View>
              <Text style={styles.greenMediumLabel}>Monitoring Active</Text>
            </View>
            <Text style={styles.blueText}>
              Your device is being monitored and the background service is running. 
              {company && ` Connected to ${company.name}-${company.branch}.`}
            </Text>
          </View>
        ) : showCompanySelection ? (
          // Show company selection flow
          <>
            <View style={[styles.card, styles.lightOrangeBg]}>
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.ORANGE}
                    name={Icons.BUILDING}
                    size={Metrics.icons.small}
                  />
                </View>
                <Text style={styles.orangeBoldLabel}>Select Your Company</Text>
              </View>
              <Text style={styles.orangeText}>
                Please select your company and branch to begin the device setup process.
              </Text>
            </View>
            <AppButton
              onPress={onPressBranchCard}
              style={[styles.card, styles.lightBlueBg]}
            >
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.BLUE}
                    size={Metrics.icons.small}
                    name={Icons.BRIEFCASE}
                  />
                </View>
                <Text style={styles.blueMediumLabel}>Select Company & Branch</Text>
                <AppButton style={styles.branchIconButton}>
                  <SvgIcon
                    color={Colors.BLUE}
                    size={Metrics.icons.tiny}
                    name={Icons.CHEVRON_RIGHT}
                  />
                </AppButton>
              </View>
            </AppButton>
          </>
        ) : company && showSetup ? (
          // Show setup flow when company is selected but service not enabled
          <>
            <View style={[styles.card, styles.lightBlueBg]}>
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.BLUE}
                    name={Icons.SECURITY}
                    size={Metrics.icons.small}
                  />
                </View>
                <Text style={styles.blueBoldLabel}>Device Setup Required</Text>
              </View>
              <Text style={styles.blueText}>
                To ensure your device security and management capabilities, we need to complete the setup process. This will enable important security features and device management controls.
              </Text>
            </View>
            <AppButton
              onPress={onPressBranchCard}
              style={[styles.card, styles.lightGreenBg]}
            >
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.GREEN}
                    size={Metrics.icons.small}
                    name={Icons.CIRCLE_CHECK}
                  />
                </View>
                <Text style={styles.greenMediumLabel}>{company.name}-{company.branch}</Text>
                <AppButton style={styles.branchIconButton}>
                  <SvgIcon
                    color={Colors.GREEN}
                    size={Metrics.icons.tiny}
                    name={Icons.CHEVRON_RIGHT}
                  />
                </AppButton>
              </View>
            </AppButton>
            <View style={[styles.card, styles.lightPurpleBg]}>
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    name={Icons.GEAR}
                    color={Colors.PURPLE}
                    size={Metrics.icons.small}
                  />
                </View>
                <Text style={styles.purpleBoldLabel}>Device Setup</Text>
              </View>
              <Text style={styles.purpleText}>
                Complete the device setup to enable security features and management controls.{"\n\n"}
              </Text>
              <AppButton 
                disabled={isSettingUp} 
                style={styles.setupBtn}
                onPress={handleStartSetup}
              >
                <Text style={styles.whiteMediumText}>
                  {isSettingUp ? 'Setting Up...' : 'Start Setup'}
                </Text>
              </AppButton>
            </View>
          </>
        ) : (
          // Fallback: show basic status
          <View style={[styles.card, styles.lightBlueBg]}>
            <View style={styles.iconTextContainer}>
              <View style={styles.iconContainer}>
                <SvgIcon
                  color={Colors.BLUE}
                  name={Icons.SECURITY}
                  size={Metrics.icons.small}
                />
              </View>
              <Text style={styles.blueBoldLabel}>Device Status</Text>
            </View>
            <Text style={styles.blueText}>
              Service Enabled: {isServiceEnabled ? 'Yes' : 'No'}{'\n'}
              Service Configured: {isServiceConfigured ? 'Yes' : 'No'}{'\n'}
              Monitoring: {isMonitoringEnabled ? 'Active' : 'Inactive'}
            </Text>
          </View>
        )}

        {/* Debug Panel - Remove this in production */}
        <View style={[styles.card, styles.debugPanel]}>
          <Text style={styles.debugTitle}>🔧 WorkManager Debug Panel</Text>
          
          <View style={styles.debugButtonContainer}>
            <TouchableOpacity style={styles.debugButton} onPress={debugWorkManager}>
              <Text style={styles.debugButtonText}>Debug Status</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.debugButton} onPress={testStartService}>
              <Text style={styles.debugButtonText}>Start Service</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.debugButton} onPress={testStopService}>
              <Text style={styles.debugButtonText}>Stop Service</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.debugButton} onPress={testInitializeService}>
              <Text style={styles.debugButtonText}>Initialize</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.debugButton} onPress={testBootFirebaseCheck}>
              <Text style={styles.debugButtonText}>Test Boot Check</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={styles.debugButton} onPress={simulateBootCompleted}>
              <Text style={styles.debugButtonText}>Simulate Boot</Text>
            </TouchableOpacity>
          </View>
          
          {debugInfo && (
            <View style={styles.debugInfo}>
              <Text style={styles.debugInfoTitle}>Last Debug Result:</Text>
              <Text style={styles.debugInfoText}>
                Service Enabled: {debugInfo.serviceStatus?.enabled ? '✅' : '❌'}{'\n'}
                Work Scheduled: {debugInfo.serviceStatus?.workScheduled ? '✅' : '❌'}{'\n'}
                Device Admin: {debugInfo.isDeviceAdmin ? '✅' : '❌'}{'\n'}
                Device Owner: {debugInfo.isDeviceOwner ? '✅' : '❌'}{'\n'}
                Time: {debugInfo.timestamp}
              </Text>
            </View>
          )}
        </View>

        <BottomSheet ref={bottomSheetRef}>
          <CompanySelector 
            onSelectCompany={onSelectCompany}
          />
        </BottomSheet>
      </View>
    </ScreenLayout>
  );
}

interface ICompanySelectorProps {
  onSelectCompany: (company: ICompany) => void;
}

const CompanySelector = ({ 
  onSelectCompany, 
}: ICompanySelectorProps) => {
  const { data: companyData, loading, error, refreshData } = useCompanyData();
  return (
    <View style={styles.container}>
      <View style={styles.iconTextContainer}>
        <View style={[styles.iconContainer, styles.companySelectorIconContainer]}>
          <SvgIcon color={Colors.BLUE} name={Icons.BUILDING} size={Metrics.icons.small}/>
        </View>
        <Text style={styles.companySelectorLabel}>Select Company</Text>
      </View>
      <Text style={styles.companySelectorText}>
        Choose your company and branch to proceed with the setup
      </Text>
      
      {error && (
        <View style={styles.errorContainer}>
          <Text style={styles.errorText}>{error}</Text>
          <Text style={styles.retryText} onPress={refreshData}>
            Tap to retry
          </Text>
        </View>
      )}
      
      <FlatList
        style={styles.listStyle}
        contentContainerStyle={styles.companyListContentContainer}
        data={companyData}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <AppButton onPress={() => onSelectCompany(item)} style={styles.companyCard}>
            <View style={styles.companyIconContainer}>
              <SvgIcon color={Colors.DARK} size={Metrics.icons.tiny} name={Icons.BUILDING} />
            </View>
            <View style={styles.companyDetailsContainer}>
              <Text style={styles.companyName}>{item.name}</Text>
              <Text style={styles.branchName}>{item.branch}</Text>
            </View>
            <View style={styles.rightIconContainer}>
              <SvgIcon size={Metrics.icons.tiny} color={Colors.CHARCOAL_GREY} name={Icons.CHEVRON_RIGHT} />
            </View>
          </AppButton>
        )}
        refreshing={loading}
        onRefresh={refreshData}
      />
    </View>
  )
}

const styles = StyleSheet.create({
  // Containers
  container: {
    flex: 1,
    rowGap: Metrics.verticalScale(16)
  },
  card: {
    paddingHorizontal: Metrics.scale(16),
    paddingVertical: Metrics.verticalScale(16),
    borderRadius: Metrics.scale(14),
    rowGap: Metrics.verticalScale(12)
  },
  companyCard: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: Metrics.scale(10),
    backgroundColor: Colors.LIGHT_GRAY,
    paddingVertical: Metrics.verticalScale(16),
    paddingHorizontal: Metrics.verticalScale(16),
    borderRadius: Metrics.scale(10),
    borderWidth: 1,
    borderColor: Colors.GRAY,
  },
  companyDetailsContainer: {
    rowGap: Metrics.scale(4),
  },
  companyIconContainer: {
    borderWidth: 1,
    borderColor: Colors.GRAY,
    backgroundColor: Colors.WHITE,
    paddingHorizontal: Metrics.scale(10),
    paddingVertical: Metrics.verticalScale(10),
    borderRadius: Metrics.scale(10),
  },
  companyListContentContainer: { rowGap: Metrics.verticalScale(8) },
  companyListStyle: { flex: 1 },
  companySelectorIconContainer: {
    backgroundColor: Colors.LIGHT_BLUE
  },
  iconContainer: {
    backgroundColor: Colors.WHITE,
    borderRadius: Metrics.scale(16),
    paddingHorizontal: Metrics.scale(12),
    paddingVertical: Metrics.verticalScale(12)
  },
  iconTextContainer: {
    flexDirection: "row",
    alignItems: "center",
    gap: Metrics.scale(16)
  },
  listStyle: { flex: 1 },
  rightIconContainer: {
    flexGrow: 1,
    flexDirection: "row",
    justifyContent: "flex-end",
  },
  branchIconButton: {
    flexDirection: "row",
    justifyContent: "flex-end",
    flexGrow: 1,
  },
  setupBtn: {
    alignItems: "center",
    paddingVertical: Metrics.verticalScale(12),
    backgroundColor: Colors.PURPLE,
    borderRadius: Metrics.scale(10),
  },
  buttonContainer: {
    flexDirection: "row",
    gap: Metrics.scale(8),
    marginTop: Metrics.verticalScale(8),
  },
  controlBtn: {
    flex: 1,
    alignItems: "center",
    paddingVertical: Metrics.verticalScale(12),
    borderRadius: Metrics.scale(10),
    marginHorizontal: Metrics.scale(4),
  },

  // Text
  blueBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.BLUE),
  blueMediumLabel: Fonts.Medium(Fonts.Size.normal, Colors.BLUE),
  greenMediumLabel: Fonts.Medium(Fonts.Size.normal, Colors.GREEN),
  companySelectorLabel: Fonts.Bold(Fonts.Size.xLarge, Colors.BLUE),
  companyName: Fonts.Bold(Fonts.Size.normal, Colors.DARK),
  orangeBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.ORANGE),
  purpleBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.PURPLE),
  redBoldLabel: Fonts.Bold(Fonts.Size.large, "#FF6B6B"),
  blueText: Fonts.Regular(Fonts.Size.xSmall, Colors.BLUE),
  companySelectorText: Fonts.Regular(Fonts.Size.xSmall, Colors.BLUE),
  branchName: Fonts.Regular(Fonts.Size.xxSmall, Colors.CHARCOAL_GREY),
  orangeText: Fonts.Regular(Fonts.Size.xSmall, Colors.ORANGE),
  purpleText: Fonts.Regular(Fonts.Size.xSmall, Colors.PURPLE),
  redText: Fonts.Regular(Fonts.Size.xSmall, "#FF6B6B"),
  whiteMediumText: Fonts.Medium(Fonts.Size.normal, Colors.WHITE),

  // Backgrounds
  lightBlueBg: { backgroundColor: Colors.LIGHT_BLUE },
  lightOrangeBg: { backgroundColor: Colors.LIGHT_ORANGE },
  lightPurpleBg: { backgroundColor: Colors.LIGHT_PURPLE },
  lightGreenBg: { backgroundColor: Colors.LIGHT_GREEN },
  lightRedBg: { backgroundColor: "#FFE6E6" },
  errorContainer: {
    paddingHorizontal: Metrics.scale(20),
    paddingVertical: Metrics.verticalScale(12),
    backgroundColor: Colors.RED + '20',
    marginBottom: Metrics.verticalScale(8),
  },
  errorText: {
    ...Fonts.Regular(Fonts.Size.small),
    textAlign: 'center',
    color: Colors.RED,
    marginBottom: Metrics.verticalScale(4),
  },
  retryText: {
    ...Fonts.Medium(Fonts.Size.small),
    color: Colors.PRIMARY_BLUE,
    textDecorationLine: 'underline',
    textAlign: 'center',
  },

  // Debug Panel Styles
  debugPanel: {
    backgroundColor: '#2D3748',
    borderWidth: 2,
    borderColor: '#4A5568',
  },
  debugTitle: {
    ...Fonts.Bold(Fonts.Size.normal),
    color: '#E2E8F0',
    marginBottom: Metrics.verticalScale(12),
    textAlign: 'center',
  },
  debugButtonContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Metrics.scale(8),
    justifyContent: 'space-between',
    marginBottom: Metrics.verticalScale(12),
  },
  debugButton: {
    backgroundColor: '#4C51BF',
    paddingHorizontal: Metrics.scale(12),
    paddingVertical: Metrics.verticalScale(8),
    borderRadius: Metrics.scale(6),
    flex: 1,
    minWidth: '45%',
    alignItems: 'center',
  },
  debugButtonText: {
    ...Fonts.Medium(Fonts.Size.xSmall),
    color: 'white',
  },
  debugInfo: {
    backgroundColor: '#1A202C',
    padding: Metrics.scale(12),
    borderRadius: Metrics.scale(8),
    marginTop: Metrics.verticalScale(8),
  },
  debugInfoTitle: {
    ...Fonts.Bold(Fonts.Size.small),
    color: '#63B3ED',
    marginBottom: Metrics.verticalScale(8),
  },
  debugInfoText: {
    ...Fonts.Regular(Fonts.Size.xSmall),
    color: '#E2E8F0',
    lineHeight: 18,
  },
})