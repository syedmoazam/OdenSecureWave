import { useEffect, useState, useCallback } from 'react';
import FirebaseMessagingService from '@/services/firebaseMessagingService';
import { AppNotification, NotificationType } from '@/types/notifications';

interface UseNotificationsReturn {
  fcmToken: string | null;
  isInitialized: boolean;
  lastNotification: AppNotification | null;
  notifications: AppNotification[];
  subscribeToTopic: (topic: string) => Promise<void>;
  unsubscribeFromTopic: (topic: string) => Promise<void>;
  clearNotifications: () => void;
  markNotificationAsRead: (notificationId: string) => void;
}

export const useNotifications = (): UseNotificationsReturn => {
  const [fcmToken, setFcmToken] = useState<string | null>(null);
  const [isInitialized, setIsInitialized] = useState(false);
  const [lastNotification, setLastNotification] = useState<AppNotification | null>(null);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);

  // Initialize Firebase messaging service
  useEffect(() => {
    const initializeService = async () => {
      try {
        const messagingService = FirebaseMessagingService.getInstance();
        await messagingService.initialize();
        
        const token = messagingService.getCurrentToken();
        setFcmToken(token);
        setIsInitialized(messagingService.getIsInitialized());

        // Subscribe to all notifications
        const unsubscribe = messagingService.onNotification((notification: AppNotification) => {
          setLastNotification(notification);
          setNotifications(prev => [notification, ...prev]);
        });

        // Cleanup function
        return unsubscribe;
      } catch (error) {
        console.error('Failed to initialize messaging service:', error);
      }
    };

    const unsubscribe = initializeService();
    
    return () => {
      unsubscribe?.then(unsub => unsub?.());
    };
  }, []);

  const subscribeToTopic = useCallback(async (topic: string) => {
    const messagingService = FirebaseMessagingService.getInstance();
    await messagingService.subscribeToTopic(topic);
  }, []);

  const unsubscribeFromTopic = useCallback(async (topic: string) => {
    const messagingService = FirebaseMessagingService.getInstance();
    await messagingService.unsubscribeFromTopic(topic);
  }, []);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
    setLastNotification(null);
  }, []);

  const markNotificationAsRead = useCallback((notificationId: string) => {
    setNotifications(prev => 
      prev.map(notification => 
        notification.id === notificationId 
          ? { ...notification, data: { ...notification.data, read: 'true' } }
          : notification
      )
    );
  }, []);

  return {
    fcmToken,
    isInitialized,
    lastNotification,
    notifications,
    subscribeToTopic,
    unsubscribeFromTopic,
    clearNotifications,
    markNotificationAsRead,
  };
};

// Hook for subscribing to specific notification types
export const useNotificationsByType = (type: NotificationType): AppNotification[] => {
  const { notifications } = useNotifications();
  
  return notifications.filter(notification => notification.type === type);
};

// Hook for getting unread notifications count
export const useUnreadNotificationsCount = (): number => {
  const { notifications } = useNotifications();
  
  return notifications.filter(notification => notification.data?.read !== 'true').length;
}; 