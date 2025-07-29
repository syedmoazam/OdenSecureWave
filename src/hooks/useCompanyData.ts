import { useState, useEffect } from 'react';
import FirebaseDatabaseService from '@/services/firebaseDatabaseService';
import { CompanyItem, CompanyState } from '@/types/company';

// Fallback mock data
const fallbackCompanyData: CompanyItem[] = [
  { id: '1', name: 'Company A', branch: 'Branch 1' },
  { id: '2', name: 'Company B', branch: 'Branch 2' },
  { id: '3', name: 'Company C', branch: 'Branch 3' },
  { id: '4', name: 'Company D', branch: 'Branch 4' },
  { id: '5', name: 'Company E', branch: 'Branch 5' },
];

export const useCompanyData = () => {
  const [state, setState] = useState<CompanyState>({
    data: [],
    loading: true,
    error: null
  });

  const databaseService = FirebaseDatabaseService.getInstance();

  const fetchCompanyData = async () => {
    try {
      setState(prev => ({ ...prev, loading: true, error: null }));
      
      const data = await databaseService.getCompanyData();
      setState({
        data: data.length > 0 ? data : fallbackCompanyData,
        loading: false,
        error: null
      });
    } catch (error) {
      console.error('Error fetching company data:', error);
      setState({
        data: fallbackCompanyData,
        loading: false,
        error: 'Failed to load company data. Showing cached version.'
      });
    }
  };

  useEffect(() => {
    fetchCompanyData();
  }, []);

  const refreshData = () => {
    fetchCompanyData();
  };

  return {
    ...state,
    refreshData
  };
}; 