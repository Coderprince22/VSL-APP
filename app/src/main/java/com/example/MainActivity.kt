package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.GroupSavingsDatabase
import com.example.repository.GroupSavingsRepository
import com.example.ui.screens.VillageSavingsApp
import com.example.ui.viewmodel.SavingsViewModel
import com.example.ui.viewmodel.SavingsViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Room Database local system
    val database = GroupSavingsDatabase.getDatabase(this)
    val dao = database.savingsDao()
    val repository = GroupSavingsRepository(dao)

    // Instantiate State ViewModel utilizing injecting Factory
    val viewModel = ViewModelProvider(
      this,
      SavingsViewModelFactory(repository)
    )[SavingsViewModel::class.java]

    setContent {
      VillageSavingsApp(viewModel)
    }
  }
}
