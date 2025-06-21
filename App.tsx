import AppNavigator from "@/navigators/AppNavigator.tsx";
import {appMainContainer} from "./src/themes/AppStyles";
import {SafeAreaProvider} from "react-native-safe-area-context";

function App() {
  return (
    <SafeAreaProvider style={appMainContainer}>
      <AppNavigator />
    </SafeAreaProvider>
  );
}

export default App;