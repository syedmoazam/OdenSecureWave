import {HomeScreen} from "@/screens/HomeScreen";
import DeviceAdminScreen from "@/screens/DeviceAdminScreen";
import routes from "@/constants/routes";
import {createNativeStackNavigator} from "@react-navigation/native-stack";
import EmergencyScreen from "@/screens/EmergencyScreen";

const Stack = createNativeStackNavigator();
const AppStack = () => {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name={routes.APP_STACK.EMERGENCY} component={EmergencyScreen} />
      <Stack.Screen name={routes.APP_STACK.HOME} component={HomeScreen} />
      <Stack.Screen 
        name={routes.APP_STACK.DEVICE_ADMIN} 
        component={DeviceAdminScreen}
        options={{ title: 'Device Admin Manager' }}
      />
    </Stack.Navigator>
  )
}

export default AppStack;