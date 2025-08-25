import React from 'react';
import { View, Text, ActivityIndicator, StyleSheet } from 'react-native';
import { Colors } from '@/themes/Colors';
import Fonts from '@/themes/Fonts';
import Metrics from '@/utils/Metrics';

const InitializationScreen: React.FC = () => {
  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color={Colors.PRIMARY_BLUE} />
      <Text style={styles.loadingText}>Initializing Device...</Text>
      <Text style={styles.subText}>Please wait while we set up your device</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.WHITE,
    paddingHorizontal: Metrics.scale(20),
  },
  loadingText: {
    ...Fonts.Bold(Fonts.Size.large),
    color: Colors.DARK,
    marginTop: Metrics.verticalScale(20),
    textAlign: 'center',
  },
  subText: {
    ...Fonts.Regular(Fonts.Size.normal),
    color: Colors.CHARCOAL_GREY,
    marginTop: Metrics.verticalScale(8),
    textAlign: 'center',
  },
});

export default InitializationScreen;
