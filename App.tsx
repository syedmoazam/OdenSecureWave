import { useEffect } from 'react';
import AppNavigator from "@/navigators/AppNavigator";
import {appMainContainer} from "@/themes/AppStyles";
import {SafeAreaProvider} from "react-native-safe-area-context";
import { useNotifications } from '@/hooks/useNotifications';
import NotificationBanner from '@/components/NotificationBanner';

function App() {
  const { isInitialized, fcmToken, lastNotification } = useNotifications();

  useEffect(() => {
    if (isInitialized && fcmToken) {
      console.log('Firebase messaging initialized with token:', fcmToken);
      // Here you can send the token to your backend server
    }
  }, [isInitialized, fcmToken]);

  const handleNotificationPress = (notification: any) => {
    console.log('Notification pressed:', notification);
    // Handle navigation based on notification type
  };

  const handleNotificationDismiss = (notification: any) => {
    console.log('Notification dismissed:', notification);
  };

  return (
    <SafeAreaProvider style={appMainContainer}>
      <AppNavigator />
      <NotificationBanner
        notification={lastNotification}
        onPress={handleNotificationPress}
        onDismiss={handleNotificationDismiss}
      />
    </SafeAreaProvider>
  );
}

export default App;