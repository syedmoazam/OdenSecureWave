import Metrics from "@/utils/Metrics.ts";
import {APP_PRIMARY_TEXT} from "@/themes/Colors.ts";

export default class Fonts {
  static FontFamily = {
    default: "Roboto",
  };

  static Type = {
    Bold: "Bold",
    Medium: "Medium",
    Regular: "Regular",
  };

  static Size = {
    xxxxSmall: 9,
    xxxSmall: 11,
    xxSmall: 13,
    xSmall: 14,
    small: 15,
    normal: 16,
    medium: 18,
    large: 20,
    xLarge: 23,
    xxLarge: 28,
    xxxLarge: 30,
    xxxxLarge: 31,
    huge: 34,
    xhuge: 37,
    xxhuge: 40,
    xxxhuge: 43,
  };

  static font = (
    type = Fonts.Type.Regular,
    size = Fonts.Size.normal,
    color = APP_PRIMARY_TEXT,
  ) => {
    return {
      lineHeight: Metrics.generatedFontSize(20),
      fontFamily: Fonts.FontFamily.default + "-" + type,
      fontSize: Metrics.generatedFontSize(size),
      color,
    };
  };

  // Fonts;
  static Regular = (size = Fonts.Size.normal, color = APP_PRIMARY_TEXT) => {
    return Fonts.font(Fonts.Type.Regular, Metrics.scale(size), color);
  };
  static Bold = (size = Fonts.Size.normal, color = APP_PRIMARY_TEXT) => {
    return Fonts.font(Fonts.Type.Bold, Metrics.scale(size), color);
  };
  static Medium = (size = Fonts.Size.normal, color = APP_PRIMARY_TEXT) => {
    return Fonts.font(Fonts.Type.Medium, Metrics.scale(size), color);
  };
}