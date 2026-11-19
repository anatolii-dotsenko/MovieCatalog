package com.example.moviecatalog.feature.genre

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moviecatalog.MovieCatalogApp
import com.example.moviecatalog.R
import com.example.moviecatalog.core.designsystem.MovieCard
import com.example.moviecatalog.core.navigation.INavigationService
import com.example.moviecatalog.data.repository.MovieRepository
import com.example.moviecatalog.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GenreMoviesUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true
)

class GenreMoviesViewModel(
    private val genre: String,
    private val repository: MovieRepository,
    private val navigationService: INavigationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenreMoviesUiState())
    val uiState: StateFlow<GenreMoviesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val filtered = repository.getMoviesByGenre(genre)
            _uiState.value = GenreMoviesUiState(movies = filtered, isLoading = false)
        }
    }

    fun onMovieClick(movie: Movie) {
        navigationService.navigateToDetails(movie.id)
    }

    fun onBackClick() {
        navigationService.navigateBack()
    }

    companion object {
        fun factory(genre: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MovieCatalogApp
                GenreMoviesViewModel(
                    genre = genre,
                    repository = app.movieRepository,
                    navigationService = app.navigationService
                )
            }
        }
    }
}

@Composable
fun GenreMoviesScreen(
    genre: String,
    modifier: Modifier = Modifier,
    viewModel: GenreMoviesViewModel = viewModel(
        key = genre,
        factory = GenreMoviesViewModel.factory(genre)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1)

    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.movies.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.no_genre_movies, genre),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = viewModel::onBackClick) {
                        Text(stringResource(R.string.back_to_list))
                    }
                }
            }
        }

        else -> {
            LazyVerticalGrid(
                columns = gridColumns,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier.fillMaxSize()
            ) {
                items(
                    items = state.movies,
                    key = { it.id }
                ) { movie ->
                    MovieCard(
                        movie = movie,
                        compact = isLandscape,
                        onClick = { viewModel.onMovieClick(movie) }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedButton(
                        onClick = viewModel::onBackClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(stringResource(R.string.back_to_list))
                    }
                }
            }
        }
    }
}
