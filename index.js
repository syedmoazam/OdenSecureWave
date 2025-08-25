/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import { name as appName } from './app.json';

const firebaseHeadlessTask = async (data) => {
    // console.log('=== Firebase Headless Task Started ===');
    // console.log('Received data:', data);
    // console.log('Data type:', typeof data.data);
    // console.log('Raw data:', JSON.stringify(data, null, 2));
    
    // try {
    //     // Process your Firebase data here
    //     const firebaseData = data.data;
        
    //     // Parse the JSON data
    //     if (typeof firebaseData === 'string') {
    //         try {
    //             const parsedData = JSON.parse(firebaseData);
    //             console.log('Parsed Firebase data:', parsedData);
                
    //             // Now you can access the data properly
    //             console.log('Message:', parsedData.message);
    //             console.log('Status:', parsedData.status);
    //             console.log('Timestamp:', parsedData.timestamp);
                
    //             // Add your business logic here
    //             // Example: Handle different types of updates
    //             if (parsedData.status === 'active') {
    //                 console.log('Handling active status update');
    //                 // Your logic here
    //             }
                
    //         } catch (parseError) {
    //             console.error('Failed to parse JSON:', parseError);
    //             console.log('Raw data that failed to parse:', firebaseData);
    //         }
    //     } else {
    //         console.log('Data is not a string:', firebaseData);
    //     }
        
    // } catch (error) {
    //     console.error('Error in Firebase headless task:', error);
    // }
    
    // console.log('=== Firebase Headless Task Completed ===');
};

AppRegistry.registerComponent(appName, () => App);
AppRegistry.registerHeadlessTask('FirebaseBackgroundTask', () => firebaseHeadlessTask);
