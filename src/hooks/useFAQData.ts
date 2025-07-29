import { useState, useEffect } from 'react';
import DatabaseService from '@/services/databaseService.ts';
import { FAQItem, FAQState } from '@/types/faq';

// Fallback mock data
const fallbackFAQData: FAQItem[] = [
  {
    id: 'faq-1',
    title: 'Why is my device locked?',
    description: 'Your device is locked due to overdue payment. Once payment is made, your device will be automatically unlocked.'
  },
  {
    id: 'faq-2',
    title: 'How can I make a payment?',
    description: 'You can make a payment using the "Make Payment Now" button on the Alert tab, or contact our support team for assistance.'
  },
  {
    id: 'faq-3',
    title: 'What happens after I pay?',
    description: 'Your device will be automatically unlocked within 24 hours of payment confirmation.'
  },
  {
    id: 'faq-4',
    title: 'Can I get an extension?',
    description: 'Please contact our support team to discuss payment arrangements and possible extensions.'
  }
];

export const useFAQData = () => {
  const [state, setState] = useState<FAQState>({
    data: [],
    loading: true,
    error: null
  });

  const databaseService = DatabaseService.getInstance();

  const fetchFAQData = async () => {
    try {
      // Bug: When data is loaded for the first time, it returns connection as false
      // const isConnected = await databaseService.isConnected();
      // if (!isConnected) {
      //   setState({
      //     data: fallbackFAQData,
      //     loading: false,
      //     error: 'No internet connection. Showing cached version.'
      //   });
      //   return;
      // }
      setState(prev => ({ ...prev, loading: true, error: null }));
      
      const data = await databaseService.getFAQData();
      
      setState({
        data: data.length > 0 ? data : fallbackFAQData,
        loading: false,
        error: null
      });
    } catch (error) {
      console.error('Error fetching FAQ data:', error);
      setState({
        data: fallbackFAQData,
        loading: false,
        error: 'Failed to load FAQ data. Showing cached version.'
      });
    }
  };

  useEffect(() => {
    fetchFAQData();
  }, []);

  const refreshData = () => {
    fetchFAQData();
  };

  return {
    ...state,
    refreshData
  };
}; 