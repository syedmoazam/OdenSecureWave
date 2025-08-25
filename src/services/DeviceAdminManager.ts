import { NativeModules, Platform } from 'react-native';

interface DeviceInfo {
  manufacturer: string;
  model: string;
  brand: string;
  device: string;
  product: string;
  androidVersion: string;
  apiLevel: number;
}

interface DeviceAdminModuleInterface {
  isDeviceAdminEnabled(): Promise<boolean>;
  enableDeviceAdmin(): Promise<string>;
  disableDeviceAdmin(): Promise<string>;
  lockDevice(): Promise<string>;
  wipeDevice(confirm: boolean): Promise<string>;
  setPasswordPolicy(minLength: number): Promise<string>;
  checkBootCompletedStatus(): Promise<boolean>;
  setBootCompletedStatus(value: boolean): Promise<string>;
  setCameraDisabled(disabled: boolean): Promise<string>;
  isCameraDisabled(): Promise<boolean>;
  setKeyguardDisabledFeatures(features: number): Promise<string>;
  getKeyguardDisabledFeatures(): Promise<number>;
  isDeviceOwner(): Promise<boolean>;
  isProfileOwner(): Promise<boolean>;
  testBridge(): Promise<string>;
  getDeviceIMEI(): Promise<string>;
  getDeviceInfo(): Promise<DeviceInfo>;
  lockApp(): Promise<string>;
  unlockApp(): Promise<string>;
  launchApp(): Promise<string>;
  closeApp(): Promise<string>;
  startPeriodicService(): Promise<string>;
  stopPeriodicService(): Promise<string>;
  isPeriodicServiceEnabled(): Promise<{enabled: boolean; workScheduled: boolean}>;
  configureDeviceOwnerPrivileges(): Promise<string>;
  getServicePrivilegeStatus(): Promise<any>;
  initializeServiceOnStartup(): Promise<string>;
  testBootFirebaseCheck(): Promise<string>;
  simulateBootCompleted(): Promise<string>;
}

const { DeviceAdminModule } = NativeModules;

class DeviceAdminManager {
  private static instance: DeviceAdminManager;

  private constructor() {
    // Singleton instance
  }

  static getInstance(): DeviceAdminManager {
    if (!DeviceAdminManager.instance) {
      DeviceAdminManager.instance = new DeviceAdminManager();
    }
    return DeviceAdminManager.instance;
  }

  // Check if device admin is enabled
  async isDeviceAdminEnabled(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.isDeviceAdminEnabled();
  }

  // Enable device admin (shows system dialog)
  async enableDeviceAdmin(): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.enableDeviceAdmin();
  }

  // Disable device admin (shows system dialog)
  async disableDeviceAdmin(): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.disableDeviceAdmin();
  }

  // Lock the device immediately
  async lockDevice(): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.lockDevice();
  }

  // Wipe device data (use with extreme caution!)
  async wipeDevice(confirm: boolean = false): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.wipeDevice(confirm);
  }

  // Set password policy
  async setPasswordPolicy(minLength: number): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.setPasswordPolicy(minLength);
  }

  // Check if app was started from boot
  async checkBootCompletedStatus(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.checkBootCompletedStatus();
  }

  // Set boot completed status
  async setBootCompletedStatus(value: boolean): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.setBootCompletedStatus(value);
  }

  // Set camera disabled/enabled
  async setCameraDisabled(disabled: boolean): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.setCameraDisabled(disabled);
  }

  // Check if camera is disabled
  async isCameraDisabled(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.isCameraDisabled();
  }

  // Set keyguard disabled features
  async setKeyguardDisabledFeatures(features: number): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.setKeyguardDisabledFeatures(features);
  }

  // Get keyguard disabled features
  async getKeyguardDisabledFeatures(): Promise<number> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.getKeyguardDisabledFeatures();
  }

  // Check if app is device owner
  async isDeviceOwner(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.isDeviceOwner();
  }

  // Check if app is profile owner
  async isProfileOwner(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.isProfileOwner();
  }

  // Test if bridge is working
  async testBridge(): Promise<string> {
    if (Platform.OS !== 'android') {
      throw new Error('Device Admin is only available on Android');
    }
    return DeviceAdminModule.testBridge();
  }

         /**
   * Get device IMEI
   */
    public async getDeviceIMEI(): Promise<string> {
      try {
        if (Platform.OS !== 'android') {
          throw new Error('IMEI is only available on Android devices');
        }
        
        const imei = await DeviceAdminModule.getDeviceIMEI();
        return imei;
      } catch (error) {
        console.error('Error getting device IMEI:', error);
        throw error;
      }
    }

  /**
   * Get device information (manufacturer, model, etc.)
   */
  public async getDeviceInfo(): Promise<DeviceInfo> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Device info is only available on Android devices');
      }
      
      const deviceInfo = await DeviceAdminModule.getDeviceInfo();
      return deviceInfo;
    } catch (error) {
      console.error('Error getting device info:', error);
      throw error;
    }
  }

  /**
   * Enable kiosk mode - locks device to only this app
   */
  public async lockApp(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Kiosk mode is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.lockApp();
      return result;
    } catch (error) {
      console.error('Error enabling kiosk mode:', error);
      throw error;
    }
  }

  /**
   * Disable kiosk mode - allows normal device usage
   */
  public async unlockApp(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Kiosk mode is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.unlockApp();
      return result;
    } catch (error) {
      console.error('Error disabling kiosk mode:', error);
      throw error;
    }
  }

  /**
   * Launch/restart the application
   */
  public async launchApp(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('App launch control is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.launchApp();
      return result;
    } catch (error) {
      console.error('Error launching app:', error);
      throw error;
    }
  }

  /**
   * Close/terminate the application
   */
  public async closeApp(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('App close control is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.closeApp();
      return result;
    } catch (error) {
      console.error('Error closing app:', error);
      throw error;
    }
  }

  /**
   * Start the WorkManager periodic service
   */
  public async startPeriodicService(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Periodic service is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.startPeriodicService();
      return result;
    } catch (error) {
      console.error('Error starting periodic service:', error);
      throw error;
    }
  }

  /**
   * Stop the WorkManager periodic service
   */
  public async stopPeriodicService(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Periodic service is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.stopPeriodicService();
      return result;
    } catch (error) {
      console.error('Error stopping periodic service:', error);
      throw error;
    }
  }

  /**
   * Check if periodic service is enabled and scheduled
   */
  public async isPeriodicServiceEnabled(): Promise<{enabled: boolean; workScheduled: boolean}> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Periodic service is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.isPeriodicServiceEnabled();
      return result;
    } catch (error) {
      console.error('Error checking periodic service status:', error);
      throw error;
    }
  }

  /**
   * Configure Device Owner privileges for better service reliability
   */
  public async configureDeviceOwnerPrivileges(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Device Owner privileges are only available on Android devices');
      }
      
      const result = await DeviceAdminModule.configureDeviceOwnerPrivileges();
      return result;
    } catch (error) {
      console.error('Error configuring Device Owner privileges:', error);
      throw error;
    }
  }

  /**
   * Get comprehensive service and privilege status
   */
  public async getServicePrivilegeStatus(): Promise<any> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Service status is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.getServicePrivilegeStatus();
      return result;
    } catch (error) {
      console.error('Error getting service privilege status:', error);
      throw error;
    }
  }

  public async debugServiceStatus() {
    try {
      console.log('=== DEBUGGING SERVICE STATUS ===');
      
      // Check if DeviceAdminModule is available
      console.log('DeviceAdminModule available:', !!DeviceAdminModule);
      
      // Check current service status
      const status = await DeviceAdminModule.isPeriodicServiceEnabled();
      console.log('Current service status:', status);
      
      // Try to start service manually
      console.log('Attempting to start service...');
      await DeviceAdminModule.startPeriodicService();
      console.log('Service start command completed');
      
      // Check status again
      const newStatus = await DeviceAdminModule.isPeriodicServiceEnabled();
      console.log('New service status:', newStatus);
      
    } catch (error) {
      console.error('Service debug error:', error);
    }
  };
  
  // Call this function when you want to debug
  // debugServiceStatus();
  /**
   * Initialize service on startup (for debugging)
   */
  public async initializeServiceOnStartup(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Service initialization is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.initializeServiceOnStartup();
      return result;
    } catch (error) {
      console.error('Error initializing service on startup:', error);
      throw error;
    }
  }

  /**
   * Test boot Firebase check (for debugging)
   */
  public async testBootFirebaseCheck(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Boot Firebase check is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.testBootFirebaseCheck();
      return result;
    } catch (error) {
      console.error('Error testing boot Firebase check:', error);
      throw error;
    }
  }

  /**
   * Simulate boot completed broadcast (for debugging)
   */
  public async simulateBootCompleted(): Promise<string> {
    try {
      if (Platform.OS !== 'android') {
        throw new Error('Boot simulation is only available on Android devices');
      }
      
      const result = await DeviceAdminModule.simulateBootCompleted();
      return result;
    } catch (error) {
      console.error('Error simulating boot completed:', error);
      throw error;
    }
  }
}

export default DeviceAdminManager;
