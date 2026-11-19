package com.example.moviecatalog.data.repository

import com.example.moviecatalog.data.remote.IMovieApiService
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.MoviePage

class MovieRepository(
    private val apiService: IMovieApiService
) {
    private val cache = LinkedHashMap<String, Movie>()

    suspend fun fetchMoviesPage(pageToken: String? = null): MoviePage {
        val page = apiService.fetchMoviesPage(pageToken)
        synchronized(cache) {
            page.movies.forEach { movie ->
                cache[movie.id] = movie
            }
        }
        return page
    }

    suspend fun getMovieById(movieId: String): Movie? {
        val cached = synchronized(cache) { cache[movieId] }
        val detailed = apiService.fetchMovieDetails(movieId)
        val merged = when {
            detailed != null && cached != null -> detailed.copy(
                posterUrl = detailed.posterUrl ?: cached.posterUrl,
                plot = detailed.plot.ifBlank { cached.plot },
                genres = detailed.genres.ifEmpty { cached.genres },
                rating = detailed.rating ?: cached.rating,
                year = detailed.year ?: cached.year
            )
            detailed != null -> detailed
            else -> cached
        }
        if (merged != null) {
            synchronized(cache) {
                cache[movieId] = merged
            }
        }
        return merged
    }

    suspend fun getMoviesByGenre(genre: String): List<Movie> {
        val hasMovies = synchronized(cache) { cache.isNotEmpty() }
        if (!hasMovies) {
            fetchMoviesPage(null)
        }
        return synchronized(cache) {
            cache.values.filter { movie ->
                movie.genres.any { it.equals(genre, ignoreCase = true) }
            }
        }
    }
}
