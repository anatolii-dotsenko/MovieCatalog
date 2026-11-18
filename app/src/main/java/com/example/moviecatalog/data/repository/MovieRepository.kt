package com.example.moviecatalog.data.repository

import com.example.moviecatalog.data.remote.IMovieApiService
import com.example.moviecatalog.domain.model.MoviePage

class MovieRepository(
    private val apiService: IMovieApiService
) {
    suspend fun fetchMoviesPage(pageToken: String? = null): MoviePage {
        return apiService.fetchMoviesPage(pageToken)
    }
}
