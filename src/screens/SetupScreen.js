import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Alert,
  NativeModules,
  Linking,
  AppState,
} from 'react-native';

const { AlarmModule } = NativeModules;

export default function SetupScreen({ navigation }) {
  const [permissions, setPermissions] = useState({
    exactAlarm: false,
    batteryOptimization: false,
    displayOverApps: false,
  });

  useEffect(() => {
    checkPermissions();
    
    // Check permissions when app comes to foreground
    const subscription = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'active') {
        checkPermissions();
      }
    });
    
    return () => {
      subscription?.remove();
    };
  }, []);

  const checkPermissions = async () => {
    try {
      const result = await AlarmModule.checkPermissions();
      const displayOverApps = await AlarmModule.checkDisplayOverApps();
      setPermissions({
        ...result,
        displayOverApps: displayOverApps,
      });
    } catch (error) {
      console.error('Error checking permissions:', error);
    }
  };

  const requestExactAlarm = async () => {
    try {
      await AlarmModule.requestExactAlarmPermission();
      setTimeout(checkPermissions, 1000);
    } catch (error) {
      console.error('Error requesting exact alarm permission:', error);
    }
  };

  const requestBatteryOptimization = async () => {
    try {
      await AlarmModule.requestBatteryOptimization();
      setTimeout(checkPermissions, 1000);
    } catch (error) {
      console.error('Error requesting battery optimization:', error);
    }
  };

  const requestDisplayOverApps = async () => {
    try {
      await AlarmModule.requestDisplayOverApps();
      setTimeout(checkPermissions, 1000);
    } catch (error) {
      console.error('Error requesting display permission:', error);
    }
  };

  const showSamsungInstructions = () => {
    Alert.alert(
      'Samsung Device Setup',
      'For reliable alarms on Samsung devices:\n\n' +
      '1. Go to Settings → Apps → Daily Reminder\n' +
      '2. Battery → Optimize battery usage → All apps\n' +
      '3. Find Daily Reminder → Don\'t optimize\n\n' +
      '4. Go to Settings → Device care → Battery\n' +
      '5. App power management → Never sleeping apps\n' +
      '6. Add Daily Reminder to the list',
      [
        { text: 'Open Settings', onPress: () => Linking.openSettings() },
        { text: 'OK' },
      ]
    );
  };

  const allPermissionsGranted = permissions.exactAlarm && 
                                 permissions.batteryOptimization && 
                                 permissions.displayOverApps;

  // Auto-navigate if permissions already granted
  useEffect(() => {
    if (allPermissionsGranted) {
      // Small delay to prevent immediate navigation
      const timer = setTimeout(() => {
        navigation.replace('Home');
      }, 500);
      return () => clearTimeout(timer);
    }
  }, [allPermissionsGranted, navigation]);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Setup Required</Text>
      <Text style={styles.subtitle}>
        For reliable alarms, we need some permissions
      </Text>

      <View style={styles.permissionsList}>
        <PermissionItem
          title="Exact Alarm Permission"
          description="Allows the app to trigger alarms at exact times"
          granted={permissions.exactAlarm}
          onPress={requestExactAlarm}
        />

        <PermissionItem
          title="Battery Optimization"
          description="Prevents Android from killing the alarm"
          granted={permissions.batteryOptimization}
          onPress={requestBatteryOptimization}
        />

        <PermissionItem
          title="Display Over Other Apps"
          description="Shows full-screen alarm even when locked"
          granted={permissions.displayOverApps}
          onPress={requestDisplayOverApps}
        />
      </View>

      <TouchableOpacity
        style={styles.samsungButton}
        onPress={showSamsungInstructions}
      >
        <Text style={styles.samsungButtonText}>
          Samsung Device? Tap Here
        </Text>
      </TouchableOpacity>

      {allPermissionsGranted && (
        <TouchableOpacity
          style={styles.continueButton}
          onPress={() => navigation.replace('Home')}
        >
          <Text style={styles.continueButtonText}>Continue to App</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

function PermissionItem({ title, description, granted, onPress }) {
  return (
    <View style={styles.permissionItem}>
      <View style={styles.permissionInfo}>
        <Text style={styles.permissionTitle}>{title}</Text>
        <Text style={styles.permissionDescription}>{description}</Text>
      </View>
      {granted ? (
        <View style={styles.grantedBadge}>
          <Text style={styles.grantedText}>✓</Text>
        </View>
      ) : (
        <TouchableOpacity style={styles.grantButton} onPress={onPress}>
          <Text style={styles.grantButtonText}>Grant</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#F5F5F5',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#212121',
    marginTop: 60,
    marginBottom: 10,
  },
  subtitle: {
    fontSize: 18,
    color: '#757575',
    marginBottom: 40,
  },
  permissionsList: {
    marginBottom: 30,
  },
  permissionItem: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  permissionInfo: {
    flex: 1,
  },
  permissionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#212121',
    marginBottom: 4,
  },
  permissionDescription: {
    fontSize: 14,
    color: '#757575',
  },
  grantButton: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
  },
  grantButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  grantedBadge: {
    backgroundColor: '#4CAF50',
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  grantedText: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: 'bold',
  },
  samsungButton: {
    backgroundColor: '#FF9800',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 20,
  },
  samsungButtonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 'bold',
  },
  continueButton: {
    backgroundColor: '#4CAF50',
    padding: 20,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 20,
  },
  continueButtonText: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: 'bold',
  },
});