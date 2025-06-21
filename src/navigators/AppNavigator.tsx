import {NavigationContainer} from "@react-navigation/native";
import {navigationRef} from "@/services/navigationService.ts";
import {StatusBar, useColorScheme} from "react-native";
import AppStack from "@/navigators/AppStack.tsx";

const AppNavigator = () => {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <NavigationContainer ref={navigationRef}>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
       <AppStack />
    </NavigationContainer>
  )
};

export default AppNavigator;