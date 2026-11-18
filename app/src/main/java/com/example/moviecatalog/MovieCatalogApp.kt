package com.example.moviecatalog

import android.app.Application
import com.example.moviecatalog.data.remote.IMovieApiService
import com.example.moviecatalog.data.remote.MovieApiService
import com.example.moviecatalog.data.repository.MovieRepository

/**
 * Application DI container holding singleton services for MovieCatalog.
 */
class MovieCatalogApp : Application() {
    val movieApiService: IMovieApiService by lazy { MovieApiService() }
    val movieRepository: MovieRepository by lazy { MovieRepository(movieApiService) }
}
