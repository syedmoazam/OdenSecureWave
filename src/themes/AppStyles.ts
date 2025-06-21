import Fonts from "@/themes/Fonts.ts";
import {Colors} from "@/themes/Colors.ts";
import Metrics from "@/utils/Metrics.ts";

const appMainContainer = {
  backgroundColor: Colors.WHITE,
  flexGrow: 1,
  paddingHorizontal: Metrics.scale(16),
  paddingVertical: Metrics.verticalScale(16),
};

const centerAlign = {
  justifyContent: "center",
  alignItems: "center",
};

const inputControl = {
  flex: 1,
  height: Metrics.verticalScale(50),
  ...Fonts.Regular(16, Colors.CHARCOAL_GREY),
};

export { appMainContainer, inputControl, centerAlign };