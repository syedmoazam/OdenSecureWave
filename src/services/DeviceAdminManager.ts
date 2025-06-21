import { NativeModules, Platform } from 'react-native';

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
}

export default DeviceAdminManager;
