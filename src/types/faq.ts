export interface FAQItem {
  id: string;
  title: string;
  description: string;
}

export interface FAQData {
  questions: FAQItem[];
}

export interface FAQState {
  data: FAQItem[];
  loading: boolean;
  error: string | null;
} 