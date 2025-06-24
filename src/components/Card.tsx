import React from 'react';
import { View, Text, StyleSheet, ViewStyle, TextStyle, StyleProp } from 'react-native';
import { APP_PRIMARY_TEXT, Colors } from '@/themes/Colors';
import Fonts from '@/themes/Fonts';
import Metrics from '@/utils/Metrics';

interface CardProps {
  title?: string;
  description?: string;
  children?: React.ReactNode;
  cardStyle?: StyleProp<ViewStyle>;
  titleStyle?: StyleProp<TextStyle>;
  descriptionStyle?: StyleProp<TextStyle>;
}

const Card: React.FC<CardProps> = ({
  title,
  description,
  children,
  cardStyle,
  titleStyle,
  descriptionStyle,
}) => {
  return (
    <View style={[styles.card, cardStyle]}>
      {title ? <Text style={[styles.title, titleStyle]}>{title}</Text> : null}
      {description ? <Text style={[styles.description, descriptionStyle]}>{description}</Text> : null}
      {children}
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    padding: Metrics.verticalScale(16),
    borderRadius: Metrics.scale(12),
    shadowColor: Colors.DARK,
    shadowOpacity: 0.1,
    shadowRadius: Metrics.scale(6),
    elevation: 3,
    rowGap: Metrics.verticalScale(8),
    backgroundColor: Colors.WHITE,
  },
  title: Fonts.Bold(Fonts.Size.normal, APP_PRIMARY_TEXT),
  description: Fonts.Regular(Fonts.Size.small, APP_PRIMARY_TEXT)
});

export default Card;
