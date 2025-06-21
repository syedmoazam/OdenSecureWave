import React, {ReactNode} from 'react';
import {StyleSheet, View} from "react-native";
import Metrics from "@/utils/Metrics.ts";

const ScreenLayout = ({ children }: { children: ReactNode }) => (
  <View style={styles.container}>
    {children}
  </View>
);

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: Metrics.scale(16),
    paddingVertical: Metrics.verticalScale(16),
    flex: 1,
  }
})

export default ScreenLayout;