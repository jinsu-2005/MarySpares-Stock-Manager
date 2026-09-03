package com.marytwowheelers.spares.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marytwowheelers.spares.data.repository.InventoryRepository

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    private val repository = InventoryRepository(context.applicationContext)
    private val accessRepository = com.marytwowheelers.spares.data.repository.AccessRepository(context.applicationContext)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository, accessRepository) as T
        }
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, accessRepository) as T
        }
        if (modelClass.isAssignableFrom(PartDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PartDetailsViewModel(repository, accessRepository) as T
        }
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, accessRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
