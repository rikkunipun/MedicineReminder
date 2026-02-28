import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  NativeModules,
  Platform,
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { TaskStorage } from '../storage/TaskStorage';
import { generateUUID } from '../utils/uuid';

const { AlarmModule } = NativeModules;

export default function AddTaskScreen({ navigation, route }) {
  const [taskName, setTaskName] = useState('');
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [selectedTime, setSelectedTime] = useState(new Date());
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [isRecurring, setIsRecurring] = useState(false);

  const handleSaveTask = async () => {
    if (!taskName.trim()) {
      Alert.alert('Error', 'Please enter a task name');
      return;
    }

    const scheduledDateTime = new Date(
      selectedDate.getFullYear(),
      selectedDate.getMonth(),
      selectedDate.getDate(),
      selectedTime.getHours(),
      selectedTime.getMinutes()
    );

    if (!isRecurring && scheduledDateTime <= new Date()) {
      Alert.alert('Error', 'Please select a future time');
      return;
    }

    if (isRecurring && scheduledDateTime <= new Date()) {
      scheduledDateTime.setDate(scheduledDateTime.getDate() + 1);
    }

    const task = {
      id: generateUUID(),
      name: taskName.trim(),
      scheduledTime: scheduledDateTime.toISOString(),
      createdAt: new Date().toISOString(),
      completed: false,
      isRecurring: isRecurring,
    };

    const saved = await TaskStorage.addTask(task);
    
    if (!saved) {
      Alert.alert('Error', 'Failed to save task');
      return;
    }

    try {
      const success = await AlarmModule.scheduleAlarm(
        task.id,
        task.name,
        scheduledDateTime.getTime(),
        isRecurring
      );

      if (success) {
        Alert.alert(
          'Success', 
          isRecurring 
            ? 'Daily reminder created! Will repeat every day at this time.' 
            : 'Reminder created!',
          [
            {
              text: 'OK',
              onPress: () => navigation.goBack(),
            },
          ]
        );
      } else {
        Alert.alert('Error', 'Failed to schedule alarm. Please check permissions.');
      }
    } catch (error) {
      console.error('Error scheduling alarm:', error);
      Alert.alert('Error', 'Failed to schedule alarm');
    }
  };

  const formatDate = (date) => {
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const formatTime = (time) => {
    return time.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <Text style={styles.cancelButton}>Cancel</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>New Reminder</Text>
        <TouchableOpacity onPress={handleSaveTask}>
          <Text style={styles.saveButton}>Save</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.form}>
        <Text style={styles.label}>Task Name</Text>
        <TextInput
          style={styles.input}
          placeholder="e.g., Morning workout, Call mom, Water plants"
          value={taskName}
          onChangeText={setTaskName}
          autoFocus
          fontSize={18}
        />

        <Text style={styles.label}>Date</Text>
        <TouchableOpacity
          style={styles.pickerButton}
          onPress={() => setShowDatePicker(true)}
        >
          <Text style={styles.pickerText}>{formatDate(selectedDate)}</Text>
        </TouchableOpacity>

        <Text style={styles.label}>Time</Text>
        <TouchableOpacity
          style={styles.pickerButton}
          onPress={() => setShowTimePicker(true)}
        >
          <Text style={styles.pickerText}>{formatTime(selectedTime)}</Text>
        </TouchableOpacity>

        <Text style={styles.label}>Repeat</Text>
        <TouchableOpacity
          style={styles.repeatButton}
          onPress={() => setIsRecurring(!isRecurring)}
        >
          <View style={styles.repeatOption}>
            <Text style={styles.repeatText}>
              {isRecurring ? '✓ Every Day' : 'Once Only'}
            </Text>
          </View>
        </TouchableOpacity>

        {showDatePicker && (
          <DateTimePicker
            value={selectedDate}
            mode="date"
            display="default"
            minimumDate={new Date()}
            onChange={(event, date) => {
              setShowDatePicker(Platform.OS === 'ios');
              if (date) setSelectedDate(date);
            }}
          />
        )}

        {showTimePicker && (
          <DateTimePicker
            value={selectedTime}
            mode="time"
            display="default"
            onChange={(event, time) => {
              setShowTimePicker(Platform.OS === 'ios');
              if (time) setSelectedTime(time);
            }}
          />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    paddingTop: 50,
    backgroundColor: '#2196F3',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  cancelButton: {
    fontSize: 18,
    color: '#FFFFFF',
  },
  saveButton: {
    fontSize: 18,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  form: {
    padding: 20,
  },
  label: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#424242',
    marginBottom: 8,
    marginTop: 20,
  },
  input: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    fontSize: 18,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  pickerButton: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  pickerText: {
    fontSize: 18,
    color: '#212121',
  },
  repeatButton: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E0E0E0',
  },
  repeatOption: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  repeatText: {
    fontSize: 18,
    color: '#212121',
    fontWeight: 'bold',
  },
});