package com.example.moviecatalog.feature.details

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isFavorite: Boolean = false,
    val isAuthorized: Boolean = false
)

class DetailsViewModel(
    private val movieId: String,
    private val repository: MovieRepository,
    private val navigationService: INavigationService,
    private val authService: IAuthService,
    private val localizationService: ILocalizationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DetailsUiState(
            isFavorite = movieId in repository.favoriteIds.value,
            isAuthorized = authService.currentUser.value != null
        )
    )
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.update { it.copy(isAuthorized = user != null) }
            }
        }
        viewModelScope.launch {
            repository.favoriteIds.collect { ids ->
                _uiState.update { it.copy(isFavorite = movieId in ids) }
            }
        }
        loadDetails()
    }

    fun retry() {
        loadDetails()
    }

    fun onBackClick() {
        navigationService.navigateBack()
    }

    fun onGenreClick(genre: String) {
        navigationService.navigateToGenre(genre)
    }

    fun onRemoveFromFavorites() {
        if (!_uiState.value.isAuthorized) return
        repository.removeFromFavorites(movieId)
    }

    fun onToggleFavorite() {
        if (!_uiState.value.isAuthorized) return
        _uiState.value.movie?.let(repository::toggleFavorite)
    }

    fun onDeleteCreatedMovie() {
        if (!_uiState.value.isAuthorized) return
        viewModelScope.launch {
            repository.deleteCreatedMovie(movieId)
            navigationService.navigateBack()
        }
    }

    private fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val movie = repository.getMovieById(movieId)
                if (movie != null) {
                    _uiState.update { it.copy(movie = movie, isLoading = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = localizationService.getString(R.string.error_movie_not_found)
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage
                            ?: localizationService.getString(R.string.error_load_details)
                    )
                }
            }
        }
    }

    companion object {
        fun factory(movieId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MovieCatalogApp
                DetailsViewModel(
                    movieId = movieId,
                    repository = app.movieRepository,
                    navigationService = app.navigationService,
                    authService = app.authService,
                    localizationService = app.localizationService
                )
            }
        }
    }
}
