package com.example.pad.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    fun observeTasks(): Flow<List<TaskEntity>> = dao.observeAll()

    suspend fun addTask(title: String, detail: String) {
        dao.insert(TaskEntity(title = title.trim(), detail = detail.trim()))
    }

    suspend fun toggleTask(task: TaskEntity) {
        dao.update(task.copy(completed = !task.completed))
    }

    suspend fun deleteTask(task: TaskEntity) {
        dao.delete(task)
    }
}
