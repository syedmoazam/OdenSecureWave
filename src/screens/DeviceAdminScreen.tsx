import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  ScrollView,
  Platform,
} from 'react-native';
import DeviceAdminManager from '@/services/DeviceAdminManager';
import { useFocusEffect } from '@react-navigation/native';

const DeviceAdminScreen: React.FC = () => {
  const [isAdminEnabled, setIsAdminEnabled] = useState<boolean>(false);
  const [bootStatus, setBootStatus] = useState<boolean>(false);
  const [isCameraDisabled, setIsCameraDisabled] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);

  const deviceAdmin = DeviceAdminManager.getInstance();

  useEffect(() => {
    checkAdminStatus();
    checkBootStatus();
    checkCameraStatus();
  }, []);

  // Refresh UI when screen comes into focus
  useFocusEffect(
    React.useCallback(() => {
      console.log('DeviceAdminScreen focused - refreshing status');
      checkAdminStatus();
      checkBootStatus();
      // Small delay for camera status to avoid race conditions
      setTimeout(() => {
        checkCameraStatus();
      }, 200);
    }, [])
  );

  const checkAdminStatus = async () => {
    try {
      if (Platform.OS === 'android') {
        const enabled = await deviceAdmin.isDeviceAdminEnabled();
        setIsAdminEnabled(enabled);
      }
    } catch (error) {
      console.error('Error checking admin status:', error);
    }
  };

  const checkBootStatus = async () => {
    try {
      if (Platform.OS === 'android') {
        const status = await deviceAdmin.checkBootCompletedStatus();
        setBootStatus(status);
      }
    } catch (error) {
      console.error('Error checking boot status:', error);
    }
  };

  const checkCameraStatus = async () => {
    try {
      if (Platform.OS === 'android' && isAdminEnabled) {
        const disabled = await deviceAdmin.isCameraDisabled();
        setIsCameraDisabled(disabled);
      }
    } catch (error) {
      console.error('Error checking camera status:', error);
    }
  };

  const handleEnableAdmin = async () => {
    try {
      setLoading(true);
      console.log('Attempting to enable device admin...');
      const result = await deviceAdmin.enableDeviceAdmin();
      console.log('Device admin result:', result);
      Alert.alert('Device Admin', result);
      setTimeout(() => {
        checkAdminStatus();
        checkCameraStatus();
      }, 1000); // Check status after a delay
    } catch (error: any) {
      console.error('Device admin error:', error);
      Alert.alert('Error', error.message || 'Failed to enable device admin');
    } finally {
      setLoading(false);
    }
  };

  const handleLockDevice = async () => {
    try {
      setLoading(true);
      const result = await deviceAdmin.lockDevice();
      Alert.alert('Success', result);
    } catch (error: any) {
      Alert.alert('Error', error.message || 'Failed to lock device');
    } finally {
      setLoading(false);
    }
  };

  const handleSetPasswordPolicy = async () => {
    try {
      setLoading(true);
      const result = await deviceAdmin.setPasswordPolicy(6);
      Alert.alert('Success', result);
    } catch (error: any) {
      Alert.alert('Error', error.message || 'Failed to set password policy');
    } finally {
      setLoading(false);
    }
  };

  const handleWipeDevice = () => {
    Alert.alert(
      'WARNING!',
      'This will completely wipe your device. Are you absolutely sure?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'WIPE DEVICE',
          style: 'destructive',
          onPress: async () => {
            try {
              setLoading(true);
              const result = await deviceAdmin.wipeDevice(true);
              Alert.alert('Success', result);
            } catch (error: any) {
              Alert.alert('Error', error.message || 'Failed to wipe device');
            } finally {
              setLoading(false);
            }
          },
        },
      ]
    );
  };

  const handleResetBootStatus = async () => {
    try {
      setLoading(true);
      const result = await deviceAdmin.setBootCompletedStatus(false);
      Alert.alert('Success', result);
      checkBootStatus();
    } catch (error: any) {
      Alert.alert('Error', error.message || 'Failed to reset boot status');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleCamera = async () => {
    try {
      setLoading(true);
      const newStatus = !isCameraDisabled;
      const result = await deviceAdmin.setCameraDisabled(newStatus);
      Alert.alert('Success', result);
      setIsCameraDisabled(newStatus);
    } catch (error: any) {
      Alert.alert('Error', error.message || 'Failed to toggle camera');
    } finally {
      setLoading(false);
    }
  };

  const handleTestBridge = async () => {
    try {
      setLoading(true);
      const result = await deviceAdmin.testBridge();
      Alert.alert('Bridge Test', result);
    } catch (error: any) {
      Alert.alert('Bridge Test Failed', error.message || 'Bridge is not working');
    } finally {
      setLoading(false);
    }
  };

  const handleDisableAdmin = async () => {
    try {
      Alert.alert(
        'Disable Device Admin',
        'Are you sure you want to disable device admin? This will remove all administrative privileges.',
        [
          { text: 'Cancel', style: 'cancel' },
          {
            text: 'Disable',
            style: 'destructive',
            onPress: async () => {
              try {
                setLoading(true);
                console.log('Attempting to disable device admin...');
                const result = await deviceAdmin.disableDeviceAdmin();
                console.log('Device admin disable result:', result);
                
                // Show success message
                Alert.alert('Device Admin', result);
                
                // Refresh all statuses immediately
                await Promise.all([
                  checkAdminStatus(),
                  checkCameraStatus(),
                  checkBootStatus()
                ]);
                
              } catch (error: any) {
                console.error('Device admin disable error:', error);
                
                // Show user-friendly error message
                if (error.message?.includes('manually')) {
                  Alert.alert(
                    'Manual Action Required',
                    'Device admin needs to be disabled manually. Go to:\n\nSettings > Security > Device admin apps > OdenSecureWave > Deactivate',
                    [
                      { text: 'OK', onPress: () => {
                        // Refresh status anyway in case it was disabled
                        setTimeout(() => {
                          checkAdminStatus();
                          checkCameraStatus();
                          checkBootStatus();
                        }, 1000);
                      }}
                    ]
                  );
                } else {
                  Alert.alert('Error', error.message || 'Failed to disable device admin');
                }
              } finally {
                setLoading(false);
              }
            }
          }
        ]
      );
    } catch (error: any) {
      console.error('Device admin disable error:', error);
      Alert.alert('Error', error.message || 'Failed to disable device admin');
    }
  };

  if (Platform.OS !== 'android') {
    return (
      <View style={styles.container}>
        <Text style={styles.errorText}>
          Device Admin functionality is only available on Android
        </Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Device Admin Manager</Text>
      
      <View style={styles.statusContainer}>
        <Text style={styles.statusText}>
          Admin Status: {isAdminEnabled ? '✅ Enabled' : '❌ Disabled'}
        </Text>
        <Text style={styles.statusText}>
          Boot Status: {bootStatus ? '✅ From Boot' : '❌ Not from Boot'}
        </Text>
        <Text style={styles.statusText}>
          Camera Status: {isCameraDisabled ? '🚫 Disabled' : '📷 Enabled'}
        </Text>
      </View>

      <TouchableOpacity
        style={styles.button}
        onPress={handleTestBridge}
        disabled={loading}
      >
        <Text style={styles.buttonText}>🔧 Test Bridge</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, !isAdminEnabled && styles.primaryButton]}
        onPress={handleEnableAdmin}
        disabled={loading}
      >
        <Text style={styles.buttonText}>
          {isAdminEnabled ? 'Admin Already Enabled' : 'Enable Device Admin'}
        </Text>
      </TouchableOpacity>

      {isAdminEnabled && (
        <TouchableOpacity
          style={[styles.button, styles.dangerButton]}
          onPress={handleDisableAdmin}
          disabled={loading}
        >
          <Text style={styles.buttonText}>Disable Device Admin</Text>
        </TouchableOpacity>
      )}

      <TouchableOpacity
        style={[styles.button, !isAdminEnabled && styles.disabledButton]}
        onPress={handleLockDevice}
        disabled={!isAdminEnabled || loading}
      >
        <Text style={styles.buttonText}>Lock Device</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, !isAdminEnabled && styles.disabledButton]}
        onPress={handleSetPasswordPolicy}
        disabled={!isAdminEnabled || loading}
      >
        <Text style={styles.buttonText}>Set Password Policy (Min 6 chars)</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[
          styles.button,
          !isAdminEnabled && styles.disabledButton,
          isCameraDisabled ? styles.dangerButton : styles.primaryButton
        ]}
        onPress={handleToggleCamera}
        disabled={!isAdminEnabled || loading}
      >
        <Text style={styles.buttonText}>
          {isCameraDisabled ? '📷 Enable Camera' : '🚫 Disable Camera'}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.button}
        onPress={handleResetBootStatus}
        disabled={loading}
      >
        <Text style={styles.buttonText}>Reset Boot Status</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.button, styles.dangerButton, !isAdminEnabled && styles.disabledButton]}
        onPress={handleWipeDevice}
        disabled={!isAdminEnabled || loading}
      >
        <Text style={styles.buttonText}>⚠️ WIPE DEVICE (DANGER!)</Text>
      </TouchableOpacity>

      <View style={styles.infoContainer}>
        <Text style={styles.infoText}>
          Note: Device Admin permissions are required for most security operations.
          Enable device admin first before using other features.
        </Text>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 20,
    color: '#333',
  },
  statusContainer: {
    backgroundColor: '#fff',
    padding: 15,
    borderRadius: 8,
    marginBottom: 20,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  statusText: {
    fontSize: 16,
    marginBottom: 5,
    color: '#333',
  },
  button: {
    backgroundColor: '#007AFF',
    padding: 15,
    borderRadius: 8,
    marginBottom: 10,
    alignItems: 'center',
  },
  primaryButton: {
    backgroundColor: '#34C759',
  },
  dangerButton: {
    backgroundColor: '#FF3B30',
  },
  disabledButton: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  errorText: {
    fontSize: 18,
    color: '#FF3B30',
    textAlign: 'center',
    marginTop: 50,
  },
  infoContainer: {
    backgroundColor: '#fff3cd',
    padding: 15,
    borderRadius: 8,
    marginTop: 20,
    borderLeftWidth: 4,
    borderLeftColor: '#ffc107',
  },
  infoText: {
    fontSize: 14,
    color: '#856404',
    lineHeight: 20,
  },
});

export default DeviceAdminScreen;
