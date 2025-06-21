// Simple test script to verify bridge functionality
// Run this from React Native debugger console or add to a test component

const testDeviceAdminBridge = async () => {
  try {
    console.log('Testing Device Admin Bridge...');
    
    // Test basic bridge connection
    const testResult = await DeviceAdminModule.testBridge();
    console.log('Bridge test result:', testResult);
    
    // Test admin status check
    const isEnabled = await DeviceAdminModule.isDeviceAdminEnabled();
    console.log('Device admin enabled:', isEnabled);
    
    // Test camera status
    const cameraDisabled = await DeviceAdminModule.isCameraDisabled();
    console.log('Camera disabled:', cameraDisabled);
    
    // Test boot status
    const bootStatus = await DeviceAdminModule.checkBootCompletedStatus();
    console.log('Boot completed status:', bootStatus);
    
    console.log('All bridge tests completed successfully!');
    
    return {
      bridgeTest: testResult,
      adminEnabled: isEnabled,
      cameraDisabled: cameraDisabled,
      bootStatus: bootStatus
    };
  } catch (error) {
    console.error('Bridge test failed:', error);
    throw error;
  }
};

// Export for use in components
export default testDeviceAdminBridge;
