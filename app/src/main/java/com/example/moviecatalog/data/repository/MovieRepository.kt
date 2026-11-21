package com.example.moviecatalog.data.repository

import com.example.moviecatalog.data.auth.IAuthService
import com.example.moviecatalog.data.local.IStorageService
import com.example.moviecatalog.data.remote.IMovieApiService
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.MovieCreateDto
import com.example.moviecatalog.domain.model.MoviePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Centralized state manager coordinating remote API data, per-user AppData storage,
 * and authentication gating (Android equivalent of MovieService).
 */
class MovieRepository(
    private val apiService: IMovieApiService,
    private val storageService: IStorageService,
    private val authService: IAuthService
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadMutex = Mutex()
    private var loadedForUserId: String? = null
    private var hasSyncedInitialSession = false

    private val cache = LinkedHashMap<String, Movie>()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _favoriteMovies = MutableStateFlow<List<Movie>>(emptyList())
    val favoriteMovies: StateFlow<List<Movie>> = _favoriteMovies.asStateFlow()

    private val _createdMovies = MutableStateFlow<List<Movie>>(emptyList())
    val createdMovies: StateFlow<List<Movie>> = _createdMovies.asStateFlow()

    init {
        repositoryScope.launch {
            authService.currentUser
                .map { it?.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    syncUserLocalData(userId)
                }
        }
    }

    suspend fun ensureLocalDataLoaded() {
        authService.restoreSessionWithRefreshToken()
        syncUserLocalData(authService.currentUser.value?.userId)
    }

    private suspend fun syncUserLocalData(userId: String?) {
        loadMutex.withLock {
            if (hasSyncedInitialSession && loadedForUserId == userId) return
            hasSyncedInitialSession = true
            loadedForUserId = userId

            if (userId == null) {
                synchronized(cache) {
                    val userIds = cache.values.filter { it.isUserCreated }.map { it.id }
                    userIds.forEach { cache.remove(it) }
                }
                _createdMovies.value = emptyList()
                _favoriteMovies.value = emptyList()
                _favoriteIds.value = emptySet()
                return
            }

            val created = storageService.loadCreatedMovies(userId)
            val favorites = storageService.loadFavoriteMovies(userId)
            synchronized(cache) {
                val staleUserIds = cache.values.filter { it.isUserCreated }.map { it.id }
                staleUserIds.forEach { cache.remove(it) }
                created.forEach { cache[it.id] = it }
                favorites.forEach { movie ->
                    if (!cache.containsKey(movie.id)) {
                        cache[movie.id] = movie
                    }
                }
            }
            _createdMovies.value = created
            _favoriteMovies.value = favorites
            _favoriteIds.value = favorites.map { it.id }.toSet()
        }
    }

    suspend fun fetchMoviesPage(pageToken: String? = null): MoviePage {
        ensureLocalDataLoaded()
        val page = apiService.fetchMoviesPage(pageToken)
        synchronized(cache) {
            page.movies.forEach { movie ->
                cache[movie.id] = movie
            }
        }
        return page
    }

    suspend fun getMovieById(movieId: String): Movie? {
        ensureLocalDataLoaded()
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
            val currentUserId = authService.currentUser.value?.userId
            if (currentUserId != null && movieId in _favoriteIds.value) {
                _favoriteMovies.update { list ->
                    list.map { if (it.id == movieId) merged else it }
                }
                persistFavorites(currentUserId)
            }
        }
        return merged
    }

    suspend fun getMoviesByGenre(genre: String): List<Movie> {
        ensureLocalDataLoaded()
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

    suspend fun createAndSaveMovie(dto: MovieCreateDto): Movie {
        val userId = authService.currentUser.value?.userId
            ?: error("Потрібна авторизація для створення фільмів")

        ensureLocalDataLoaded()
        val localPosterUrl = dto.posterUri?.let { uri ->
            storageService.copyPosterToAppData(uri)
        } ?: dto.posterUri

        val movie = dto.copy(posterUri = localPosterUrl).toMovie()
        synchronized(cache) {
            cache[movie.id] = movie
        }
        val updated = listOf(movie) + _createdMovies.value.filterNot { it.id == movie.id }
        _createdMovies.value = updated
        storageService.saveCreatedMovies(updated, userId)
        return movie
    }

    suspend fun deleteCreatedMovie(movieId: String) {
        val userId = authService.currentUser.value?.userId ?: return
        ensureLocalDataLoaded()
        val target = _createdMovies.value.firstOrNull { it.id == movieId }
            ?: synchronized(cache) { cache[movieId] }

        synchronized(cache) {
            cache.remove(movieId)
        }
        val updatedCreated = _createdMovies.value.filterNot { it.id == movieId }
        _createdMovies.value = updatedCreated
        storageService.saveCreatedMovies(updatedCreated, userId)

        if (movieId in _favoriteIds.value) {
            _favoriteIds.update { it - movieId }
            val updatedFavorites = _favoriteMovies.value.filterNot { it.id == movieId }
            _favoriteMovies.value = updatedFavorites
            storageService.saveFavoriteMovies(updatedFavorites, userId)
        }

        storageService.deletePosterFromAppData(target?.posterUrl)
    }

    fun addToFavorites(movie: Movie) {
        val userId = authService.currentUser.value?.userId ?: return
        if (movie.id in _favoriteIds.value) return
        _favoriteIds.update { it + movie.id }
        _favoriteMovies.update { current ->
            if (current.any { it.id == movie.id }) current else current + movie
        }
        persistFavorites(userId)
    }

    fun removeFromFavorites(movieId: String) {
        val userId = authService.currentUser.value?.userId ?: return
        if (movieId !in _favoriteIds.value) return
        _favoriteIds.update { it - movieId }
        _favoriteMovies.update { current -> current.filterNot { it.id == movieId } }
        persistFavorites(userId)
    }

    fun toggleFavorite(movie: Movie) {
        if (authService.currentUser.value == null) return
        if (movie.id in _favoriteIds.value) {
            removeFromFavorites(movie.id)
        } else {
            addToFavorites(movie)
        }
    }

    private fun persistFavorites(userId: String) {
        val snapshot = _favoriteMovies.value
        repositoryScope.launch {
            storageService.saveFavoriteMovies(snapshot, userId)
        }
    }
}
