export interface MonitoredDeviceData {
    createdAt: string;
    deviceId: string;
    deviceName: string;
    manufacturer: string;
    fcmToken: string;
    isEnabled: boolean;
    status: 'active' | 'lock';
    company: {
      id: string;
      name: string;
      branch: string;
    };
  }
  
  export interface DeviceState {
    isMonitoringEnabled: boolean;
    deviceData: MonitoredDeviceData | null;
    loading: boolean;
    error: string | null;
  }