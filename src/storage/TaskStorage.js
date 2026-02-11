import AsyncStorage from '@react-native-async-storage/async-storage';

const TASKS_KEY = '@medicine_tasks';

export const TaskStorage = {
  async saveTasks(tasks) {
    try {
      await AsyncStorage.setItem(TASKS_KEY, JSON.stringify(tasks));
      return true;
    } catch (error) {
      console.error('Error saving tasks:', error);
      return false;
    }
  },

  async getTasks() {
    try {
      const tasksJson = await AsyncStorage.getItem(TASKS_KEY);
      return tasksJson ? JSON.parse(tasksJson) : [];
    } catch (error) {
      console.error('Error loading tasks:', error);
      return [];
    }
  },

  async addTask(task) {
    try {
      const tasks = await this.getTasks();
      tasks.push(task);
      await this.saveTasks(tasks);
      return true;
    } catch (error) {
      console.error('Error adding task:', error);
      return false;
    }
  },

  async updateTask(taskId, updates) {
    try {
      const tasks = await this.getTasks();
      const index = tasks.findIndex(t => t.id === taskId);
      if (index !== -1) {
        tasks[index] = { ...tasks[index], ...updates };
        await this.saveTasks(tasks);
        return true;
      }
      return false;
    } catch (error) {
      console.error('Error updating task:', error);
      return false;
    }
  },

  async deleteTask(taskId) {
    try {
      const tasks = await this.getTasks();
      const filtered = tasks.filter(t => t.id !== taskId);
      await this.saveTasks(filtered);
      return true;
    } catch (error) {
      console.error('Error deleting task:', error);
      return false;
    }
  },

  async markTaskComplete(taskId) {
    return await this.updateTask(taskId, { 
      completed: true,
      completedAt: new Date().toISOString()
    });
  }
};