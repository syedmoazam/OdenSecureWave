import {HomeScreen} from "@/screens/HomeScreen";
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
    </Stack.Navigator>
  );
}

export default AppStack;