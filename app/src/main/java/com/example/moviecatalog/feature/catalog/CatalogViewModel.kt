package com.example.moviecatalog.feature.catalog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviecatalog.MovieCatalogApp
import com.example.moviecatalog.core.navigation.INavigationService
import com.example.moviecatalog.data.repository.MovieRepository
import com.example.moviecatalog.domain.model.Movie
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val isFavoritesMode: Boolean = false,
    val favoriteIds: Set<String> = emptySet()
)

class CatalogViewModel(
    private val repository: MovieRepository,
    private val navigationService: INavigationService
) : ViewModel() {

    private val titlesLoaded = mutableListOf<Movie>()
    private val animatedMovieIds = mutableSetOf<String>()
    private var visibleCount = 0

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.favoriteIds.collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }
        viewModelScope.launch {
            repository.favoriteMovies.collect { favorites ->
                if (_uiState.value.isFavoritesMode) {
                    _uiState.update {
                        it.copy(
                            movies = favorites,
                            totalLoadedInBuffer = favorites.size,
                            endReached = true
                        )
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

    fun toggleFavoritesMode() {
        val nextFavoritesMode = !_uiState.value.isFavoritesMode
        if (nextFavoritesMode) {
            val favorites = repository.favoriteMovies.value
            _uiState.update {
                it.copy(
                    isFavoritesMode = true,
                    movies = favorites,
                    totalLoadedInBuffer = favorites.size,
                    isLoadingMore = false,
                    errorMessage = null,
                    endReached = true
                )
            }
        } else {
            val visibleSnapshot = titlesLoaded.take(visibleCount)
            _uiState.update {
                it.copy(
                    isFavoritesMode = false,
                    movies = visibleSnapshot,
                    totalLoadedInBuffer = titlesLoaded.size,
                    errorMessage = null,
                    endReached = visibleCount >= titlesLoaded.size && it.nextPageToken == null
                )
            }
        }
    }

    fun manageFavoriteBySwipe(movie: Movie) {
        if (_uiState.value.isFavoritesMode) {
            repository.removeFromFavorites(movie.id)
        } else {
            repository.addToFavorites(movie)
        }
    }

    fun loadInitial() {
        viewModelScope.launch {
            titlesLoaded.clear()
            animatedMovieIds.clear()
            visibleCount = 0

            _uiState.update {
                it.copy(
                    movies = if (it.isFavoritesMode) repository.favoriteMovies.value else emptyList(),
                    totalLoadedInBuffer = if (it.isFavoritesMode) repository.favoriteMovies.value.size else 0,
                    isInitialLoading = !it.isFavoritesMode,
                    isLoadingMore = false,
                    errorMessage = null,
                    endReached = it.isFavoritesMode,
                    currentPage = 0
                )
            }
            try {
                val page = repository.fetchMoviesPage(pageToken = null)
                appendToBuffer(page.movies)
                visibleCount = minOf(VISIBLE_CHUNK_SIZE, titlesLoaded.size)
                val visibleSnapshot = titlesLoaded.take(visibleCount)

                _uiState.update { state ->
                    if (state.isFavoritesMode) {
                        state.copy(
                            isInitialLoading = false,
                            nextPageToken = page.nextPageToken,
                            currentPage = if (titlesLoaded.isNotEmpty()) 1 else 0
                        )
                    } else {
                        state.copy(
                            movies = visibleSnapshot,
                            totalLoadedInBuffer = titlesLoaded.size,
                            isInitialLoading = false,
                            nextPageToken = page.nextPageToken,
                            endReached = visibleCount >= titlesLoaded.size && page.nextPageToken == null,
                            currentPage = if (titlesLoaded.isNotEmpty()) 1 else 0
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        errorMessage = e.localizedMessage ?: "Не вдалося завантажити каталог"
                    )
                }
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isFavoritesMode || current.isInitialLoading || current.isLoadingMore || current.endReached) {
            return
        }

        viewModelScope.launch {
            if (visibleCount < titlesLoaded.size) {
                visibleCount = minOf(visibleCount + VISIBLE_CHUNK_SIZE, titlesLoaded.size)
                val visibleSnapshot = titlesLoaded.take(visibleCount)
                val remainingBuffered = titlesLoaded.size - visibleCount
                val shouldPrefetch = remainingBuffered < VISIBLE_CHUNK_SIZE && current.nextPageToken != null

                _uiState.update { state ->
                    if (state.isFavoritesMode) {
                        state
                    } else {
                        state.copy(
                            movies = visibleSnapshot,
                            totalLoadedInBuffer = titlesLoaded.size,
                            endReached = visibleCount >= titlesLoaded.size && state.nextPageToken == null
                        )
                    }
                }

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
            val visibleSnapshot = titlesLoaded.take(visibleCount)

            _uiState.update { state ->
                if (state.isFavoritesMode) {
                    state.copy(
                        isLoadingMore = false,
                        nextPageToken = page.nextPageToken,
                        currentPage = state.currentPage + 1
                    )
                } else {
                    state.copy(
                        movies = visibleSnapshot,
                        totalLoadedInBuffer = titlesLoaded.size,
                        isLoadingMore = false,
                        nextPageToken = page.nextPageToken,
                        endReached = visibleCount >= titlesLoaded.size && page.nextPageToken == null,
                        currentPage = state.currentPage + 1
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    errorMessage = e.localizedMessage ?: "Не вдалося довантажити фільми"
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
                    navigationService = app.navigationService
                )
            }
        }
    }
}
