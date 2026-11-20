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

    private val _createdMovies = MutableStateFlow<List<Movie>>(emptyList())
    val createdMovies: StateFlow<List<Movie>> = _createdMovies.asStateFlow()

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
        if (cached?.isUserCreated == true || movieId.startsWith("user_")) {
            return cached
        }

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

    fun addCreatedMovie(movie: Movie) {
        synchronized(cache) {
            cache[movie.id] = movie
        }
        _createdMovies.update { current ->
            listOf(movie) + current.filterNot { it.id == movie.id }
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
