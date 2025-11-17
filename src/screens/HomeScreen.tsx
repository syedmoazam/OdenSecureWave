import {FlatList, StyleSheet, Text, View, Alert} from 'react-native';
import SvgIcon from "@/components/SvgIcon.tsx";
import Icons from "@/constants/icons.ts";
import Metrics from "@/utils/Metrics.ts";
import {Colors} from "@/themes/Colors.ts";
import Fonts from "@/themes/Fonts.ts";
import AppButton from "@/components/AppButton.tsx";
import BottomSheet, {BottomSheetRef} from "@/components/BottomSheet.tsx";
import {useRef, useState, useEffect, useCallback} from "react";
import ScreenLayout from "@/layouts/ScreenLayout.tsx";
import { useCompanyData } from '@/hooks/useCompanyData';
import { useDeviceData } from '@/hooks/useDeviceData';
import DeviceAdminManager from '@/services/DeviceAdminManager';


interface ICompany {
  id: string;
  name: string;
  branch: string;
}

export function HomeScreen() {
  const { 
    isMonitoringEnabled, 
    isSettingUp, 
    setupDevice, 
    deviceData
  } = useDeviceData();
  const [company, setCompany] = useState<ICompany | null>(null);
  const bottomSheetRef = useRef<BottomSheetRef>(null);

  const onPressBranchCard = () => {
    if (bottomSheetRef.current && !isMonitoringEnabled) {
      bottomSheetRef.current.open();
    }
  }

  const onSelectCompany = (companyToSelect: ICompany) => {
    setCompany(companyToSelect);
    if (bottomSheetRef.current) {
      bottomSheetRef.current.close();
    }
  }

  const handleStartSetup = async () => {
    if (!company) {
      return;
    }

    try {
      await setupDevice(company);

      Alert.alert(
        'Setup Complete',
        'Device has been successfully registered for monitoring. Background service has been enabled.',
        [{ text: 'OK' }]
      );

    } catch (error: any) {
      console.error('Error during device setup:', error);
      Alert.alert(
        'Setup Failed',
        error.message || 'Failed to setup device monitoring. Please try again.',
        [{ text: 'OK' }]
      );
    }
  }


  useEffect(() => {
    if (deviceData?.company) {
      setCompany(deviceData.company)
    }
  }, [deviceData])

  // Determine what to show based on monitoring status
  const showSetup = !isMonitoringEnabled;
  const showMonitoring = isMonitoringEnabled;
  const showCompanySelection = !company && showSetup;

  return (
    <ScreenLayout>
      <View style={styles.container}>
        {showMonitoring ? (
          // Show monitoring active status
          <View style={[styles.card, styles.lightGreenBg]}>
            <View style={styles.iconTextContainer}>
              <View style={styles.iconContainer}>
                <SvgIcon
                  color={Colors.GREEN}
                  name={Icons.CIRCLE_CHECK}
                  size={Metrics.icons.small}
                />
              </View>
              <Text style={styles.greenMediumLabel}>Monitoring Active</Text>
            </View>
            <Text style={styles.blueText}>
              Your device is being monitored and the background service is running. 
              {company && ` Connected to ${company.name}-${company.branch}.`}
            </Text>
          </View>
        ) : showCompanySelection ? (
          // Show company selection flow
          <>
            <View style={[styles.card, styles.lightOrangeBg]}>
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.ORANGE}
                    name={Icons.BUILDING}
                    size={Metrics.icons.small}
                  />
                </View>
                <Text style={styles.orangeBoldLabel}>Select Your Company</Text>
              </View>
              <Text style={styles.orangeText}>
                Please select your company and branch to begin the device setup process.
              </Text>
            </View>
            <AppButton
              onPress={onPressBranchCard}
              style={[styles.card, styles.lightBlueBg]}
            >
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.BLUE}
                    size={Metrics.icons.small}
                    name={Icons.BRIEFCASE}
                  />
                </View>
                <Text style={styles.blueMediumLabel}>Select Company & Branch</Text>
                <AppButton style={styles.branchIconButton}>
                  <SvgIcon
                    color={Colors.BLUE}
                    size={Metrics.icons.tiny}
                    name={Icons.CHEVRON_RIGHT}
                  />
                </AppButton>
              </View>
            </AppButton>
          </>
        ) : company && showSetup ? (
          // Show setup flow when company is selected but service not enabled
          <>
            <View style={[styles.card, styles.lightBlueBg]}>
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.BLUE}
                    name={Icons.SECURITY}
                    size={Metrics.icons.small}
                  />
                </View>
                <Text style={styles.blueBoldLabel}>Device Setup Required</Text>
              </View>
              <Text style={styles.blueText}>
                To ensure your device security and management capabilities, we need to complete the setup process. This will enable important security features and device management controls.
              </Text>
            </View>
            <AppButton
              onPress={onPressBranchCard}
              style={[styles.card, styles.lightGreenBg]}
            >
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    color={Colors.GREEN}
                    size={Metrics.icons.small}
                    name={Icons.CIRCLE_CHECK}
                  />
                </View>
                <Text style={styles.greenMediumLabel}>{company.name}-{company.branch}</Text>
                <AppButton style={styles.branchIconButton}>
                  <SvgIcon
                    color={Colors.GREEN}
                    size={Metrics.icons.tiny}
                    name={Icons.CHEVRON_RIGHT}
                  />
                </AppButton>
              </View>
            </AppButton>
            <View style={[styles.card, styles.lightPurpleBg]}>
              <View style={styles.iconTextContainer}>
                <View style={styles.iconContainer}>
                  <SvgIcon
                    name={Icons.GEAR}
                    color={Colors.PURPLE}
                    size={Metrics.icons.small}
                  />
                </View>
                <Text style={styles.purpleBoldLabel}>Device Setup</Text>
              </View>
              <Text style={styles.purpleText}>
                Complete the device setup to enable security features and management controls.{"\n\n"}
              </Text>
              <AppButton 
                disabled={isSettingUp} 
                style={styles.setupBtn}
                onPress={handleStartSetup}
              >
                <Text style={styles.whiteMediumText}>
                  {isSettingUp ? 'Setting Up...' : 'Start Setup'}
                </Text>
              </AppButton>
            </View>
          </>
        ) : (
          // Fallback: show basic status
          <View style={[styles.card, styles.lightBlueBg]}>
            <View style={styles.iconTextContainer}>
              <View style={styles.iconContainer}>
                <SvgIcon
                  color={Colors.BLUE}
                  name={Icons.SECURITY}
                  size={Metrics.icons.small}
                />
              </View>
              <Text style={styles.blueBoldLabel}>Device Status</Text>
            </View>
            <Text style={styles.blueText}>
              Monitoring: {isMonitoringEnabled ? 'Active' : 'Inactive'}
            </Text>
          </View>
        )}


        <BottomSheet ref={bottomSheetRef}>
          <CompanySelector 
            onSelectCompany={onSelectCompany}
          />
        </BottomSheet>
      </View>
    </ScreenLayout>
  );
}

interface ICompanySelectorProps {
  onSelectCompany: (company: ICompany) => void;
}

const CompanySelector = ({ 
  onSelectCompany, 
}: ICompanySelectorProps) => {
  const { data: companyData, loading, error, refreshData } = useCompanyData();
  return (
    <View style={styles.container}>
      <View style={styles.iconTextContainer}>
        <View style={[styles.iconContainer, styles.companySelectorIconContainer]}>
          <SvgIcon color={Colors.BLUE} name={Icons.BUILDING} size={Metrics.icons.small}/>
        </View>
        <Text style={styles.companySelectorLabel}>Select Company</Text>
      </View>
      <Text style={styles.companySelectorText}>
        Choose your company and branch to proceed with the setup
      </Text>
      
      {error && (
        <View style={styles.errorContainer}>
          <Text style={styles.errorText}>{error}</Text>
          <Text style={styles.retryText} onPress={refreshData}>
            Tap to retry
          </Text>
        </View>
      )}
      
      <FlatList
        style={styles.listStyle}
        contentContainerStyle={styles.companyListContentContainer}
        data={companyData}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <AppButton onPress={() => onSelectCompany(item)} style={styles.companyCard}>
            <View style={styles.companyIconContainer}>
              <SvgIcon color={Colors.DARK} size={Metrics.icons.tiny} name={Icons.BUILDING} />
            </View>
            <View style={styles.companyDetailsContainer}>
              <Text style={styles.companyName}>{item.name}</Text>
              <Text style={styles.branchName}>{item.branch}</Text>
            </View>
            <View style={styles.rightIconContainer}>
              <SvgIcon size={Metrics.icons.tiny} color={Colors.CHARCOAL_GREY} name={Icons.CHEVRON_RIGHT} />
            </View>
          </AppButton>
        )}
        refreshing={loading}
        onRefresh={refreshData}
      />
    </View>
  )
}

const styles = StyleSheet.create({
  // Containers
  container: {
    flex: 1,
    rowGap: Metrics.verticalScale(16)
  },
  card: {
    paddingHorizontal: Metrics.scale(16),
    paddingVertical: Metrics.verticalScale(16),
    borderRadius: Metrics.scale(14),
    rowGap: Metrics.verticalScale(12)
  },
  companyCard: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: Metrics.scale(10),
    backgroundColor: Colors.LIGHT_GRAY,
    paddingVertical: Metrics.verticalScale(16),
    paddingHorizontal: Metrics.verticalScale(16),
    borderRadius: Metrics.scale(10),
    borderWidth: 1,
    borderColor: Colors.GRAY,
  },
  companyDetailsContainer: {
    rowGap: Metrics.scale(4),
  },
  companyIconContainer: {
    borderWidth: 1,
    borderColor: Colors.GRAY,
    backgroundColor: Colors.WHITE,
    paddingHorizontal: Metrics.scale(10),
    paddingVertical: Metrics.verticalScale(10),
    borderRadius: Metrics.scale(10),
  },
  companyListContentContainer: { rowGap: Metrics.verticalScale(8) },
  companyListStyle: { flex: 1 },
  companySelectorIconContainer: {
    backgroundColor: Colors.LIGHT_BLUE
  },
  iconContainer: {
    backgroundColor: Colors.WHITE,
    borderRadius: Metrics.scale(16),
    paddingHorizontal: Metrics.scale(12),
    paddingVertical: Metrics.verticalScale(12)
  },
  iconTextContainer: {
    flexDirection: "row",
    alignItems: "center",
    gap: Metrics.scale(16)
  },
  listStyle: { flex: 1 },
  rightIconContainer: {
    flexGrow: 1,
    flexDirection: "row",
    justifyContent: "flex-end",
  },
  branchIconButton: {
    flexDirection: "row",
    justifyContent: "flex-end",
    flexGrow: 1,
  },
  setupBtn: {
    alignItems: "center",
    paddingVertical: Metrics.verticalScale(12),
    backgroundColor: Colors.PURPLE,
    borderRadius: Metrics.scale(10),
  },
  buttonContainer: {
    flexDirection: "row",
    gap: Metrics.scale(8),
    marginTop: Metrics.verticalScale(8),
  },
  controlBtn: {
    flex: 1,
    alignItems: "center",
    paddingVertical: Metrics.verticalScale(12),
    borderRadius: Metrics.scale(10),
    marginHorizontal: Metrics.scale(4),
  },

  // Text
  blueBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.BLUE),
  blueMediumLabel: Fonts.Medium(Fonts.Size.normal, Colors.BLUE),
  greenMediumLabel: Fonts.Medium(Fonts.Size.normal, Colors.GREEN),
  companySelectorLabel: Fonts.Bold(Fonts.Size.xLarge, Colors.BLUE),
  companyName: Fonts.Bold(Fonts.Size.normal, Colors.DARK),
  orangeBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.ORANGE),
  purpleBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.PURPLE),
  redBoldLabel: Fonts.Bold(Fonts.Size.large, "#FF6B6B"),
  blueText: Fonts.Regular(Fonts.Size.xSmall, Colors.BLUE),
  companySelectorText: Fonts.Regular(Fonts.Size.xSmall, Colors.BLUE),
  branchName: Fonts.Regular(Fonts.Size.xxSmall, Colors.CHARCOAL_GREY),
  orangeText: Fonts.Regular(Fonts.Size.xSmall, Colors.ORANGE),
  purpleText: Fonts.Regular(Fonts.Size.xSmall, Colors.PURPLE),
  redText: Fonts.Regular(Fonts.Size.xSmall, "#FF6B6B"),
  whiteMediumText: Fonts.Medium(Fonts.Size.normal, Colors.WHITE),

  // Backgrounds
  lightBlueBg: { backgroundColor: Colors.LIGHT_BLUE },
  lightOrangeBg: { backgroundColor: Colors.LIGHT_ORANGE },
  lightPurpleBg: { backgroundColor: Colors.LIGHT_PURPLE },
  lightGreenBg: { backgroundColor: Colors.LIGHT_GREEN },
  lightRedBg: { backgroundColor: "#FFE6E6" },
  errorContainer: {
    paddingHorizontal: Metrics.scale(20),
    paddingVertical: Metrics.verticalScale(12),
    backgroundColor: Colors.RED + '20',
    marginBottom: Metrics.verticalScale(8),
  },
  errorText: {
    ...Fonts.Regular(Fonts.Size.small),
    textAlign: 'center',
    color: Colors.RED,
    marginBottom: Metrics.verticalScale(4),
  },
  retryText: {
    ...Fonts.Medium(Fonts.Size.small),
    color: Colors.PRIMARY_BLUE,
    textDecorationLine: 'underline',
    textAlign: 'center',
  },

})