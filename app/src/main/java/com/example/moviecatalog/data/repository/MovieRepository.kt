package com.example.moviecatalog.data.repository

import com.example.moviecatalog.data.remote.IMovieApiService
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.MoviePage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MovieRepository(
    private val apiService: IMovieApiService
) {
    private val cache = LinkedHashMap<String, Movie>()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _favoriteMovies = MutableStateFlow<List<Movie>>(emptyList())
    val favoriteMovies: StateFlow<List<Movie>> = _favoriteMovies.asStateFlow()

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
            if (movieId in _favoriteIds.value) {
                _favoriteMovies.update { list ->
                    list.map { if (it.id == movieId) merged else it }
                }
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

    fun addToFavorites(movie: Movie) {
        if (movie.id in _favoriteIds.value) return
        _favoriteIds.update { it + movie.id }
        _favoriteMovies.update { current ->
            if (current.any { it.id == movie.id }) current else current + movie
        }
    }

    fun removeFromFavorites(movieId: String) {
        if (movieId !in _favoriteIds.value) return
        _favoriteIds.update { it - movieId }
        _favoriteMovies.update { current -> current.filterNot { it.id == movieId } }
    }

    fun toggleFavorite(movie: Movie) {
        if (movie.id in _favoriteIds.value) {
            removeFromFavorites(movie.id)
        } else {
            addToFavorites(movie)
        }
    }
}
