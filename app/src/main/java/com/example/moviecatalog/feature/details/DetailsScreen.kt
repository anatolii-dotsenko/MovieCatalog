package com.example.moviecatalog.feature.details

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.moviecatalog.R
import com.example.moviecatalog.core.designsystem.metadataBadge
import com.example.moviecatalog.core.designsystem.movieCardContainer
import com.example.moviecatalog.core.designsystem.posterFrame
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.ui.theme.MovieCatalogTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DetailsEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

@Composable
fun DetailsScreen(
    movieId: String,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = viewModel(
        key = movieId,
        factory = DetailsViewModel.factory(movieId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DetailsContent(
        state = state,
        onRetry = viewModel::retry,
        onBackClick = viewModel::onBackClick,
        onGenreClick = viewModel::onGenreClick,
        onRemoveFromFavorites = viewModel::onRemoveFromFavorites,
        onToggleFavorite = viewModel::onToggleFavorite,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsContent(
    state: DetailsUiState,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onGenreClick: (String) -> Unit,
    onRemoveFromFavorites: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.errorMessage != null || state.movie == null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.errorMessage ?: "Дані фільму відсутні",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Спробувати знову")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onBackClick) {
                        Text(stringResource(R.string.back_to_list))
                    }
                }
            }
        }

        else -> {
            val movie = state.movie
            val context = LocalContext.current
            val swipeBackThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .pointerInput(movie.id) {
                        var totalRightDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalRightDrag = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0f) {
                                    totalRightDrag += dragAmount
                                } else {
                                    totalRightDrag = 0f
                                }
                            },
                            onDragEnd = {
                                if (totalRightDrag >= swipeBackThresholdPx) {
                                    onBackClick()
                                }
                            }
                        )
                    }
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StaggeredReveal(itemKey = movie.id, delayMillis = 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .movieCardContainer()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(195.dp)
                                .posterFrame()
                                .pointerInput(movie.id) {
                                    detectTapGestures(
                                        onDoubleTap = { onToggleFavorite() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(movie.posterUrl)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(26.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                },
                                error = {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            )

                            if (state.isFavorite) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = stringResource(R.string.favorite_badge),
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = movie.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            movie.originalTitle?.let { original ->
                                Text(
                                    text = stringResource(R.string.original_title_format, original),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (movie.isUserCreated) {
                                    Text(
                                        text = stringResource(R.string.user_created_badge),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.metadataBadge(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                }

                                movie.year?.let { year ->
                                    Text(
                                        text = stringResource(R.string.year_format, year),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.metadataBadge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }

                                movie.runtimeMinutes?.let { minutes ->
                                    Text(
                                        text = stringResource(R.string.runtime_format, minutes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.metadataBadge(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                }

                                movie.rating?.let { rating ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.metadataBadge(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.rating_format, rating),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            movie.voteCount?.let { votes ->
                                Text(
                                    text = stringResource(R.string.votes_format, votes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = stringResource(R.string.double_tap_poster_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (movie.genres.isNotEmpty()) {
                    StaggeredReveal(itemKey = movie.id, delayMillis = 70) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.genres_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                movie.genres.forEach { genre ->
                                    AssistChip(
                                        onClick = { onGenreClick(genre) },
                                        label = { Text(genre) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (movie.director != null || movie.country != null || movie.cast.isNotEmpty()) {
                    StaggeredReveal(itemKey = movie.id, delayMillis = 140) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .movieCardContainer()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            movie.director?.let { director ->
                                DetailField(
                                    label = stringResource(R.string.director_label),
                                    value = director
                                )
                            }
                            if (movie.cast.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                DetailField(
                                    label = stringResource(R.string.cast_label),
                                    value = movie.cast.joinToString(", ")
                                )
                            }
                            movie.country?.let { country ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                DetailField(
                                    label = stringResource(R.string.country_label),
                                    value = country
                                )
                            }
                        }
                    }
                }

                StaggeredReveal(itemKey = movie.id, delayMillis = 210) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .movieCardContainer()
                            .animateContentSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.plot_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = movie.plot,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StaggeredReveal(itemKey = movie.id, delayMillis = 260) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.isFavorite) {
                            Button(
                                onClick = onRemoveFromFavorites,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.remove_from_favorites))
                            }
                        } else {
                            OutlinedButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.add_to_favorites))
                            }
                        }

                        Button(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.back_to_list))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredReveal(
    itemKey: String,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val initialOffsetPx = with(density) { 24.dp.toPx() }
    val alpha = remember(itemKey) { Animatable(0f) }
    val offsetY = remember(itemKey) { Animatable(initialOffsetPx) }

    LaunchedEffect(itemKey) {
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 360, easing = DetailsEaseOut)
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 360, easing = DetailsEaseOut)
            )
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        }
    ) {
        content()
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailsScreenPreview() {
    MovieCatalogTheme {
        DetailsContent(
            state = DetailsUiState(
                isLoading = false,
                isFavorite = true,
                movie = Movie(
                    id = "tt6751668",
                    title = "Parasite",
                    originalTitle = "Gisaengchung",
                    year = 2019,
                    rating = 8.5,
                    runtimeMinutes = 132,
                    voteCount = 1005000,
                    posterUrl = null,
                    plot = "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan.",
                    genres = listOf("Drama", "Thriller"),
                    director = "Bong Joon Ho",
                    cast = listOf("Song Kang-ho", "Lee Sun-kyun", "Cho Yeo-jeong"),
                    country = "South Korea"
                )
            ),
            onRetry = {},
            onBackClick = {},
            onGenreClick = {},
            onRemoveFromFavorites = {},
            onToggleFavorite = {}
        )
    }
}
