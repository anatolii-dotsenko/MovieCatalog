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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable card container style (Compose analog of MAUI Styles.xaml).
 */
@Composable
fun Modifier.movieCardContainer(
    shape: Shape = MaterialTheme.shapes.medium,
    elevation: Dp = 3.dp
): Modifier = this
    .shadow(elevation = elevation, shape = shape, clip = false)
    .clip(shape)
    .background(MaterialTheme.colorScheme.surfaceContainerLow)
    .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = shape
    )

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
