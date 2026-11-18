package com.example.moviecatalog.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviecatalog.MovieCatalogApp
import com.example.moviecatalog.data.repository.MovieRepository
import com.example.moviecatalog.domain.model.Movie
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val movies: List<Movie> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val nextPageToken: String? = null,
    val endReached: Boolean = false,
    val currentPage: Int = 0
)

class CatalogViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInitialLoading = true,
                    isLoadingMore = false,
                    errorMessage = null,
                    endReached = false
                )
            }
            try {
                val page = repository.fetchMoviesPage(pageToken = null)
                _uiState.update {
                    it.copy(
                        movies = page.movies,
                        isInitialLoading = false,
                        nextPageToken = page.nextPageToken,
                        endReached = page.nextPageToken == null,
                        currentPage = if (page.movies.isNotEmpty()) 1 else 0
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
        val token = current.nextPageToken
        if (current.isInitialLoading || current.isLoadingMore || current.endReached || token == null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
            try {
                val page = repository.fetchMoviesPage(pageToken = token)
                _uiState.update { state ->
                    val merged = (state.movies + page.movies).distinctBy { it.id }
                    state.copy(
                        movies = merged,
                        isLoadingMore = false,
                        nextPageToken = page.nextPageToken,
                        endReached = page.nextPageToken == null,
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
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MovieCatalogApp
                CatalogViewModel(app.movieRepository)
            }
        }
    }
}
