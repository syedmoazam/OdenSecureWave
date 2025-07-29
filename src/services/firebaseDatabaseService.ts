import database from '@react-native-firebase/database';
import { FAQItem } from '@/types/faq';
import { CompanyData, CompanyItem } from '@/types/company';

class FirebaseDatabaseService {
  private static instance: FirebaseDatabaseService;
  private databaseRef: any;

  public static getInstance(): FirebaseDatabaseService {
    if (!FirebaseDatabaseService.instance) {
      FirebaseDatabaseService.instance = new FirebaseDatabaseService();
    }
    return FirebaseDatabaseService.instance;
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
}

export default FirebaseDatabaseService; 