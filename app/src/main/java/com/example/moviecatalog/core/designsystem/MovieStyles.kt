package com.example.moviecatalog.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable card container style (Compose analog of MAUI Styles.xaml).
 * Supports highlighted border/tint to visually distinguish user-created movies.
 */
@Composable
fun Modifier.movieCardContainer(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 3.dp,
    isHighlighted: Boolean = false,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary
): Modifier {
    val backgroundColor = if (isHighlighted) {
        MaterialTheme.colorScheme.tertiaryContainer
            .copy(alpha = 0.28f)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val borderWidth = if (isHighlighted) 1.8.dp else 1.dp
    val borderColor = if (isHighlighted) highlightColor else MaterialTheme.colorScheme.outlineVariant

    return this
        .shadow(elevation = elevation, shape = shape, clip = false)
        .clip(shape)
        .background(backgroundColor)
        .border(
            width = borderWidth,
            color = borderColor,
            shape = shape
        )
}

/**
 * Reusable poster frame style with rounded clip, border, and placeholder background.
 */
@Composable
fun Modifier.posterFrame(
    shape: Shape = MaterialTheme.shapes.small
): Modifier = this
    .clip(shape)
    .background(MaterialTheme.colorScheme.surfaceVariant)
    .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = shape
    )

/**
 * Reusable pill badge style for movie metadata (year, rating, genre tags).
 */
@Composable
fun Modifier.metadataBadge(
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    shape: Shape = MaterialTheme.shapes.extraSmall
): Modifier = this
    .clip(shape)
    .background(containerColor)
    .padding(horizontal = 8.dp, vertical = 3.dp)
