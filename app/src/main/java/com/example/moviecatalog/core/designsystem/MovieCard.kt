package com.example.moviecatalog.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.moviecatalog.R
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.ui.theme.MovieCatalogTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CubicOutEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

/**
 * Swipe-to-reveal wrapper for [MovieCard] (analog of MAUI SwipeView + LeftItems).
 * Swiping right reveals the action panel without triggering [onCardClick].
 */
@Composable
fun SwipeableMovieCard(
    movie: Movie,
    isFavorite: Boolean,
    isFavoritesMode: Boolean,
    onCardClick: () -> Unit,
    onSwipeAction: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    shouldAnimateAppear: Boolean = false,
    appearDelayMillis: Int = 50,
    onAppearAnimationConsumed: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val revealWidth = 104.dp
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val offsetX = remember(movie.id, isFavoritesMode) { Animatable(0f) }

    val actionBackgroundColor = if (isFavoritesMode) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val actionContentColor = if (isFavoritesMode) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val actionIcon = if (isFavoritesMode) Icons.Default.Delete else Icons.Default.Star
    val actionLabel = stringResource(
        if (isFavoritesMode) R.string.swipe_delete_favorite else R.string.swipe_add_favorite
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(actionBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .width(revealWidth)
                    .fillMaxHeight()
                    .clickable(enabled = offsetX.value > 2f) {
                        scope.launch {
                            offsetX.animateTo(0f, animationSpec = tween(180))
                            onSwipeAction(movie)
                        }
                    }
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionLabel,
                    tint = actionContentColor,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = actionContentColor
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(movie.id, isFavoritesMode) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(0f, revealWidthPx)
                                offsetX.snapTo(next)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                val target = if (offsetX.value >= revealWidthPx * 0.45f) {
                                    revealWidthPx
                                } else {
                                    0f
                                }
                                offsetX.animateTo(target, animationSpec = tween(200))
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, animationSpec = tween(200))
                            }
                        }
                    )
                }
        ) {
            MovieCard(
                movie = movie,
                compact = compact,
                isFavorite = isFavorite,
                shouldAnimateAppear = shouldAnimateAppear,
                appearDelayMillis = appearDelayMillis,
                onAppearAnimationConsumed = onAppearAnimationConsumed,
                onClick = {
                    if (offsetX.value > 2f) {
                        scope.launch { offsetX.animateTo(0f, animationSpec = tween(180)) }
                    } else {
                        onCardClick()
                    }
                }
            )
        }
    }
}

/**
 * Reusable movie list item component configured via [movie].
 * Includes draw-phase appear animation (alpha + vertical slide) and interactive press scale.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    isFavorite: Boolean = false,
    shouldAnimateAppear: Boolean = false,
    appearDelayMillis: Int = 50,
    onAppearAnimationConsumed: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val posterWidth = if (compact) 78.dp else 96.dp
    val posterHeight = if (compact) 116.dp else 144.dp
    val posterWidthPx = with(density) { posterWidth.roundToPx() }
    val posterHeightPx = with(density) { posterHeight.roundToPx() }
    val cardPadding = if (compact) 10.dp else 12.dp
    val plotMaxLines = if (compact) 2 else 3

    val startOffsetPx = with(density) { 40.dp.toPx() }
    val alpha = remember(movie.id) { Animatable(if (shouldAnimateAppear) 0f else 1f) }
    val offsetY = remember(movie.id) { Animatable(if (shouldAnimateAppear) startOffsetPx else 0f) }
    var hasStartedAnimation by remember(movie.id) { mutableStateOf(false) }

    LaunchedEffect(movie.id) {
        if (shouldAnimateAppear && !hasStartedAnimation) {
            hasStartedAnimation = true
            onAppearAnimationConsumed?.invoke()
            if (appearDelayMillis > 0) {
                delay(appearDelayMillis.toLong())
            }
            coroutineScope {
                launch {
                    alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 420, easing = CubicOutEasing)
                    )
                }
                launch {
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 420, easing = CubicOutEasing)
                    )
                }
            }
        } else if (!hasStartedAnimation) {
            alpha.snapTo(1f)
            offsetY.snapTo(0f)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "movie_card_press_scale"
    )

    val cardModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
            scaleX = pressScale
            scaleY = pressScale
        }
        .movieCardContainer()
        .let { base ->
            if (onClick != null) {
                base.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
            } else {
                base
            }
        }

    Row(
        modifier = cardModifier.padding(cardPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(posterWidth)
                .height(posterHeight)
                .posterFrame(),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(movie.posterUrl)
                    .size(posterWidthPx, posterHeightPx)
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
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    PosterPlaceholder(title = movie.title)
                }
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

                if (isFavorite) {
                    Text(
                        text = stringResource(R.string.favorite_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.metadataBadge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            if (movie.genres.isNotEmpty()) {
                val genresLimit = if (compact) 2 else 3
                Text(
                    text = stringResource(
                        R.string.genres_format,
                        movie.genres.take(genresLimit).joinToString(" • ")
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = movie.plot,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = plotMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PosterPlaceholder(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title.take(2).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val sampleMovie = Movie(
    id = "tt0468569",
    title = "The Dark Knight",
    year = 2008,
    rating = 9.0,
    posterUrl = null,
    plot = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological tests.",
    genres = listOf("Action", "Crime", "Drama")
)

@Preview(showBackground = true)
@Composable
private fun MovieCardLightPreview() {
    MovieCatalogTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(12.dp)) {
            MovieCard(movie = sampleMovie, isFavorite = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MovieCardDarkPreview() {
    MovieCatalogTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(12.dp)) {
            MovieCard(movie = sampleMovie, isFavorite = true)
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun MovieCardCompactPreview() {
    MovieCatalogTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(12.dp)) {
            MovieCard(movie = sampleMovie, compact = true, isFavorite = false)
        }
    }
}
