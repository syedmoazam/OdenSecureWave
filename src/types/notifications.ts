export enum NotificationType {
  INFO = 'info',
  ALERT = 'alert',
}

export interface BaseNotification {
  id: string;
  title: string;
  body: string;
  type: NotificationType;
  timestamp: number;
  data?: Record<string, string>;
}

export interface InfoNotification extends BaseNotification {
  type: NotificationType.INFO;
  category?: string;
}

export interface AlertNotification extends BaseNotification {
  type: NotificationType.ALERT;
  severity: 'low' | 'medium' | 'high' | 'critical';
  actionRequired?: boolean;
}

export type AppNotification = InfoNotification | AlertNotification;

export interface NotificationConfig {
  channelId: string;
  channelName: string;
  importance: 'default' | 'high' | 'max';
  enableVibration: boolean;
  enableSound: boolean;
  showBadge: boolean;
}

export interface FCMMessage {
  messageId?: string;
  from?: string;
  to?: string;
  collapseKey?: string;
  data?: Record<string, string>;
  notification?: {
    title?: string;
    body?: string;
    android?: {
      channelId?: string;
      priority?: string;
      smallIcon?: string;
      largeIcon?: string;
      color?: string;
    };
  };
} 