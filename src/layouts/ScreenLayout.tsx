import React from 'react';
import {StyleProp, StyleSheet, View, ViewStyle} from "react-native";
import Metrics from "@/utils/Metrics.ts";

interface ScreenLayoutProps {
  children: React.ReactNode;
  containerStyle?: StyleProp<ViewStyle>;
}

const ScreenLayout: React.FC<ScreenLayoutProps> = ({ children, containerStyle }) => (
  <View style={[styles.container, containerStyle]}>
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