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


}

export default DeviceAdminManager;
