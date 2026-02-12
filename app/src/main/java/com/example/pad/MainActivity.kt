package com.example.pad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pad.data.AppDatabase
import com.example.pad.data.TaskRepository
import com.example.pad.ui.TaskApp
import com.example.pad.ui.TaskViewModel
import com.example.pad.ui.TaskViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TaskRepository(AppDatabase.getInstance(this).taskDao())

        setContent {
            val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModelFactory(repository))
            TaskApp(viewModel = taskViewModel)
        }
    }
}
