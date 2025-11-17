import database from '@react-native-firebase/database';
import { FAQItem } from '@/types/faq';
import { CompanyData, CompanyItem } from '@/types/company';
import { MonitoredDeviceData } from '@/types/device';

class DatabaseService {
  private static instance: DatabaseService;
  private databaseRef: any;

  public static getInstance(): DatabaseService {
    if (!DatabaseService.instance) {
      DatabaseService.instance = new DatabaseService();
    }
    return DatabaseService.instance;
  }

  private constructor() {
    this.databaseRef = database();
  }

  /**
   * Check if Firebase is connected with better error handling
   */
  public async isConnected(): Promise<boolean> {
    try {
      console.log('Checking Firebase connection...');
      
      // Add timeout to prevent hanging
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Connection timeout')), 10000);
      });
      
      const connectionPromise = this.databaseRef.ref('.info/connected').once('value');
      const snapshot = await Promise.race([connectionPromise, timeoutPromise]);
      
      const connected = snapshot.val() === true;
      console.log('Firebase connection status:', connected);
      return connected;
    } catch (error) {
      console.error('Error checking Firebase connection:', error);
      return false;
    }
  }

  /**
   * Fetch FAQ data from Firebase Realtime Database
   */
  public async getFAQData(): Promise<FAQItem[]> {
    try {
      const snapshot = await this.databaseRef.ref('FAQ/questions').once('value');
      const data = snapshot.val();
      
      if (!data) {
        return [];
      }

      // Convert object to array if needed
      const faqArray = Array.isArray(data) ? data : Object.values(data);
      
      // Ensure each item has an id
      return faqArray.map((item: any, index: number) => ({
        id: item.id || `faq-${index}`,
        title: item.title || '',
        description: item.description || ''
      }));
    } catch (error) {
      console.error('Error fetching FAQ data:', error);
      throw error;
    }
  }

  /**
   * Fetch company data from Firebase Realtime Database
   */
  public async getCompanyData(): Promise<CompanyItem[]> {
    try {
      const snapshot = await this.databaseRef.ref('Company').once('value');
      const data = snapshot.val();
      
      if (!data) {
        return [];
      }

      // Convert Firebase object to array of CompanyItem
      const companyItems: CompanyItem[] = Object.entries(data).map(([key, value]: [string, any]) => {
        const companyData = value as CompanyData;
        return {
          id: key,
          name: companyData.companyName,
          branch: companyData.branchName
        };
      });

      return companyItems;
    } catch (error) {
      console.error('Error fetching company data:', error);
      throw error;
    }
  }

  /**
   * Create a monitored device record in Firebase
   */
  public async createMonitoredDevice(monitoredDeviceData: MonitoredDeviceData): Promise<void> {
    try {
      console.log('Creating monitored device record:', monitoredDeviceData);
      
      // Use the deviceId (IMEI) as the key
      await this.databaseRef.ref(`monitoredDevices/${monitoredDeviceData.deviceId}`).set(monitoredDeviceData);
      
      console.log('Monitored device record created successfully');
    } catch (error) {
      console.error('Error creating monitored device record:', error);
      throw error;
    }
  }

  /**
   * Get full monitored device data including status and isEnabled
   */
  public async getMonitoredDeviceData(imei: string): Promise<MonitoredDeviceData | null> {
    try {
      console.log('Fetching monitored device data for IMEI:', imei);
      
      const snapshot = await this.databaseRef.ref(`monitoredDevices/${imei}`).once('value');
      const data = snapshot.val();
      
      if (!data) {
        console.log('No monitored device data found for IMEI:', imei);
        return null;
      }

      console.log('Monitored device data found:', data);
      return data as MonitoredDeviceData;
    } catch (error) {
      console.error('Error fetching monitored device data:', error);
      throw error;
    }
  }
}

export default DatabaseService;