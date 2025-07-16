import messaging, { FirebaseMessagingTypes } from '@react-native-firebase/messaging';
import { Platform, PermissionsAndroid } from 'react-native';
import { NotificationType, AppNotification, NotificationConfig } from '@/types/notifications';

class FirebaseMessagingService {
  private static instance: FirebaseMessagingService;
  private fcmToken: string | null = null;
  private isInitialized = false;
  private notificationCallbacks: Array<(notification: AppNotification) => void> = [];

  // Notification channel configurations
  private readonly notificationChannels: Record<NotificationType, NotificationConfig> = {
    [NotificationType.INFO]: {
      channelId: 'info_notifications',
      channelName: 'Information Notifications',
      importance: 'default',
      enableVibration: true,
      enableSound: true,
      showBadge: true,
    },
    [NotificationType.ALERT]: {
      channelId: 'alert_notifications',
      channelName: 'Security Alerts',
      importance: 'high',
      enableVibration: true,
      enableSound: true,
      showBadge: true,
    },
  };

  public static getInstance(): FirebaseMessagingService {
    if (!FirebaseMessagingService.instance) {
      FirebaseMessagingService.instance = new FirebaseMessagingService();
    }
    return FirebaseMessagingService.instance;
  }

  private constructor() {}

  /**
   * Initialize Firebase messaging service
   */
  public async initialize(): Promise<void> {
    if (this.isInitialized) {
      return;
    }

    try {
      // Request permission for notifications
      await this.requestPermission();

      // Get FCM token
      await this.getFCMToken();

      // Set up message handlers
      this.setupMessageHandlers();

      // Set background message handler
      this.setupBackgroundMessageHandler();

      this.isInitialized = true;
      console.log('Firebase Messaging Service initialized successfully');
    } catch (error) {
      console.error('Failed to initialize Firebase Messaging Service:', error);
      throw error;
    }
  }

  /**
   * Request notification permissions
   */
  private async requestPermission(): Promise<boolean> {
    try {
      if (Platform.OS === 'android') {
        if (Platform.Version >= 33) {
          const granted = await PermissionsAndroid.request(
            PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
          );
          return granted === PermissionsAndroid.RESULTS.GRANTED;
        }
        return true; // Permissions are granted by default for Android < 13
      }

      const authStatus = await messaging().requestPermission();
      const enabled =
        authStatus === messaging.AuthorizationStatus.AUTHORIZED ||
        authStatus === messaging.AuthorizationStatus.PROVISIONAL;

      if (!enabled) {
        console.warn('Notification permission denied');
      }

      return enabled;
    } catch (error) {
      console.error('Error requesting notification permission:', error);
      return false;
    }
  }

  /**
   * Get FCM token
   */
  public async getFCMToken(): Promise<string | null> {
    try {
      const token = await messaging().getToken();
      this.fcmToken = token;
      console.log('FCM Token:', token);
      return token;
    } catch (error) {
      console.error('Error getting FCM token:', error);
      return null;
    }
  }

  /**
   * Set up message handlers for foreground and background
   */
  private setupMessageHandlers(): void {
    // Handle messages when app is in foreground
    messaging().onMessage(async (remoteMessage: FirebaseMessagingTypes.RemoteMessage) => {
      console.log('Received foreground message:', remoteMessage);
      this.handleMessage(remoteMessage);
    });

    // Handle notification opened app from background/quit state
    messaging().onNotificationOpenedApp((remoteMessage: FirebaseMessagingTypes.RemoteMessage) => {
      console.log('Notification opened app from background:', remoteMessage);
      this.handleNotificationOpened(remoteMessage);
    });

    // Handle initial notification when app is opened from quit state
    messaging()
      .getInitialNotification()
      .then((remoteMessage: FirebaseMessagingTypes.RemoteMessage | null) => {
        if (remoteMessage) {
          console.log('App opened from quit state by notification:', remoteMessage);
          this.handleNotificationOpened(remoteMessage);
        }
      });

    // Listen for token refresh
    messaging().onTokenRefresh((token: string) => {
      console.log('FCM Token refreshed:', token);
      this.fcmToken = token;
      // You can send this token to your server
    });
  }

  /**
   * Set up background message handler
   */
  private setupBackgroundMessageHandler(): void {
    messaging().setBackgroundMessageHandler(async (remoteMessage: FirebaseMessagingTypes.RemoteMessage) => {
      console.log('Message handled in the background:', remoteMessage);
      // Handle background message if needed
    });
  }

  /**
   * Handle incoming messages
   */
  private handleMessage(remoteMessage: FirebaseMessagingTypes.RemoteMessage): void {
    const notification = this.parseMessage(remoteMessage);
    if (notification) {
      this.notifyCallbacks(notification);
    }
  }

  /**
   * Handle notification opened events
   */
  private handleNotificationOpened(remoteMessage: FirebaseMessagingTypes.RemoteMessage): void {
    const notification = this.parseMessage(remoteMessage);
    if (notification) {
      // Handle navigation or specific actions based on notification type
      this.handleNotificationAction(notification);
    }
  }

  /**
   * Parse FCM message to app notification format
   */
  private parseMessage(remoteMessage: FirebaseMessagingTypes.RemoteMessage): AppNotification | null {
    try {
      const { notification, data } = remoteMessage;
      
      if (!notification?.title || !notification?.body || !data?.type || typeof data.type !== 'string') {
        console.warn('Invalid notification format:', remoteMessage);
        return null;
      }

      const notificationType = data.type as NotificationType;
      
      // Convert data to Record<string, string>
      const notificationData: Record<string, string> = {};
      if (data) {
        Object.entries(data).forEach(([key, value]) => {
          notificationData[key] = typeof value === 'string' ? value : String(value);
        });
      }

      const baseNotification = {
        id: remoteMessage.messageId || Date.now().toString(),
        title: notification.title,
        body: notification.body,
        timestamp: Date.now(),
        data: notificationData,
      };

      switch (notificationType) {
        case NotificationType.INFO:
          return {
            ...baseNotification,
            type: NotificationType.INFO,
            category: notificationData.category,
          };

        case NotificationType.ALERT:
          return {
            ...baseNotification,
            type: NotificationType.ALERT,
            severity: (notificationData.severity as 'low' | 'medium' | 'high' | 'critical') || 'medium',
            actionRequired: notificationData.actionRequired === 'true',
          };

        default:
          console.warn('Unknown notification type:', notificationType);
          return null;
      }
    } catch (error) {
      console.error('Error parsing notification:', error);
      return null;
    }
  }

  /**
   * Handle notification actions based on type
   */
  private handleNotificationAction(notification: AppNotification): void {
    switch (notification.type) {
      case NotificationType.ALERT:
        // Navigate to emergency screen or specific alert handling
        console.log('Handling alert notification:', notification);
        break;

      case NotificationType.INFO:
        // Navigate to relevant screen based on category
        console.log('Handling info notification:', notification);
        break;

      default:
        console.log('Unhandled notification type:', (notification as any).type);
    }
  }

  /**
   * Subscribe to notification events
   */
  public onNotification(callback: (notification: AppNotification) => void): () => void {
    this.notificationCallbacks.push(callback);
    
    // Return unsubscribe function
    return () => {
      const index = this.notificationCallbacks.indexOf(callback);
      if (index > -1) {
        this.notificationCallbacks.splice(index, 1);
      }
    };
  }

  /**
   * Notify all subscribers
   */
  private notifyCallbacks(notification: AppNotification): void {
    this.notificationCallbacks.forEach(callback => {
      try {
        callback(notification);
      } catch (error) {
        console.error('Error in notification callback:', error);
      }
    });
  }

  /**
   * Subscribe to topic
   */
  public async subscribeToTopic(topic: string): Promise<void> {
    try {
      await messaging().subscribeToTopic(topic);
      console.log(`Subscribed to topic: ${topic}`);
    } catch (error) {
      console.error(`Error subscribing to topic ${topic}:`, error);
    }
  }

  /**
   * Unsubscribe from topic
   */
  public async unsubscribeFromTopic(topic: string): Promise<void> {
    try {
      await messaging().unsubscribeFromTopic(topic);
      console.log(`Unsubscribed from topic: ${topic}`);
    } catch (error) {
      console.error(`Error unsubscribing from topic ${topic}:`, error);
    }
  }

  /**
   * Get current FCM token
   */
  public getCurrentToken(): string | null {
    return this.fcmToken;
  }

  /**
   * Check if service is initialized
   */
  public getIsInitialized(): boolean {
    return this.isInitialized;
  }

  /**
   * Get notification channel config for type
   */
  public getNotificationChannelConfig(type: NotificationType): NotificationConfig {
    return this.notificationChannels[type];
  }
}

export default FirebaseMessagingService; 