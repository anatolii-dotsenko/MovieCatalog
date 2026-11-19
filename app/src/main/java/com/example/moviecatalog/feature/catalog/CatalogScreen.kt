package com.example.moviecatalog.feature.catalog

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviecatalog.R
import com.example.moviecatalog.core.designsystem.MovieCard
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.ui.theme.MovieCatalogTheme

@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CatalogContent(
        state = state,
        onRetryInitial = viewModel::loadInitial,
        onLoadMore = viewModel::loadMore,
        onMovieClick = viewModel::onMovieClick,
        shouldAnimateMovie = viewModel::shouldAnimateItem,
        onMovieAnimated = viewModel::markItemAnimated,
        modifier = modifier
    )
}

@Composable
fun CatalogContent(
    state: CatalogUiState,
    onRetryInitial: () -> Unit,
    onLoadMore: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    shouldAnimateMovie: (String) -> Boolean = { false },
    onMovieAnimated: (String) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1)
    val gridState = rememberLazyGridState()

    LaunchedEffect(
        gridState,
        state.movies.size,
        state.isInitialLoading,
        state.isLoadingMore,
        state.endReached
    ) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex to totalItems
        }.collect { (lastVisibleIndex, totalItems) ->
            if (totalItems > 0 &&
                lastVisibleIndex >= totalItems - 3 &&
                !state.isInitialLoading &&
                !state.isLoadingMore &&
                !state.endReached
            ) {
                onLoadMore()
            }
        }
    }

    when {
        state.isInitialLoading && state.movies.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Завантаження фільмів...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        state.errorMessage != null && state.movies.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetryInitial) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Спробувати знову")
                    }
                }
            }
        }

        else -> {
            LazyVerticalGrid(
                columns = gridColumns,
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier.fillMaxSize()
            ) {
                item(
                    key = "summary_header",
                    contentType = "header",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    AnimatedVisibility(
                        visible = state.movies.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        CatalogSummaryHeader(
                            visibleCount = state.movies.size,
                            bufferCount = maxOf(state.movies.size, state.totalLoadedInBuffer),
                            currentPage = state.currentPage
                        )
                    }
                }

                itemsIndexed(
                    items = state.movies,
                    key = { _, movie -> movie.id },
                    contentType = { _, _ -> "movie_card" }
                ) { index, movie ->
                    val shouldAnimate = remember(movie.id) { shouldAnimateMovie(movie.id) }
                    MovieCard(
                        movie = movie,
                        compact = isLandscape,
                        shouldAnimateAppear = shouldAnimate,
                        appearDelayMillis = (index % 6) * 50,
                        onAppearAnimationConsumed = { onMovieAnimated(movie.id) },
                        onClick = { onMovieClick(movie) },
                        modifier = Modifier.animateItem()
                    )
                }

                item(
                    key = "catalog_footer",
                    contentType = "footer",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        AnimatedVisibility(
                            visible = state.isLoadingMore,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Довантаження фільмів...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (!state.isLoadingMore && state.errorMessage != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(onClick = onLoadMore) {
                                    Text("Повторити довантаження")
                                }
                            }
                        } else if (!state.isLoadingMore && state.endReached && state.movies.isNotEmpty()) {
                            Text(
                                text = "Усі доступні фільми завантажено",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSummaryHeader(
    visibleCount: Int,
    bufferCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Text(
            text = stringResource(
                R.string.catalog_header_buffer_format,
                visibleCount,
                bufferCount,
                currentPage
            ),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

private val previewMovies = listOf(
    Movie(
        id = "tt0111161",
        title = "The Shawshank Redemption",
        year = 1994,
        rating = 9.3,
        posterUrl = null,
        plot = "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.",
        genres = listOf("Drama")
    ),
    Movie(
        id = "tt0468569",
        title = "The Dark Knight",
        year = 2008,
        rating = 9.0,
        posterUrl = null,
        plot = "When the menace known as the Joker wreaks havoc on Gotham, Batman must accept one of the greatest psychological tests.",
        genres = listOf("Action", "Crime", "Drama")
    )
)

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,orientation=portrait")
@Composable
private fun CatalogPortraitPreview() {
    MovieCatalogTheme {
        CatalogContent(
            state = CatalogUiState(movies = previewMovies, totalLoadedInBuffer = 8, currentPage = 1),
            onRetryInitial = {},
            onLoadMore = {},
            onMovieClick = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,orientation=landscape")
@Composable
private fun CatalogLandscapePreview() {
    MovieCatalogTheme {
        CatalogContent(
            state = CatalogUiState(movies = previewMovies, totalLoadedInBuffer = 8, currentPage = 1),
            onRetryInitial = {},
            onLoadMore = {},
            onMovieClick = {}
        )
    }
}
