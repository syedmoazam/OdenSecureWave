#!/bin/bash

echo "📱 Setting up Device Owner for OdenSecureWave..."
echo ""
echo "⚠️  IMPORTANT: This requires the device to have NO Google accounts or other accounts configured!"
echo "If you have accounts configured, please remove them first in Settings > Accounts"
echo ""
echo "🔧 Running ADB command to set device owner..."

# Set device owner
adb shell dpm set-device-owner com.odensecurewave/.AppDeviceAdminReceiver

echo ""
echo "✅ Device owner setup complete!"
echo ""
echo "📝 If you got an error about accounts, please:"
echo "   1. Go to Settings > Accounts"
echo "   2. Remove all Google accounts and other accounts"
echo "   3. Run this script again"
echo ""
echo "🧪 You can now test camera control in the app!"
