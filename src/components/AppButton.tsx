import React from "react";
import { TouchableOpacity } from "react-native";

let disableClick = false;
const debounceTime = 200;

interface IButtonView {
  disabled?: boolean;
  children?: React.ReactNode;
  style?: object;
  onPress?: () => void;
  onLongPress?: () => void;
  disableRipple?: boolean;
  enableClick?: boolean;
}

function AppButton({
  style,
  children,
  onPress,
  onLongPress,
  disableRipple = false,
  enableClick = false,
  disabled = false,
  ...rest
}: IButtonView) {
  const _onPress = () => {
    if (enableClick && onPress) {
      onPress();
    } else if (!disableClick) {
      disableClick = true;
      if (onPress) {
        onPress();
      }
      setTimeout(() => {
        disableClick = false;
      }, debounceTime);
    }
  };

  const _onLongPress = () => {
    if (enableClick && onLongPress) {
      onLongPress();
    } else if (!disableClick) {
      disableClick = true;
      if (onLongPress) {
        onLongPress();
      }
      setTimeout(() => {
        disableClick = false;
      }, debounceTime);
    }
  };

  const opacity = disableRipple ? 1 : 0.5;

  return (
    <TouchableOpacity
      style={style}
      {...rest}
      onPress={_onPress}
      activeOpacity={opacity}
      disabled={disabled}
      onLongPress={_onLongPress}
    >
      {children}
    </TouchableOpacity>
  );
}

export default AppButton;