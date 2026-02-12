package com.example.pad.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val detail: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
