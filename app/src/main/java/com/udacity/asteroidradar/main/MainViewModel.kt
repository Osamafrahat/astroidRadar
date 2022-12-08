package com.udacity.asteroidradar.main

import android.app.Application
import androidx.lifecycle.*
import com.udacity.asteroidradar.database.getDatabase
import com.udacity.asteroidradar.repository.AsteroidsRepository
import com.udacity.asteroidradar.utils.FilterAsteroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(application: Application) : ViewModel() {

    private val database = getDatabase(application)
    private val asteroidsRepository = AsteroidsRepository(database)
    var asteroids = asteroidsRepository.asteroids
    val pictureOfDay = asteroidsRepository.pictureOfDay
    val loadingComplete = asteroidsRepository.loadingComplete

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                asteroidsRepository.refreshData()
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing data")
            }
        }
    }

    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }

    fun updateAsteroids(asteroidsFilter: FilterAsteroid) {
        asteroidsRepository.getAsteroidsWithFilter(asteroidsFilter)
        asteroids = asteroidsRepository.asteroids
    }
}