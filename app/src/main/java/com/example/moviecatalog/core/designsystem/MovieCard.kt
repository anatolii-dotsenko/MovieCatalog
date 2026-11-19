package com.example.moviecatalog.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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

private val CubicOutEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

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
            MovieCard(movie = sampleMovie)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MovieCardDarkPreview() {
    MovieCatalogTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(12.dp)) {
            MovieCard(movie = sampleMovie)
        }
    }
}

@Preview(showBackground = true, widthDp = 440)
@Composable
private fun MovieCardCompactPreview() {
    MovieCatalogTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(12.dp)) {
            MovieCard(movie = sampleMovie, compact = true)
        }
    }
}
