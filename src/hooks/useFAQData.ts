import { useQuery } from '@tanstack/react-query';
import DatabaseService from '@/services/databaseService.ts';
import { FAQItem } from '@/types/faq';
import { QUERY_KEYS, QUERY_CONFIG } from '@/constants/storageKeys';

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

// Helper function to fetch FAQ data
const fetchFAQData = async (): Promise<FAQItem[]> => {
  const databaseService = DatabaseService.getInstance();
  const data = await databaseService.getFAQData();
  return data.length > 0 ? data : fallbackFAQData;
};

export const useFAQData = () => {
  const {
    data = fallbackFAQData,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: QUERY_KEYS.FAQ_DATA,
    queryFn: fetchFAQData,
    staleTime: QUERY_CONFIG.STALE_TIME.LONG,
    gcTime: QUERY_CONFIG.CACHE_TIME.LONG,
    retry: QUERY_CONFIG.RETRY.DEFAULT,
    // Always provide fallback data
    placeholderData: fallbackFAQData,
    // Handle network errors gracefully
    retryOnMount: true,
    refetchOnReconnect: true,
  });

  const refreshData = () => {
    refetch();
  };

  return {
    data,
    loading: isLoading,
    error: error?.message || null,
    refreshData,
  };
}; 