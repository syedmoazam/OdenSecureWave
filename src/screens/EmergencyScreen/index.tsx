import {ReactNode, useEffect, useRef, useState} from "react";
import {Dimensions, StyleSheet, Text, Animated, View} from "react-native";
import AlertTab from "./tabs/AlertTab";
import {Colors} from "@/themes/Colors";
import Metrics from "@/utils/Metrics";
import Fonts from "@/themes/Fonts.ts";
import Icons from "@/constants/icons.ts";
import AppButton from "@/components/AppButton";
import SvgIcon from "@/components/SvgIcon";
import FAQTab from "./tabs/FAQTab";

const initialLayout = { width: Dimensions.get('window').width };

const tabRouteKeys = {
  ALERT: "ALERT_TAB",
  FAQ: "FAQ_TAB",
}

const TabIcons = {
  [tabRouteKeys.ALERT]: Icons.TRIANGLE_EXCLAMATION,
  [tabRouteKeys.FAQ]: Icons.MESSAGE,
}

const routes = [
  {
    key: tabRouteKeys.ALERT,
    title: 'Alert',
  },
  {
    key: tabRouteKeys.FAQ,
    title: 'FAQ',
  }
];



const tabWidth = initialLayout.width / routes.length;

const EmergencyScreen = () => {
  const [activeIndex, setActiveIndex] = useState(0);
  const indicatorX = useRef(new Animated.Value(0));

  useEffect(() => {
    Animated.spring(indicatorX.current, {
      toValue: activeIndex * tabWidth,
      useNativeDriver: false,
    }).start();
  }, [activeIndex]);

  const renderTabBar = () => (
    <View style={styles.tabBar}>
      {routes.map((route, i) => {
        const focused = i === activeIndex;
        const tabColor = focused ? Colors.WHITE : Colors.GRAY;
        return (
          <AppButton
            key={route.key}
            onPress={() => setActiveIndex(i)}
            style={styles.tabItem}
          >
            <SvgIcon
              name={TabIcons[route.key]}
              size={Metrics.icons.small}
              color={tabColor}
            />
            <Text
              style={[
                styles.label,
                { color: tabColor },
              ]}
            >
              {route.title}
            </Text>
          </AppButton>
        );
      })}
      <Animated.View
        style={[
          styles.indicator,
          { width: tabWidth, left: indicatorX.current },
        ]}
      />
    </View>
  );

  const renderScene = () => {
    switch (activeIndex) {
      case 0:
        return <AlertTab />;
      case 1:
        return <FAQTab />;
      default:
        return <AlertTab />;
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.emergencyHeader}>
        <Text style={styles.emergencyHeaderText}>EMERGENCY NOTICE</Text>
      </View>
      {renderTabBar()}
      <View style={styles.sceneContainer}>
        {renderScene()}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  emergencyHeader: {
    backgroundColor: Colors.RED,
    paddingVertical: Metrics.verticalScale(40),
    paddingHorizontal: Metrics.scale(16)
  },
  emergencyHeaderText: Fonts.Bold(Fonts.Size.large, Colors.WHITE),
  tabBar: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    backgroundColor: Colors.RED,
    paddingVertical: Metrics.verticalScale(12),
  },
  tabItem: {
    alignItems: 'center',
    justifyContent: 'center',
    rowGap: Metrics.verticalScale(12),
  },
  label: Fonts.Medium(Fonts.Size.normal, Colors.WHITE),
  indicator: {
    position: 'absolute',
    height: Metrics.verticalScale(3),
    backgroundColor: Colors.WHITE,
    bottom: 0,
    borderRadius: Metrics.scale(2),
  },
  sceneContainer: {
    flex: 1,
  },
})

export default EmergencyScreen;