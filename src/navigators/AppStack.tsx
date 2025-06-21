import {HomeScreen} from "@/screens/HomeScreen.tsx";
import routes from "@/constants/routes.ts";
import {createNativeStackNavigator} from "@react-navigation/native-stack";

const Stack = createNativeStackNavigator();
const AppStack = () => {
  return (
    <Stack.Navigator>
      <Stack.Screen name={routes.APP_STACK.HOME} component={HomeScreen} />
    </Stack.Navigator>
  )
}

export default AppStack;