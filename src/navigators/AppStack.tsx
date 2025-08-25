import {HomeScreen} from "@/screens/HomeScreen";
import DeviceAdminScreen from "@/screens/DeviceAdminScreen";
import InitializationScreen from "@/screens/InitializationScreen";
import routes from "@/constants/routes";
import {createNativeStackNavigator} from "@react-navigation/native-stack";
import EmergencyScreen from "@/screens/EmergencyScreen";
import { useDeviceData } from "@/contexts/DeviceDataContext";

const Stack = createNativeStackNavigator();
const AppStack = () => {
  const { isInitialized } = useDeviceData();

  // Show Home screen for all other cases
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      {!isInitialized && (
        <Stack.Screen name="Initialization" component={InitializationScreen} />
      )}
      <Stack.Screen name={routes.APP_STACK.HOME} component={HomeScreen} />
      <Stack.Screen name={routes.APP_STACK.EMERGENCY} component={EmergencyScreen} />
      
      <Stack.Screen 
        name={routes.APP_STACK.DEVICE_ADMIN} 
        component={DeviceAdminScreen}
        options={{ 
          title: 'Device Admin Manager',
          headerShown: true
        }}
      />
    </Stack.Navigator>
  );
}

export default AppStack;