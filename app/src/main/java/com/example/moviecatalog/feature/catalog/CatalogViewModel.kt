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
    val selectedMovieId: String? = null
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

    fun loadInitial() {
        viewModelScope.launch {
            titlesLoaded.clear()
            animatedMovieIds.clear()
            visibleCount = 0

            _uiState.update {
                it.copy(
                    movies = emptyList(),
                    totalLoadedInBuffer = 0,
                    isInitialLoading = true,
                    isLoadingMore = false,
                    errorMessage = null,
                    endReached = false,
                    currentPage = 0
                )
            }
            try {
                val page = repository.fetchMoviesPage(pageToken = null)
                appendToBuffer(page.movies)
                visibleCount = minOf(VISIBLE_CHUNK_SIZE, titlesLoaded.size)
                val visibleSnapshot = titlesLoaded.take(visibleCount)

                _uiState.update {
                    it.copy(
                        movies = visibleSnapshot,
                        totalLoadedInBuffer = titlesLoaded.size,
                        isInitialLoading = false,
                        nextPageToken = page.nextPageToken,
                        endReached = visibleCount >= titlesLoaded.size && page.nextPageToken == null,
                        currentPage = if (titlesLoaded.isNotEmpty()) 1 else 0
                    )
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
        if (current.isInitialLoading || current.isLoadingMore || current.endReached) {
            return
        }

        viewModelScope.launch {
            if (visibleCount < titlesLoaded.size) {
                visibleCount = minOf(visibleCount + VISIBLE_CHUNK_SIZE, titlesLoaded.size)
                val visibleSnapshot = titlesLoaded.take(visibleCount)
                val remainingBuffered = titlesLoaded.size - visibleCount
                val shouldPrefetch = remainingBuffered < VISIBLE_CHUNK_SIZE && current.nextPageToken != null

                _uiState.update {
                    it.copy(
                        movies = visibleSnapshot,
                        totalLoadedInBuffer = titlesLoaded.size,
                        endReached = visibleCount >= titlesLoaded.size && it.nextPageToken == null
                    )
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
                state.copy(
                    movies = visibleSnapshot,
                    totalLoadedInBuffer = titlesLoaded.size,
                    isLoadingMore = false,
                    nextPageToken = page.nextPageToken,
                    endReached = visibleCount >= titlesLoaded.size && page.nextPageToken == null,
                    currentPage = state.currentPage + 1
                )
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
