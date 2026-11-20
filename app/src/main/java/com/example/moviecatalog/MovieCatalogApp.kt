package com.example.moviecatalog

import android.app.Application
import com.example.moviecatalog.core.navigation.INavigationService
import com.example.moviecatalog.core.navigation.NavigationService
import com.example.moviecatalog.data.local.IStorageService
import com.example.moviecatalog.data.local.StorageService
import com.example.moviecatalog.data.remote.IMovieApiService
import com.example.moviecatalog.data.remote.MovieApiService
import com.example.moviecatalog.data.repository.MovieRepository

/**
 * Application DI container holding singleton services for MovieCatalog.
 */
class MovieCatalogApp : Application() {
    val movieApiService: IMovieApiService by lazy { MovieApiService() }
    val storageService: IStorageService by lazy { StorageService(applicationContext) }
    val movieRepository: MovieRepository by lazy { MovieRepository(movieApiService, storageService) }
    val navigationService: INavigationService by lazy { NavigationService() }
}
