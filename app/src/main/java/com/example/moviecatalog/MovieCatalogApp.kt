package com.example.moviecatalog

import android.app.Application
import com.example.moviecatalog.core.navigation.INavigationService
import com.example.moviecatalog.core.navigation.NavigationService
import com.example.moviecatalog.data.auth.AuthService
import com.example.moviecatalog.data.auth.IAuthService
import com.example.moviecatalog.data.local.ILocalizationService
import com.example.moviecatalog.data.local.IPreferencesStorage
import com.example.moviecatalog.data.local.ISecureStorage
import com.example.moviecatalog.data.local.IStorageService
import com.example.moviecatalog.data.local.LocalizationService
import com.example.moviecatalog.data.local.PreferencesStorage
import com.example.moviecatalog.data.local.SecureStorage
import com.example.moviecatalog.data.local.StorageService
import com.example.moviecatalog.data.remote.IMovieApiService
import com.example.moviecatalog.data.remote.MovieApiService
import com.example.moviecatalog.data.repository.MovieRepository

/**
 * Application DI container holding singleton services for MovieCatalog.
 */
class MovieCatalogApp : Application() {
    val preferencesStorage: IPreferencesStorage by lazy { PreferencesStorage(applicationContext) }
    val secureStorage: ISecureStorage by lazy { SecureStorage(applicationContext) }
    val authService: IAuthService by lazy {
        AuthService(applicationContext, preferencesStorage, secureStorage)
    }
    val localizationService: ILocalizationService by lazy {
        LocalizationService(applicationContext, preferencesStorage, authService)
    }
    val movieApiService: IMovieApiService by lazy { MovieApiService() }
    val storageService: IStorageService by lazy { StorageService(applicationContext) }
    val movieRepository: MovieRepository by lazy {
        MovieRepository(movieApiService, storageService, authService)
    }
    val navigationService: INavigationService by lazy { NavigationService() }
}
