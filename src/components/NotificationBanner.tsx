import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Animated,
  TouchableOpacity,
  Dimensions,
} from 'react-native';
import { AppNotification, NotificationType } from '@/types/notifications';
import { Colors } from '@/themes/Colors';
import { scale, verticalScale, moderateScale } from 'react-native-size-matters';

interface NotificationBannerProps {
  notification: AppNotification | null;
  onPress?: (notification: AppNotification) => void;
  onDismiss?: (notification: AppNotification) => void;
  duration?: number; // Auto dismiss duration in ms
}

const { width } = Dimensions.get('window');

const NotificationBanner: React.FC<NotificationBannerProps> = ({
  notification,
  onPress,
  onDismiss,
  duration = 5000,
}) => {
  const [slideAnim] = useState(new Animated.Value(-100));
  const [currentNotification, setCurrentNotification] = useState<AppNotification | null>(null);

  useEffect(() => {
    if (notification) {
      setCurrentNotification(notification);
      
      // Slide down animation
      Animated.timing(slideAnim, {
        toValue: 0,
        duration: 300,
        useNativeDriver: true,
      }).start();

      // Auto dismiss after duration
      const timer = setTimeout(() => {
        dismissNotification();
      }, duration);

      return () => clearTimeout(timer);
    }
  }, [notification]);

  const dismissNotification = () => {
    Animated.timing(slideAnim, {
      toValue: -100,
      duration: 300,
      useNativeDriver: true,
    }).start(() => {
      setCurrentNotification(null);
      if (currentNotification && onDismiss) {
        onDismiss(currentNotification);
      }
    });
  };

  const handlePress = () => {
    if (currentNotification && onPress) {
      onPress(currentNotification);
    }
    dismissNotification();
  };

  if (!currentNotification) {
    return null;
  }

  const getNotificationStyle = (type: NotificationType) => {
    switch (type) {
      case NotificationType.ALERT:
        return {
          backgroundColor: Colors.RED,
          borderLeftColor: '#D63031',
        };
      case NotificationType.INFO:
        return {
          backgroundColor: Colors.PRIMARY_BLUE,
          borderLeftColor: '#0984E3',
        };
      default:
        return {
          backgroundColor: Colors.PRIMARY_BLUE,
          borderLeftColor: '#0984E3',
        };
    }
  };

  const getIconForType = (type: NotificationType) => {
    switch (type) {
      case NotificationType.ALERT:
        return '⚠️';
      case NotificationType.INFO:
        return 'ℹ️';
      default:
        return 'ℹ️';
    }
  };

  const notificationStyle = getNotificationStyle(currentNotification.type);

  return (
    <Animated.View
      style={[
        styles.container,
        notificationStyle,
        {
          transform: [{ translateY: slideAnim }],
        },
      ]}
    >
      <TouchableOpacity
        style={styles.content}
        onPress={handlePress}
        activeOpacity={0.8}
      >
        <View style={styles.iconContainer}>
          <Text style={styles.icon}>
            {getIconForType(currentNotification.type)}
          </Text>
        </View>
        
        <View style={styles.textContainer}>
          <Text style={styles.title} numberOfLines={1}>
            {currentNotification.title}
          </Text>
          <Text style={styles.body} numberOfLines={2}>
            {currentNotification.body}
          </Text>
          
          {currentNotification.type === NotificationType.ALERT && 
           'severity' in currentNotification && (
            <Text style={styles.severity}>
              Severity: {currentNotification.severity.toUpperCase()}
            </Text>
          )}
        </View>
        
        <TouchableOpacity
          style={styles.dismissButton}
          onPress={dismissNotification}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        >
          <Text style={styles.dismissText}>✕</Text>
        </TouchableOpacity>
      </TouchableOpacity>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 9999,
    borderLeftWidth: scale(4),
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: verticalScale(2),
    },
    shadowOpacity: 0.25,
    shadowRadius: scale(3.84),
    elevation: 5,
  },
  content: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: scale(16),
    paddingVertical: verticalScale(12),
    paddingTop: verticalScale(50), // Account for status bar
  },
  iconContainer: {
    marginRight: scale(12),
  },
  icon: {
    fontSize: moderateScale(24),
  },
  textContainer: {
    flex: 1,
    marginRight: scale(8),
  },
  title: {
    fontSize: moderateScale(16),
    fontWeight: '600',
    color: '#FFFFFF',
    marginBottom: verticalScale(2),
  },
  body: {
    fontSize: moderateScale(14),
    color: '#FFFFFF',
    opacity: 0.9,
    lineHeight: moderateScale(18),
  },
  severity: {
    fontSize: moderateScale(12),
    color: '#FFFFFF',
    fontWeight: '500',
    marginTop: verticalScale(4),
    opacity: 0.8,
  },
  dismissButton: {
    padding: scale(4),
  },
  dismissText: {
    fontSize: moderateScale(18),
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
});

export default NotificationBanner; 