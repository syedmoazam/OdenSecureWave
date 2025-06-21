import {FlatList, StyleSheet, Text, View} from 'react-native';
import SvgIcon from "@/components/SvgIcon.tsx";
import Icons from "@/constants/icons.ts";
import Metrics from "@/utils/Metrics.ts";
import {Colors} from "@/themes/Colors.ts";
import Fonts from "@/themes/Fonts.ts";
import AppButton from "@/components/AppButton.tsx";
import BottomSheet, {BottomSheetRef} from "@/components/BottomSheet.tsx";
import {useRef, useState} from "react";

interface ICompany {
  id: string;
  name: string;
  branch: string;
}
const companyData = [
  { id: '1', name: 'Company A', branch: 'Branch 1' },
  { id: '2', name: 'Company B', branch: 'Branch 2' },
  { id: '3', name: 'Company C', branch: 'Branch 3' },
  { id: '4', name: 'Company D', branch: 'Branch 4' },
  { id: '5', name: 'Company E', branch: 'Branch 5' },
  { id: '6', name: 'Company F', branch: 'Branch 6' },
  { id: '7', name: 'Company G', branch: 'Branch 7' },
  { id: '8', name: 'Company H', branch: 'Branch 8' },
  { id: '9', name: 'Company I', branch: 'Branch 9' },
  { id: '10', name: 'Company J', branch: 'Branch 10' },
] as ICompany[];

export function HomeScreen() {
  const [company, setCompany] = useState<ICompany | null>(null);
  const bottomSheetRef = useRef<BottomSheetRef>(null);

  const onPressBranchCard = () => {
    if (bottomSheetRef.current) {
      bottomSheetRef.current.open();
    }
  }

  const onSelectCompany = (companyToSelect: ICompany) => {
    setCompany(companyToSelect);
    if (bottomSheetRef.current) {
      bottomSheetRef.current.close();
    }
  }

  const onPressDisableCamera = () => {
    // Logic to disable camera
  }

  return (
    <View style={styles.container}>
      {company ? (
        <>
          {/* Device Setup */}
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
          {/* Device Setup Initiator */}
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
              Complete the device setup to enable security features and management controls.
            </Text>
            <AppButton style={styles.setupBtn}>
              <Text style={styles.whiteMediumText}>Start Setup</Text>
            </AppButton>
          </View>
        </>
      ) : (
        <>
          {/* Company Selection */}
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
          {/* Branch Card */}
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
      )}
      <AppButton
        style={styles.setupBtn}
        onPress={onPressDisableCamera}
      >
        <Text style={styles.whiteMediumText}>Disable Camera</Text>
      </AppButton>
      <BottomSheet ref={bottomSheetRef}>
        <CompanySelector onSelectCompany={onSelectCompany}/>
      </BottomSheet>
    </View>
  );
}

interface ICompanySelectorProps {
  onSelectCompany: (company: ICompany) => void,
}
const CompanySelector = ({ onSelectCompany }: ICompanySelectorProps) => {
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
      <FlatList
        style={styles.listStyle}
        contentContainerStyle={styles.companyListContentContainer}
        data={companyData}
        keyExtractor={(_item, index) => index.toString()}
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

  // Text
  blueBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.BLUE),
  blueMediumLabel: Fonts.Medium(Fonts.Size.normal, Colors.BLUE),
  greenMediumLabel: Fonts.Medium(Fonts.Size.normal, Colors.GREEN),
  companySelectorLabel: Fonts.Bold(Fonts.Size.xLarge, Colors.BLUE),
  companyName: Fonts.Bold(Fonts.Size.normal, Colors.DARK),
  orangeBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.ORANGE),
  purpleBoldLabel: Fonts.Bold(Fonts.Size.large, Colors.PURPLE),
  blueText: Fonts.Regular(Fonts.Size.xSmall, Colors.BLUE),
  companySelectorText: Fonts.Regular(Fonts.Size.xSmall, Colors.BLUE),
  branchName: Fonts.Regular(Fonts.Size.xxSmall, Colors.CHARCOAL_GREY),
  orangeText: Fonts.Regular(Fonts.Size.xSmall, Colors.ORANGE),
  purpleText: Fonts.Regular(Fonts.Size.xSmall, Colors.PURPLE),
  whiteMediumText: Fonts.Medium(Fonts.Size.normal, Colors.WHITE),

  // Backgrounds
  lightBlueBg: { backgroundColor: Colors.LIGHT_BLUE },
  lightOrangeBg: { backgroundColor: Colors.LIGHT_ORANGE },
  lightPurpleBg: { backgroundColor: Colors.LIGHT_PURPLE },
  lightGreenBg: { backgroundColor: Colors.LIGHT_GREEN },
})