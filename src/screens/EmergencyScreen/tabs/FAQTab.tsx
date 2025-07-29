import Accordion from "@/components/Accordion";
import ScreenLayout from "@/layouts/ScreenLayout";
import Fonts from "@/themes/Fonts";
import Metrics from "@/utils/Metrics";
import { useFAQData } from "@/hooks/useFAQData";
import { FlatList, StyleSheet, Text, View, ActivityIndicator } from "react-native";
import { Colors } from "@/themes/Colors";

const FAQTab = () => {
  const { data, loading, error, refreshData } = useFAQData();

  if (loading) {
    return (
      <ScreenLayout>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={Colors.PRIMARY_BLUE} />
          <Text style={styles.loadingText}>Loading FAQ...</Text>
        </View>
      </ScreenLayout>
    );
  }

  return (
    <ScreenLayout>
      {error && (
        <View style={styles.errorContainer}>
          <Text style={styles.errorText}>{error}</Text>
          <Text style={styles.retryText} onPress={refreshData}>
            Tap to retry
          </Text>
        </View>
      )}

      <FlatList 
        data={data}
        style={styles.listStyle}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <Accordion title={item.title}>
            <Text style={styles.faqTextStyle}>{item.description}</Text>
          </Accordion>
        )}
        refreshing={loading}
        onRefresh={refreshData}
      />
    </ScreenLayout>
  );
};

const styles = StyleSheet.create({
  listStyle: { flex: 1 },
  listContent: { rowGap: Metrics.verticalScale(8) }, 
  faqTextStyle: Fonts.Regular(Fonts.Size.small),
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    ...Fonts.Regular(Fonts.Size.medium),
    marginTop: Metrics.verticalScale(16),
    color: Colors.GRAY,
  },
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
});

export default FAQTab;