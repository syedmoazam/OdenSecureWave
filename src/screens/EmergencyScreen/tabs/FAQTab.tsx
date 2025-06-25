import Accordion from "@/components/Accordion";
import ScreenLayout from "@/layouts/ScreenLayout";
import Fonts from "@/themes/Fonts";
import Metrics from "@/utils/Metrics";
import { FlatList, StyleSheet, Text } from "react-native";

const faqContent = [
  {
    "description": "Your device is locked due to overdue payment. Once payment is made, your device will be automatically unlocked.",
    "title": "Why is my device locked?"
  },
  {
    "description": "You can make a payment using the \"Make Payment Now\" button on the Alert tab, or contact our support team for assistance.",
    "title": "How can I make a payment?"
  },
  {
    "description": "Your device will be automatically unlocked within 24 hours of payment confirmation.",
    "title": "What happens after I pay?"
  },
  {
    "description": "Please contact our support team to discuss payment arrangements and possible extensions.",
    "title": "Can I get an extension?"
  }
]
const FAQTab = () => (
	<ScreenLayout>
		<FlatList 
			data={faqContent}
			style={styles.listStyle}
			contentContainerStyle={styles.listContent}
			showsVerticalScrollIndicator={false}
			keyExtractor={(_item, index) => `faq-${index}`}
			renderItem={({ item }) => (
				<Accordion title={item.title}>
					<Text style={styles.faqTextStyle}>{item.description}</Text>
				</Accordion>
			)}
		/>
	</ScreenLayout>
)
const styles = StyleSheet.create({
	listStyle: { flex: 1 },
	listContent: { rowGap: Metrics.verticalScale(8) }, 
	faqTextStyle: Fonts.Regular(Fonts.Size.small)
})

export default FAQTab;