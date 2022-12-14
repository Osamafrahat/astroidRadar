package com.udacity.asteroidradar.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.udacity.asteroidradar.Asteroid
import com.udacity.asteroidradar.BuildConfig
import com.udacity.asteroidradar.PictureOfDay
import com.udacity.asteroidradar.api.Network
import com.udacity.asteroidradar.api.asDatabaseModel
import com.udacity.asteroidradar.api.parseAsteroidsJsonResult
import com.udacity.asteroidradar.database.*
import com.udacity.asteroidradar.utils.Constants
import com.udacity.asteroidradar.utils.DateUtils
import com.udacity.asteroidradar.utils.FilterAsteroid
import org.json.JSONObject
import timber.log.Timber


class AsteroidsRepository(private val database: AsteroidsDatabase) {

    lateinit var asteroids: LiveData<List<Asteroid>>

    private var _loadingComplete = MutableLiveData<Boolean>(false)
    val loadingComplete: LiveData<Boolean>
        get() = _loadingComplete

    init {
        _loadingComplete.postValue(false)
        getAsteroidsWithFilter()
        _loadingComplete.postValue(true)
    }

    private fun updateAsteroidsList(liveData: LiveData<List<DatabaseAsteroid>>) =
        Transformations.map(liveData) {
            it.asDomainModel()
        }

    val pictureOfDay: LiveData<PictureOfDay> = Transformations.map(
        database.pictureOfDayDao.getPictureOfDay(DateUtils.getTodayAsString())) {
        it?.asDomainModel()
    }

    suspend fun refreshData() {
        _loadingComplete.postValue(false)
        try {
            refreshAsteroids()
            refreshPictureOfDay()
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing data")
        } finally {
            _loadingComplete.postValue(true)
        }
    }

    private suspend fun refreshAsteroids() {
        val response = Network.service.getAsteroids(
            DateUtils.getTodayAsString(),
            DateUtils.getEndDateAsString(Constants.DEFAULT_END_DATE_DAYS),
            Constants.API_KEY)
        val asteroidsList = parseAsteroidsJsonResult(JSONObject(response.body()!!)).toList()
        database.asteroidDao.insertAll(*asteroidsList.asDatabaseModel())
        database.asteroidDao.deleteAsteroidsBefore(DateUtils.getTodayAsString())
    }

    private suspend fun refreshPictureOfDay() {
        val pictureOfDay = Network.service.getPictureOfDay(Constants.API_KEY)

        database.pictureOfDayDao.insert(pictureOfDay.asDatabaseModel())
        database.pictureOfDayDao.deletePicturesBefore(DateUtils.getTodayAsString())
    }

    fun getAsteroidsWithFilter(filter: FilterAsteroid = FilterAsteroid.WEEK) {
        val liveData = when(filter) {
            FilterAsteroid.WEEK -> database.asteroidDao.getAsteroidsAfterDate(DateUtils.getTodayAsString())
            FilterAsteroid.TODAY -> database.asteroidDao.getAsteroidsWithDate(DateUtils.getTodayAsString())
            FilterAsteroid.ALL -> database.asteroidDao.getAllAsteroids()
        }
        asteroids = updateAsteroidsList(liveData)
    }
}