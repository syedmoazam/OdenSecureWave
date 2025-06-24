import React from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import Card from '@/components/Card';
import SvgIcon from '@/components/SvgIcon';
import ScreenLayout from '@/layouts/ScreenLayout';
import Icons from '@/constants/icons';
import AppButton from '@/components/AppButton';
import Metrics from '@/utils/Metrics';
import { Colors } from '@/themes/Colors';
import Fonts from '@/themes/Fonts';

const supportContent = {
  contact: "+923337257968",
  description: "Your device has been locked due to overdue payment",
  notification_desc: "This device is owned by Secure wave.",
  notification_title: "SECURE WAVE",
  title: "PAYMENT OVERDUE"
};

const AlertTab = () => {
  const onPressContactSupport = () => {
    Alert.alert(
      supportContent.notification_title,
      `${supportContent.notification_desc}\n\nContact: ${supportContent.contact}`,
      [
        {
          text: "Close",
          style: "cancel"
        },
      ]
    );
  }
  return (
    <ScreenLayout containerStyle={styles.container}>
      <Card cardStyle={styles.cardStyle}>
        <View style={styles.cardHeader}>
          <SvgIcon color={Colors.RED} size={Metrics.icons.small} name={Icons.TRIANGLE_EXCLAMATION} />
          <Text style={styles.cardTitle}>{supportContent.title}</Text>
        </View>
        <Text style={styles.cardDescription}>{supportContent.description}</Text>
      </Card>
      <AppButton onPress={onPressContactSupport} style={styles.contactBtn}>
        <Text style={styles.buttonText}>Contact Support</Text>
      </AppButton>
    </ScreenLayout>
)};

const styles = StyleSheet.create({
  container: {
    rowGap: Metrics.verticalScale(16),
  },
  cardStyle: {
    rowGap: Metrics.scale(12),
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: "center",
    columnGap: Metrics.scale(8),
  },
  cardTitle: Fonts.Bold(Fonts.Size.normal, Colors.RED),
  cardDescription: Fonts.Regular(Fonts.Size.small),
  contactBtn: {
    alignItems: "center",
    paddingVertical: Metrics.verticalScale(12),
    backgroundColor: Colors.PRIMARY_BLUE,
    borderRadius: Metrics.scale(12),
  },
  buttonText: Fonts.Regular(Fonts.Size.small, Colors.WHITE)
})
export default AlertTab;
