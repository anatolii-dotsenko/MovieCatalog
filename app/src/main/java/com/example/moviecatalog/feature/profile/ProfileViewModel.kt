package com.example.moviecatalog.feature.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviecatalog.MovieCatalogApp
import com.example.moviecatalog.core.navigation.INavigationService
import com.example.moviecatalog.data.auth.IAuthService
import com.example.moviecatalog.data.local.AppFontScale
import com.example.moviecatalog.data.local.AppLanguage
import com.example.moviecatalog.data.local.AppThemeMode
import com.example.moviecatalog.data.local.ILocalizationService
import com.example.moviecatalog.data.repository.MovieRepository
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ProfileUiState(
    val user: UserProfile? = null,
    val favoriteMovies: List<Movie> = emptyList(),
    val createdMoviesCount: Int = 0,
    val currentLanguage: AppLanguage = AppLanguage.EN,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val fontScale: AppFontScale = AppFontScale.NORMAL
)

class ProfileViewModel(
    private val repository: MovieRepository,
    private val authService: IAuthService,
    private val localizationService: ILocalizationService,
    private val navigationService: INavigationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            user = authService.currentUser.value,
            favoriteMovies = repository.favoriteMovies.value,
            createdMoviesCount = repository.createdMovies.value.size,
            currentLanguage = localizationService.currentLanguage.value,
            themeMode = localizationService.themeMode.value,
            fontScale = localizationService.fontScale.value
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            repository.favoriteMovies.collect { favorites ->
                _uiState.update { it.copy(favoriteMovies = favorites) }
            }
        }
        viewModelScope.launch {
            repository.createdMovies.collect { created ->
                _uiState.update { it.copy(createdMoviesCount = created.size) }
            }
        }
        viewModelScope.launch {
            localizationService.currentLanguage.collect { language ->
                _uiState.update { it.copy(currentLanguage = language) }
            }
        }
        viewModelScope.launch {
            localizationService.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            localizationService.fontScale.collect { scale ->
                _uiState.update { it.copy(fontScale = scale) }
            }
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        localizationService.setLanguage(language)
    }

    fun onThemeModeSelected(mode: AppThemeMode) {
        localizationService.setThemeMode(mode)
    }

    fun onFontScaleSelected(scale: AppFontScale) {
        localizationService.setFontScale(scale)
    }

    fun onMovieClick(movie: Movie) {
        navigationService.navigateToDetails(movie.id)
    }

    fun onRemoveFavorite(movie: Movie) {
        repository.removeFromFavorites(movie.id)
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            authService.signOut()
            navigationService.navigateBack()
        }
    }

    fun onBackClick() {
        navigationService.navigateBack()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MovieCatalogApp
                ProfileViewModel(
                    repository = app.movieRepository,
                    authService = app.authService,
                    localizationService = app.localizationService,
                    navigationService = app.navigationService
                )
            }
        }
    }
}
