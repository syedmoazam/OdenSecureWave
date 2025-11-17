import { useQuery } from '@tanstack/react-query';
import DatabaseService from '@/services/databaseService.ts';
import { CompanyItem } from '@/types/company';
import { QUERY_KEYS, QUERY_CONFIG } from '@/constants/storageKeys';

// Fallback mock data
const fallbackCompanyData: CompanyItem[] = [
  { id: '1', name: 'Company A', branch: 'Branch 1' },
  { id: '2', name: 'Company B', branch: 'Branch 2' },
  { id: '3', name: 'Company C', branch: 'Branch 3' },
  { id: '4', name: 'Company D', branch: 'Branch 4' },
  { id: '5', name: 'Company E', branch: 'Branch 5' },
];

// Helper function to fetch company data
const fetchCompanyData = async (): Promise<CompanyItem[]> => {
  const databaseService = DatabaseService.getInstance();
  const data = await databaseService.getCompanyData();
  return data.length > 0 ? data : fallbackCompanyData;
};

export const useCompanyData = () => {
  const {
    data = fallbackCompanyData,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: QUERY_KEYS.COMPANIES,
    queryFn: fetchCompanyData,
    staleTime: QUERY_CONFIG.STALE_TIME.LONG,
    gcTime: QUERY_CONFIG.CACHE_TIME.LONG,
    retry: QUERY_CONFIG.RETRY.DEFAULT,
    placeholderData: fallbackCompanyData,
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