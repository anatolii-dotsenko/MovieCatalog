package com.example.moviecatalog.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Movie(
    val id: String,
    val title: String,
    val year: Int?,
    val rating: Double?,
    val posterUrl: String?,
    val plot: String,
    val genres: List<String> = emptyList(),
    val originalTitle: String? = null,
    val runtimeMinutes: Int? = null,
    val voteCount: Int? = null,
    val director: String? = null,
    val cast: List<String> = emptyList(),
    val country: String? = null
)

@Immutable
data class MoviePage(
    val movies: List<Movie>,
    val nextPageToken: String?
)
