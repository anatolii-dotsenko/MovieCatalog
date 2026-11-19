package com.example.moviecatalog.feature.catalog

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.grid.items
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
        modifier = modifier
    )
}

@Composable
fun CatalogContent(
    state: CatalogUiState,
    onRetryInitial: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) GridCells.Fixed(2) else GridCells.Fixed(1)
    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState, state.movies.size, state.isInitialLoading, state.isLoadingMore, state.endReached) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex to totalItems
        }.collect { (lastVisibleIndex, totalItems) ->
            if (totalItems > 0 &&
                lastVisibleIndex >= totalItems - 4 &&
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CatalogSummaryHeader(
                        loadedCount = state.movies.size,
                        currentPage = state.currentPage
                    )
                }

                items(
                    items = state.movies,
                    key = { it.id }
                ) { movie ->
                    MovieCard(
                        movie = movie,
                        compact = isLandscape
                    )
                }

                if (state.isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
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
                } else if (state.errorMessage != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
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
                    }
                } else if (state.endReached && state.movies.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
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

@Composable
private fun CatalogSummaryHeader(
    loadedCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.catalog_header_format, loadedCount, currentPage),
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
            state = CatalogUiState(movies = previewMovies, currentPage = 1),
            onRetryInitial = {},
            onLoadMore = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,orientation=landscape")
@Composable
private fun CatalogLandscapePreview() {
    MovieCatalogTheme {
        CatalogContent(
            state = CatalogUiState(movies = previewMovies, currentPage = 1),
            onRetryInitial = {},
            onLoadMore = {}
        )
    }
}
