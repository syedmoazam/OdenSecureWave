import { useEffect } from 'react';
import AppNavigator from "@/navigators/AppNavigator";
import {appMainContainer} from "@/themes/AppStyles";
import {SafeAreaProvider} from "react-native-safe-area-context";
import { useNotifications } from '@/hooks/useNotifications';
import NotificationBanner from '@/components/NotificationBanner';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { QUERY_CONFIG } from '@/constants/storageKeys';
import { DeviceDataProvider } from '@/contexts/DeviceDataContext';

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: QUERY_CONFIG.STALE_TIME.MEDIUM,
      gcTime: QUERY_CONFIG.CACHE_TIME.MEDIUM, // Previously cacheTime in v4
      retry: QUERY_CONFIG.RETRY.DEFAULT,
      refetchOnWindowFocus: false,
      refetchOnMount: false,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: QUERY_CONFIG.RETRY.DEFAULT,
    },
  },
});

function App() {
  useNotifications();

  return (
    <QueryClientProvider client={queryClient}>
      <DeviceDataProvider>
        <SafeAreaProvider style={appMainContainer}>
          <AppNavigator />
          {/* <NotificationBanner
            notification={lastNotification}
            onPress={handleNotificationPress}
            onDismiss={handleNotificationDismiss}
          /> */}
        </SafeAreaProvider>
      </DeviceDataProvider>
    </QueryClientProvider>
  );
}

export default App;