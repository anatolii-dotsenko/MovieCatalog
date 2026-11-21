package com.example.moviecatalog.feature.catalog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviecatalog.MovieCatalogApp
import com.example.moviecatalog.R
import com.example.moviecatalog.core.navigation.INavigationService
import com.example.moviecatalog.data.auth.IAuthService
import com.example.moviecatalog.data.local.ILocalizationService
import com.example.moviecatalog.data.repository.MovieRepository
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CatalogFilterMode {
    ALL,
    FAVORITES,
    CREATED
}

@Immutable
data class CatalogUiState(
    val movies: List<Movie> = emptyList(),
    val totalLoadedInBuffer: Int = 0,
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val nextPageToken: String? = null,
    val endReached: Boolean = false,
    val currentPage: Int = 0,
    val selectedMovieId: String? = null,
    val filterMode: CatalogFilterMode = CatalogFilterMode.ALL,
    val favoriteIds: Set<String> = emptySet(),
    val createdCount: Int = 0,
    val currentUser: UserProfile? = null,
    val authStatusMessage: String? = null
) {
    val isFavoritesMode: Boolean
        get() = filterMode == CatalogFilterMode.FAVORITES

    val isCreatedMode: Boolean
        get() = filterMode == CatalogFilterMode.CREATED

    val isAuthorized: Boolean
        get() = currentUser != null
}

class CatalogViewModel(
    private val repository: MovieRepository,
    private val navigationService: INavigationService,
    private val authService: IAuthService,
    private val localizationService: ILocalizationService
) : ViewModel() {

    private val titlesLoaded = mutableListOf<Movie>()
    private val animatedMovieIds = mutableSetOf<String>()
    private var visibleCount = 0

    private val _uiState = MutableStateFlow(
        CatalogUiState(
            currentUser = authService.currentUser.value,
            authStatusMessage = authService.authStatusMessage.value
        )
    )
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.update { state ->
                    val nextFilter = if (user == null) CatalogFilterMode.ALL else state.filterMode
                    applyFilterProjection(
                        state.copy(
                            currentUser = user,
                            filterMode = nextFilter
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            authService.authStatusMessage.collect { status ->
                _uiState.update { it.copy(authStatusMessage = status) }
            }
        }
        viewModelScope.launch {
            repository.favoriteIds.collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }
        viewModelScope.launch {
            repository.favoriteMovies.collect {
                _uiState.update { state ->
                    if (state.filterMode == CatalogFilterMode.FAVORITES) {
                        applyFilterProjection(state)
                    } else {
                        state
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.createdMovies.collect { created ->
                _uiState.update { state ->
                    val updated = state.copy(createdCount = created.size)
                    if (updated.filterMode == CatalogFilterMode.ALL ||
                        updated.filterMode == CatalogFilterMode.CREATED
                    ) {
                        applyFilterProjection(updated)
                    } else {
                        updated
                    }
                }
            }
        }
        loadInitial()
    }

    fun shouldAnimateItem(movieId: String): Boolean = movieId !in animatedMovieIds

    fun markItemAnimated(movieId: String) {
        animatedMovieIds.add(movieId)
    }

    fun onMovieClick(movie: Movie) {
        _uiState.update { it.copy(selectedMovieId = movie.id) }
        navigationService.navigateToDetails(movie.id)
        _uiState.update { it.copy(selectedMovieId = null) }
    }

    fun onCreateMovieClick() {
        if (!_uiState.value.isAuthorized) return
        navigationService.navigateToCreateMovie()
    }

    fun toggleFavoritesMode() {
        if (!_uiState.value.isAuthorized) return
        _uiState.update { state ->
            val nextMode = if (state.filterMode == CatalogFilterMode.FAVORITES) {
                CatalogFilterMode.ALL
            } else {
                CatalogFilterMode.FAVORITES
            }
            applyFilterProjection(
                state.copy(
                    filterMode = nextMode,
                    isLoadingMore = false,
                    errorMessage = null
                )
            )
        }
    }

    fun toggleCreatedMode() {
        if (!_uiState.value.isAuthorized) return
        _uiState.update { state ->
            val nextMode = if (state.filterMode == CatalogFilterMode.CREATED) {
                CatalogFilterMode.ALL
            } else {
                CatalogFilterMode.CREATED
            }
            applyFilterProjection(
                state.copy(
                    filterMode = nextMode,
                    isLoadingMore = false,
                    errorMessage = null
                )
            )
        }
    }

    fun manageFavoriteBySwipe(movie: Movie) {
        if (!_uiState.value.isAuthorized) return
        when (_uiState.value.filterMode) {
            CatalogFilterMode.FAVORITES -> repository.removeFromFavorites(movie.id)
            CatalogFilterMode.CREATED -> {
                viewModelScope.launch {
                    repository.deleteCreatedMovie(movie.id)
                }
            }
            CatalogFilterMode.ALL -> repository.addToFavorites(movie)
        }
    }

    fun loadInitial() {
        viewModelScope.launch {
            titlesLoaded.clear()
            animatedMovieIds.clear()
            visibleCount = 0

            _uiState.update { state ->
                val loadingState = state.copy(
                    isInitialLoading = state.filterMode == CatalogFilterMode.ALL,
                    isLoadingMore = false,
                    errorMessage = null,
                    currentPage = 0
                )
                applyFilterProjection(loadingState)
            }

            try {
                repository.ensureLocalDataLoaded()
                val page = repository.fetchMoviesPage(pageToken = null)
                appendToBuffer(page.movies)
                visibleCount = minOf(VISIBLE_CHUNK_SIZE, titlesLoaded.size)

                _uiState.update { state ->
                    val nextState = state.copy(
                        isInitialLoading = false,
                        nextPageToken = page.nextPageToken,
                        currentPage = if (titlesLoaded.isNotEmpty()) 1 else 0
                    )
                    applyFilterProjection(nextState)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        errorMessage = e.localizedMessage
                            ?: localizationService.getString(R.string.error_load_catalog)
                    )
                }
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.filterMode != CatalogFilterMode.ALL ||
            current.isInitialLoading ||
            current.isLoadingMore ||
            current.endReached
        ) {
            return
        }

        viewModelScope.launch {
            if (visibleCount < titlesLoaded.size) {
                visibleCount = minOf(visibleCount + VISIBLE_CHUNK_SIZE, titlesLoaded.size)
                val remainingBuffered = titlesLoaded.size - visibleCount
                val shouldPrefetch = remainingBuffered < VISIBLE_CHUNK_SIZE && current.nextPageToken != null

                _uiState.update { state -> applyFilterProjection(state) }

                if (shouldPrefetch) {
                    fetchNextPageIntoBuffer()
                }
            } else if (current.nextPageToken != null) {
                fetchNextPageIntoBuffer(revealNextChunk = true)
            }
        }
    }

    private suspend fun fetchNextPageIntoBuffer(revealNextChunk: Boolean = false) {
        val token = _uiState.value.nextPageToken ?: return
        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        try {
            val page = repository.fetchMoviesPage(pageToken = token)
            appendToBuffer(page.movies)
            if (revealNextChunk) {
                visibleCount = minOf(visibleCount + VISIBLE_CHUNK_SIZE, titlesLoaded.size)
            }

            _uiState.update { state ->
                applyFilterProjection(
                    state.copy(
                        isLoadingMore = false,
                        nextPageToken = page.nextPageToken,
                        currentPage = state.currentPage + 1
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    errorMessage = e.localizedMessage
                        ?: localizationService.getString(R.string.error_load_more_movies)
                )
            }
        }
    }

    private fun applyFilterProjection(state: CatalogUiState): CatalogUiState {
        val created = if (state.currentUser != null) repository.createdMovies.value else emptyList()
        val favorites = if (state.currentUser != null) repository.favoriteMovies.value else emptyList()
        return when (state.filterMode) {
            CatalogFilterMode.FAVORITES -> state.copy(
                movies = favorites,
                totalLoadedInBuffer = favorites.size,
                endReached = true,
                createdCount = created.size
            )
            CatalogFilterMode.CREATED -> state.copy(
                movies = created,
                totalLoadedInBuffer = created.size,
                endReached = true,
                createdCount = created.size
            )
            CatalogFilterMode.ALL -> {
                val visibleApiMovies = titlesLoaded.take(visibleCount)
                val merged = (created + visibleApiMovies).distinctBy { it.id }
                state.copy(
                    movies = merged,
                    totalLoadedInBuffer = created.size + titlesLoaded.size,
                    endReached = visibleCount >= titlesLoaded.size && state.nextPageToken == null,
                    createdCount = created.size
                )
            }
        }
    }

    private fun appendToBuffer(incoming: List<Movie>) {
        val existingIds = titlesLoaded.asSequence().map { it.id }.toHashSet()
        incoming.forEach { movie ->
            if (existingIds.add(movie.id)) {
                titlesLoaded.add(movie)
            }
        }
    }

    companion object {
        private const val VISIBLE_CHUNK_SIZE = 6

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MovieCatalogApp
                CatalogViewModel(
                    repository = app.movieRepository,
                    navigationService = app.navigationService,
                    authService = app.authService,
                    localizationService = app.localizationService
                )
            }
        }
    }
}
