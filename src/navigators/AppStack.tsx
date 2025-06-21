import {HomeScreen} from "@/screens/HomeScreen.tsx";
import DeviceAdminScreen from "@/screens/DeviceAdminScreen";
import routes from "@/constants/routes.ts";
import {createNativeStackNavigator} from "@react-navigation/native-stack";

const Stack = createNativeStackNavigator();
const AppStack = () => {
  return (
    <Stack.Navigator>
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