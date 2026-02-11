# Daily Reminder

A React Native app that schedules reliable daily reminders with full-screen alarm notifications on Android. Built with native Kotlin alarm modules for precise scheduling that survives device reboots and battery optimization.

## Features

- **Create reminders** with a name, date, and time
- **Recurring daily reminders** that automatically reschedule for the next day
- **Full-screen alarm** that displays even when the phone is locked
- **Survives reboots** - alarms are rescheduled on device restart
- **Battery optimization bypass** to prevent Android from killing alarms
- **Samsung device support** with guided setup instructions

## Screenshots

<!-- Add screenshots here -->

## Tech Stack

- **React Native** 0.73 with TypeScript
- **React Navigation** for screen routing
- **AsyncStorage** for local persistence
- **Native Kotlin modules** for Android alarm scheduling (`AlarmManager`, `AlarmReceiver`, `AlarmService`)

## Project Structure

```
src/
  screens/
    SetupScreen.js      # Permission setup on first launch
    HomeScreen.js       # Main screen with reminder list
    AddTaskScreen.js    # Create new reminders
  storage/
    TaskStorage.js      # AsyncStorage wrapper for tasks
  utils/
    uuid.js             # UUID generation

android/.../
  AlarmModule.kt        # React Native bridge for alarm APIs
  AlarmScheduler.kt     # AlarmManager scheduling logic
  AlarmReceiver.kt      # BroadcastReceiver for alarm triggers
  AlarmService.kt       # Foreground service for alarm playback
  AlarmActivity.kt      # Full-screen alarm UI
  BootReceiver.kt       # Re-schedules alarms after reboot
```

## Getting Started

### Prerequisites

- Node.js >= 18
- React Native CLI
- Android Studio with an emulator or physical device

### Installation

```bash
npm install
```

### Running

```bash
# Start Metro bundler
npm start

# Run on Android
npm run android
```

### Android Permissions

On first launch, the app will guide you through granting:

1. **Exact Alarm** - required for precise scheduling
2. **Battery Optimization exemption** - prevents the OS from killing the alarm
3. **Display Over Other Apps** - allows the full-screen alarm on the lock screen

## License

MIT
