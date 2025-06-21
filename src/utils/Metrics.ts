import { Dimensions } from "react-native";
import {
    moderateScale,
    moderateVerticalScale,
} from "react-native-size-matters";

const { width: screenWidth, height: screenHeight } = Dimensions.get("window");

const widthRatio = (size: number, factor = 0) => moderateScale(size, factor); // scaleHorizontal(size); //  // scaleHorizontal(size);
const heightRatio = (size: number, factor = 0) =>
    moderateVerticalScale(size, factor); // scaleVertical(size); //

const DEFAULT_SCALING_FACTOR = 0.3;

const scaleSvg = (size: number) => widthRatio(size, DEFAULT_SCALING_FACTOR);
const verticalScaleSvg = (size: number) =>
    heightRatio(size, DEFAULT_SCALING_FACTOR);

// Sometimes you don't want to scale everything in a linear manner, that's where moderateScale comes in.
// The cool thing about it is that you can control the resize factor (default is 0.5).
// If normal scale will increase your size by +2X, moderateScale will only increase it by +X, for example:
// moderateScale(10) = 15
// moderateScale(10, 0.1) = 11
const generatedFontSize = (size: number, factor = 0.1) =>
    moderateScale(size, factor);

/*const tabBarHeight = utilService.isPlatformAndroid()
  ? widthRatio(90)
  : widthRatio(90);*/

export default {
    DEFAULT_SCALING_FACTOR,
    verticalScale: heightRatio,
    scale: widthRatio,
    scaleSvg,
    verticalScaleSvg,
    screenWidth,
    screenHeight,
    generatedFontSize,
    // tabBarHeight,

    xxsmallMargin: widthRatio(2),
    xsmallMargin: widthRatio(4),
    smallMargin: widthRatio(8),
    baseMargin: widthRatio(16),
    largeBaseMargin: widthRatio(22),
    xLargeBaseMargin: heightRatio(32),
    xxLargeBaseMargin: heightRatio(42),
    horizontalLineHeight: heightRatio(1),

    icons: {
        tiny: heightRatio(18),
        small: heightRatio(24),
        normal: heightRatio(32),
        medium: heightRatio(48),
        large: heightRatio(64),
        xl: heightRatio(128),
    },
    images: {
        xSmall: heightRatio(15),
        small: heightRatio(20),
        medium: heightRatio(40),
        large: heightRatio(55),
        xLarge: heightRatio(75),
        avatar: heightRatio(90),
        logo: heightRatio(200),
        radius: heightRatio(100),
        coverWidth: screenWidth,
        coverHeight: screenWidth / 2,
    },
};