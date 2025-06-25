import React, { useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Animated,
  LayoutChangeEvent,
} from 'react-native';
import SvgIcon from './SvgIcon';
import icons from '@/constants/icons';
import Card from './Card';
import AppButton from './AppButton';
import Fonts from '@/themes/Fonts';
import Metrics from '@/utils/Metrics';
import { Colors } from '@/themes/Colors';
interface AccordionProps {
  title: string;
  children: React.ReactNode;
}

const Accordion: React.FC<AccordionProps> = ({ title, children }) => {
  const [contentHeight, setContentHeight] = useState<number | null>(null);
  const animation = useRef(new Animated.Value(0)).current;
  const rotateAnim = useRef(new Animated.Value(0)).current;
  const isOpen = useRef(false);

  const toggle = () => {
    if (contentHeight === null) return;
    const isOpened = isOpen.current;
    Animated.parallel([
      Animated.timing(animation, {
        toValue: isOpened ? 0 : contentHeight,
        duration: 300,
        useNativeDriver: false,
      }),
      Animated.timing(rotateAnim, {
        toValue: isOpened ? 0 : 1,
        duration: 300,
        useNativeDriver: false,
      }),
    ]).start();

    isOpen.current = !isOpened;
  };

  const onLayout = (e: LayoutChangeEvent) => {
    if (contentHeight === null) {
      const height = e.nativeEvent.layout.height;
      setContentHeight(height);
      animation.setValue(0);
      rotateAnim.setValue(0);
    }
  };

  const rotateStyle = {
    transform: [
      {
        rotate: rotateAnim.interpolate({
          inputRange: [0, 1],
          outputRange: ['0deg', '180deg'],
        }),
      },
    ],
  };

  return (
    <Card>
      <AppButton onPress={toggle} style={styles.header}>
        <Text style={styles.headerText}>{title}</Text>
        <Animated.View style={rotateStyle}>
          <SvgIcon color={Colors.CHARCOAL_GREY} size={Metrics.icons.tiny} name={icons.CHEVRON_DOWN} />
        </Animated.View>
      </AppButton>
      <Animated.View style={{ height: animation, overflow: 'hidden' }}>
        {children}
      </Animated.View>
      {contentHeight === null && (
        <View style={styles.hidden} onLayout={onLayout}>
          {children}
        </View>
      )}
    </Card>
  );
};

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerText: Fonts.Medium(Fonts.Size.normal),
  hidden: {
    position: 'absolute',
    top: -9999,
    left: 0,
    right: 0,
    opacity: 0,
  },
});

export default Accordion;
