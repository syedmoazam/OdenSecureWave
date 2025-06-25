import React from 'react';
import Metrics from "@/utils/Metrics.ts";
import Icons from "@/constants/icons.ts";

function getSvgIcon(name: string) {
  switch (name) {
    case Icons.BUILDING:
      return require('@/assets/icons/building.svg').default;
    case Icons.CHEVRON_RIGHT:
      return require('@/assets/icons/chevron-right.svg').default;
    case Icons.BRIEFCASE:
      return require('@/assets/icons/briefcase.svg').default;
    case Icons.SECURITY:
      return require('@/assets/icons/security.svg').default;
    case Icons.GEAR:
      return require('@/assets/icons/gear.svg').default;
    case Icons.CIRCLE_CHECK:
      return require('@/assets/icons/circle-check.svg').default;
    case Icons.TRIANGLE_EXCLAMATION:
      return require('@/assets/icons/triangle-exclamation.svg').default;
    case Icons.MESSAGE:
      return require('@/assets/icons/message.svg').default;
    case Icons.CHEVRON_DOWN:
      return require('@/assets/icons/chevron-down.svg').default;
    case Icons.CHEVRON_UP:
      return require('@/assets/icons/chevron-up.svg').default;
    default:
  }
}

const ICON_COLOR_PROP: Record<string, 'fill' | 'stroke'> = {
  [Icons.BUILDING]: 'fill',
  [Icons.CHEVRON_RIGHT]: 'fill',
  [Icons.BRIEFCASE]: 'fill',
  [Icons.SECURITY]: 'fill',
  [Icons.GEAR]: 'fill',
  [Icons.CIRCLE_CHECK]: 'fill',
  [Icons.TRIANGLE_EXCLAMATION]: 'fill',
  [Icons.MESSAGE]: 'fill',
};

const SvgIcon = ({
  size = Metrics.icons.normal,
  name = "",
  color = "",
  ...rest
}) => {
  const Component = getSvgIcon(name);

  if (!Component) {
    console.error(`Invalid SVG icon: ${name}`);
    return null;
  }

  const colorProp = ICON_COLOR_PROP[name] || 'fill';

  return (
    <Component
      {...rest}
      {...{ [colorProp]: color }}
      width={rest.width ?? size}
      height={rest.height ?? size}
    />
  );
}
export default SvgIcon;