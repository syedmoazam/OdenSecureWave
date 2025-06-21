import AppNavigator from "@/navigators/AppNavigator";
import {appMainContainer} from "@/themes/AppStyles";
import {SafeAreaProvider} from "react-native-safe-area-context";

function App() {
  return (
    <SafeAreaProvider style={appMainContainer}>
      <AppNavigator />
    </SafeAreaProvider>
  );
}

export default App;