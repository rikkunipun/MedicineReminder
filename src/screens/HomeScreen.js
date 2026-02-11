import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Alert,
  NativeModules,
  NativeEventEmitter,
} from 'react-native';
import { TaskStorage } from '../storage/TaskStorage';

const { AlarmModule } = NativeModules;
const alarmEmitter = new NativeEventEmitter(AlarmModule);

export default function HomeScreen({ navigation }) {
  const [tasks, setTasks] = useState([]);

  useEffect(() => {
    loadTasks();
    
    const completedSubscription = alarmEmitter.addListener('onTaskCompleted', (event) => {
      console.log('Task completed:', event.taskId);
      handleTaskCompleted(event.taskId);
    });

    const snoozedSubscription = alarmEmitter.addListener('onTaskSnoozed', (event) => {
      console.log('Task snoozed:', event.taskId, event.newTime);
    });

    return () => {
      completedSubscription.remove();
      snoozedSubscription.remove();
    };
  }, []);

  const loadTasks = async () => {
    const loadedTasks = await TaskStorage.getTasks();
    const pendingTasks = loadedTasks.filter(t => !t.completed);
    setTasks(pendingTasks);
  };

  const handleTaskCompleted = async (taskId) => {
  // Get the task details
  const allTasks = await TaskStorage.getTasks();
  const task = allTasks.find(t => t.id === taskId);
  
  if (!task) return;
  
  if (task.isRecurring) {
    // For recurring tasks, schedule for tomorrow
    const originalTime = new Date(task.scheduledTime);
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(originalTime.getHours());
    tomorrow.setMinutes(originalTime.getMinutes());
    tomorrow.setSeconds(0);
    
    // Update task with new time
    await TaskStorage.updateTask(taskId, {
      scheduledTime: tomorrow.toISOString(),
      completed: false
    });
    
    // Reschedule alarm for tomorrow
    await AlarmModule.scheduleAlarm(
      taskId,
      task.name,
      tomorrow.getTime()
    );
    
    console.log('Recurring task rescheduled for tomorrow');
  } else {
    // For one-time tasks, mark as complete and cancel
    await TaskStorage.markTaskComplete(taskId);
    await AlarmModule.cancelAlarm(taskId);
  }
  
  loadTasks();
};

  const deleteTask = async (task) => {
    Alert.alert(
      'Delete Task',
      `Are you sure you want to delete "${task.name}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await AlarmModule.cancelAlarm(task.id);
            await TaskStorage.deleteTask(task.id);
            loadTasks();
          },
        },
      ]
    );
  };

  const formatTime = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', { 
      hour: 'numeric', 
      minute: '2-digit',
      hour12: true 
    });
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    if (date.toDateString() === today.toDateString()) {
      return 'Today';
    } else if (date.toDateString() === tomorrow.toDateString()) {
      return 'Tomorrow';
    } else {
      return date.toLocaleDateString('en-US', { 
        month: 'short', 
        day: 'numeric' 
      });
    }
  };

  const renderTask = ({ item }) => (
  <View style={styles.taskCard}>
    <View style={styles.taskInfo}>
      <View style={styles.taskHeader}>
        <Text style={styles.taskName}>{item.name}</Text>
        {item.isRecurring && (
          <View style={styles.dailyBadge}>
            <Text style={styles.dailyBadgeText}>DAILY</Text>
          </View>
        )}
      </View>
      <Text style={styles.taskTime}>
        {item.isRecurring ? 'Every day at ' : formatDate(item.scheduledTime) + ' at '}
        {formatTime(item.scheduledTime)}
      </Text>
    </View>
    <TouchableOpacity
      style={styles.deleteButton}
      onPress={() => deleteTask(item)}
    >
      <Text style={styles.deleteButtonText}>Delete</Text>
    </TouchableOpacity>
  </View>
);

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Daily Reminders</Text>
        <Text style={styles.headerSubtitle}>
          {tasks.length} {tasks.length === 1 ? 'reminder' : 'reminders'}
        </Text>
      </View>

      <FlatList
        data={tasks}
        renderItem={renderTask}
        keyExtractor={item => item.id}
        contentContainerStyle={styles.listContainer}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>No reminders yet</Text>
            <Text style={styles.emptySubtext}>
              Tap + to create your first reminder
            </Text>
          </View>
        }
      />

      <TouchableOpacity
        style={styles.addButton}
        onPress={() => navigation.navigate('AddTask', { onTaskAdded: loadTasks })}
      >
        <Text style={styles.addButtonText}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
  },
  header: {
    backgroundColor: '#2196F3',
    padding: 20,
    paddingTop: 50,
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  headerSubtitle: {
    fontSize: 16,
    color: '#E3F2FD',
    marginTop: 5,
  },
  listContainer: {
    padding: 16,
  },
  taskCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  taskInfo: {
    flex: 1,
  },
  taskName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#212121',
    marginBottom: 4,
  },
  taskTime: {
    fontSize: 16,
    color: '#757575',
  },
  deleteButton: {
    backgroundColor: '#F44336',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
  },
  deleteButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
  emptyContainer: {
    alignItems: 'center',
    marginTop: 100,
  },
  emptyText: {
    fontSize: 22,
    color: '#9E9E9E',
    fontWeight: 'bold',
  },
  emptySubtext: {
    fontSize: 16,
    color: '#BDBDBD',
    marginTop: 8,
  },
  addButton: {
    position: 'absolute',
    right: 20,
    bottom: 30,
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#4CAF50',
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 5,
    elevation: 8,
  },
  addButtonText: {
    fontSize: 40,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  taskHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  dailyBadge: {
    backgroundColor: '#4CAF50',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    marginLeft: 8,
  },
  dailyBadgeText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: 'bold',
  },
});