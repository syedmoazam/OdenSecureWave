import React, { useRef, ReactNode, useImperativeHandle, forwardRef } from 'react';
import { StyleSheet } from 'react-native';
import RBSheet from 'react-native-raw-bottom-sheet';
import {Colors} from "@/themes/Colors.ts";
import Metrics from "@/utils/Metrics.ts";

interface BottomSheetProps {
  children: ReactNode;
}

export interface BottomSheetRef {
  open: () => void;
  close: () => void;
}

const BottomSheet = forwardRef<BottomSheetRef, BottomSheetProps>(({ children }, ref) => {
  // @ts-ignore
  const sheetRef = useRef<RBSheet>(null);

  useImperativeHandle(ref, () => ({
    open: () => {
      sheetRef.current?.open();
    },
    close: () => {
      sheetRef.current?.close();
    },
  }));

  return (
    <RBSheet
      ref={sheetRef}
      height={Metrics.verticalScale(400)}
      customStyles={{
        wrapper: styles.wrapper,
        container: styles.container
      }}
      customModalProps={{
        animationType: 'fade',
        statusBarTranslucent: true,
      }}
      customAvoidingViewProps={{
        enabled: false,
      }}>
      {children}
    </RBSheet>
  );
});

const styles = StyleSheet.create({
  container: {
    borderTopLeftRadius: Metrics.scale(20),
    borderTopRightRadius: Metrics.scale(20),
    paddingHorizontal: Metrics.scale(16),
    paddingVertical: Metrics.verticalScale(16),
  },
  wrapper: {
    backgroundColor: Colors.OVERLAY,
  }
})

export default BottomSheet;