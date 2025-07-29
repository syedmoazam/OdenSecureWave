export interface CompanyData {
  branchId: string;
  branchName: string;
  companyId: string;
  companyName: string;
}

export interface CompanyItem {
  id: string;
  name: string;
  branch: string;
}

export interface CompanyState {
  data: CompanyItem[];
  loading: boolean;
  error: string | null;
} 