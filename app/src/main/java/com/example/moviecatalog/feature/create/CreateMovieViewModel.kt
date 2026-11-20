package com.example.moviecatalog.feature.create

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
import com.example.moviecatalog.domain.model.MovieCreateDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class CreateMovieUiState(
    val dto: MovieCreateDto = MovieCreateDto(),
    val titleError: String? = null,
    val yearError: String? = null,
    val ratingError: String? = null,
    val runtimeError: String? = null,
    val permissionGranted: Boolean = false,
    val permissionChecked: Boolean = false,
    val isSaving: Boolean = false
)

class CreateMovieViewModel(
    private val repository: MovieRepository,
    private val navigationService: INavigationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateMovieUiState())
    val uiState: StateFlow<CreateMovieUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(title = value), titleError = null) }
    }

    fun onOriginalTitleChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(originalTitle = value)) }
    }

    fun onYearChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(yearText = value), yearError = null) }
    }

    fun onRatingChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(ratingText = value), ratingError = null) }
    }

    fun onRuntimeChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(runtimeText = value), runtimeError = null) }
    }

    fun onGenresChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(genresText = value)) }
    }

    fun onDirectorChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(director = value)) }
    }

    fun onCastChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(castText = value)) }
    }

    fun onCountryChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(country = value)) }
    }

    fun onPlotChange(value: String) {
        _uiState.update { it.copy(dto = it.dto.copy(plot = value)) }
    }

    fun onPosterSelected(uriString: String?) {
        if (uriString != null) {
            _uiState.update { it.copy(dto = it.dto.copy(posterUri = uriString)) }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted, permissionChecked = true) }
    }

    fun onSaveClick() {
        val currentState = _uiState.value
        if (currentState.isSaving) return

        val dto = currentState.dto
        val titleError = if (dto.title.isBlank()) "Введіть назву фільму" else null

        val yearError = if (dto.yearText.isNotBlank()) {
            val year = dto.parsedYear()
            if (year == null || year !in 1888..2100) "Вкажіть рік від 1888 до 2100" else null
        } else {
            null
        }

        val ratingError = if (dto.ratingText.isNotBlank()) {
            val rating = dto.parsedRating()
            if (rating == null || rating !in 0.0..10.0) "Рейтинг має бути від 0.0 до 10.0" else null
        } else {
            null
        }

        val runtimeError = if (dto.runtimeText.isNotBlank()) {
            val runtime = dto.parsedRuntime()
            if (runtime == null || runtime <= 0) "Вкажіть коректну тривалість (хв)" else null
        } else {
            null
        }

        if (titleError != null || yearError != null || ratingError != null || runtimeError != null) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    yearError = yearError,
                    ratingError = ratingError,
                    runtimeError = runtimeError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repository.createAndSaveMovie(dto)
                navigationService.navigateBack()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onCancelClick() {
        navigationService.navigateBack()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MovieCatalogApp
                CreateMovieViewModel(
                    repository = app.movieRepository,
                    navigationService = app.navigationService
                )
            }
        }
    }
}
