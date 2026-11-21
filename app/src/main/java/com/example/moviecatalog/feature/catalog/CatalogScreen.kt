package com.example.moviecatalog.feature.catalog

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviecatalog.R
import com.example.moviecatalog.core.designsystem.SwipeableMovieCard
import com.example.moviecatalog.core.designsystem.movieCardContainer
import com.example.moviecatalog.core.navigation.UserAvatarBadge
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.UserProfile
import com.example.moviecatalog.ui.theme.MovieCatalogTheme

@Composable
fun CatalogScreen(
    onSignInClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CatalogContent(
        state = state,
        onRetryInitial = viewModel::loadInitial,
        onLoadMore = viewModel::loadMore,
        onMovieClick = viewModel::onMovieClick,
        onToggleFavoritesMode = viewModel::toggleFavoritesMode,
        onToggleCreatedMode = viewModel::toggleCreatedMode,
        onCreateMovieClick = viewModel::onCreateMovieClick,
        onManageFavorite = viewModel::manageFavoriteBySwipe,
        onSignInClick = onSignInClick,
        onProfileClick = onProfileClick,
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
    onToggleFavoritesMode: () -> Unit,
    onToggleCreatedMode: () -> Unit,
    onCreateMovieClick: () -> Unit,
    onManageFavorite: (Movie) -> Unit,
    onSignInClick: () -> Unit,
    onProfileClick: () -> Unit = {},
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
        state.endReached,
        state.filterMode
    ) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex to totalItems
        }.collect { (lastVisibleIndex, totalItems) ->
            if (state.filterMode == CatalogFilterMode.ALL &&
                totalItems > 0 &&
                lastVisibleIndex >= totalItems - 3 &&
                !state.isInitialLoading &&
                !state.isLoadingMore &&
                !state.endReached
            ) {
                onLoadMore()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.filterMode == CatalogFilterMode.ALL && state.isInitialLoading && state.movies.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.loading_movies),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            state.filterMode == CatalogFilterMode.ALL && state.errorMessage != null && state.movies.isEmpty() -> {
                Box(
                    modifier = Modifier
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
                            Text(stringResource(R.string.retry_button))
                        }
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = gridColumns,
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = if (state.isAuthorized) 84.dp else 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(
                        key = "summary_header",
                        contentType = "header",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        CatalogHeaderBar(
                            visibleCount = state.movies.size,
                            bufferCount = maxOf(state.movies.size, state.totalLoadedInBuffer),
                            currentPage = state.currentPage,
                            favoritesCount = state.favoriteIds.size,
                            createdCount = state.createdCount,
                            filterMode = state.filterMode,
                            currentUser = state.currentUser,
                            onToggleFavoritesMode = onToggleFavoritesMode,
                            onToggleCreatedMode = onToggleCreatedMode,
                            onCreateMovieClick = onCreateMovieClick,
                            onSignInClick = onSignInClick,
                            onProfileClick = onProfileClick
                        )
                    }

                    if (state.filterMode != CatalogFilterMode.ALL && state.movies.isEmpty()) {
                        item(
                            key = "empty_filtered_state",
                            contentType = "empty_state",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .movieCardContainer()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isFavoritesMode) Icons.Default.Star else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (state.isFavoritesMode) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    },
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = stringResource(
                                        if (state.isFavoritesMode) {
                                            R.string.empty_favorites
                                        } else {
                                            R.string.empty_created
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = state.movies,
                            key = { _, movie -> movie.id },
                            contentType = { _, _ -> "movie_card" }
                        ) { index, movie ->
                            val shouldAnimate = remember(movie.id, state.filterMode) {
                                state.filterMode == CatalogFilterMode.ALL && shouldAnimateMovie(movie.id)
                            }
                            SwipeableMovieCard(
                                movie = movie,
                                swipeEnabled = state.isAuthorized,
                                compact = isLandscape,
                                isFavorite = movie.id in state.favoriteIds,
                                isFavoritesMode = state.isFavoritesMode || state.isCreatedMode,
                                shouldAnimateAppear = shouldAnimate,
                                appearDelayMillis = (index % 6) * 50,
                                onAppearAnimationConsumed = { onMovieAnimated(movie.id) },
                                onCardClick = { onMovieClick(movie) },
                                onSwipeAction = onManageFavorite,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (state.filterMode == CatalogFilterMode.ALL) {
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
                                            text = stringResource(R.string.loading_more_movies),
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
                                            Text(stringResource(R.string.retry_load_more))
                                        }
                                    }
                                } else if (!state.isLoadingMore && state.endReached && state.movies.isNotEmpty()) {
                                    Text(
                                        text = stringResource(R.string.all_movies_loaded),
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

        if (state.isAuthorized) {
            FloatingActionButton(
                onClick = onCreateMovieClick,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_movie_button)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogHeaderBar(
    visibleCount: Int,
    bufferCount: Int,
    currentPage: Int,
    favoritesCount: Int,
    createdCount: Int,
    filterMode: CatalogFilterMode,
    currentUser: UserProfile?,
    onToggleFavoritesMode: () -> Unit,
    onToggleCreatedMode: () -> Unit,
    onCreateMovieClick: () -> Unit,
    onSignInClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFavorites = filterMode == CatalogFilterMode.FAVORITES
    val isCreated = filterMode == CatalogFilterMode.CREATED
    val actionWord = stringResource(
        if (isFavorites || isCreated) R.string.swipe_delete_favorite else R.string.swipe_add_favorite
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (currentUser == null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.auth_required_message),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onSignInClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.sign_in),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onProfileClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UserAvatarBadge(
                        name = currentUser.name,
                        avatarUrl = currentUser.avatarUrl,
                        size = 40.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentUser.tokenRefreshedAt?.let { timestamp ->
                            Text(
                                text = stringResource(R.string.token_refreshed_badge, timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.profile_open_hint),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (filterMode) {
                        CatalogFilterMode.FAVORITES -> stringResource(R.string.showing_favorites, favoritesCount)
                        CatalogFilterMode.CREATED -> stringResource(R.string.showing_created, createdCount)
                        CatalogFilterMode.ALL -> stringResource(
                            R.string.catalog_header_buffer_format,
                            visibleCount,
                            bufferCount,
                            currentPage
                        )
                    },
                    style = MaterialTheme.typography.labelLarge
                )

                if (currentUser != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleFavoritesMode,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFavorites) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = if (isFavorites) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.show_favorites, favoritesCount),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onToggleCreatedMode,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCreated) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = if (isCreated) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.my_movies_filter, createdCount),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onCreateMovieClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.add_movie_button),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (currentUser != null) {
            Text(
                text = stringResource(R.string.swipe_hint, actionWord),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private val previewMovies = listOf(
    Movie(
        id = "user_1",
        title = "Мій локальний фільм",
        year = 2026,
        rating = 9.5,
        posterUrl = null,
        plot = "Створено користувачем у застосунку.",
        genres = listOf("Drama"),
        isUserCreated = true
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
private fun CatalogAuthorizedPreview() {
    MovieCatalogTheme {
        CatalogContent(
            state = CatalogUiState(
                movies = previewMovies,
                totalLoadedInBuffer = 8,
                currentPage = 1,
                favoriteIds = setOf("tt0468569"),
                createdCount = 1,
                currentUser = UserProfile(
                    userId = "google_1",
                    name = "Alisa Shevchenko",
                    email = "alisa.shevchenko@gmail.com",
                    avatarUrl = null,
                    tokenRefreshedAt = "13:15:00"
                )
            ),
            onRetryInitial = {},
            onLoadMore = {},
            onMovieClick = {},
            onToggleFavoritesMode = {},
            onToggleCreatedMode = {},
            onCreateMovieClick = {},
            onManageFavorite = {},
            onSignInClick = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,orientation=portrait")
@Composable
private fun CatalogLoggedOutPreview() {
    MovieCatalogTheme {
        CatalogContent(
            state = CatalogUiState(
                movies = listOf(previewMovies[1]),
                totalLoadedInBuffer = 8,
                currentPage = 1,
                currentUser = null
            ),
            onRetryInitial = {},
            onLoadMore = {},
            onMovieClick = {},
            onToggleFavoritesMode = {},
            onToggleCreatedMode = {},
            onCreateMovieClick = {},
            onManageFavorite = {},
            onSignInClick = {}
        )
    }
}
