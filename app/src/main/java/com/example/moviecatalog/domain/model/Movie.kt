package com.example.moviecatalog.domain.model

data class Movie(
    val id: String,
    val title: String,
    val year: Int?,
    val rating: Double?,
    val posterUrl: String?,
    val plot: String,
    val genres: List<String> = emptyList()
)

data class MoviePage(
    val movies: List<Movie>,
    val nextPageToken: String?
)
