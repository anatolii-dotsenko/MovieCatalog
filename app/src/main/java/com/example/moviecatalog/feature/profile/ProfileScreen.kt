package com.example.moviecatalog.feature.profile

import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moviecatalog.R
import com.example.moviecatalog.core.designsystem.SwipeableMovieCard
import com.example.moviecatalog.core.designsystem.metadataBadge
import com.example.moviecatalog.core.designsystem.movieCardContainer
import com.example.moviecatalog.core.navigation.UserAvatarBadge
import com.example.moviecatalog.data.local.AppFontScale
import com.example.moviecatalog.data.local.AppLanguage
import com.example.moviecatalog.data.local.AppThemeMode
import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.UserProfile
import com.example.moviecatalog.ui.theme.MovieCatalogTheme

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileContent(
        state = state,
        onLanguageSelected = viewModel::onLanguageSelected,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onFontScaleSelected = viewModel::onFontScaleSelected,
        onMovieClick = viewModel::onMovieClick,
        onRemoveFavorite = viewModel::onRemoveFavorite,
        onSignOutClick = viewModel::onSignOutClick,
        onBackClick = viewModel::onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onFontScaleSelected: (AppFontScale) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onRemoveFavorite: (Movie) -> Unit,
    onSignOutClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var languageMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "profile_header") {
            if (state.user != null) {
                AccountHeaderCard(
                    user = state.user,
                    favoritesCount = state.favoriteMovies.size,
                    createdMoviesCount = state.createdMoviesCount,
                    onSignOutClick = onSignOutClick
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(R.string.profile_not_signed_in),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item(key = "profile_preferences") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .movieCardContainer()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.profile_section_preferences),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.profile_language_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.profile_language_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.small
                                )
                                .clickable { languageMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${state.currentLanguage.displayName} (${state.currentLanguage.code})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = { languageMenuExpanded = false }
                        ) {
                            AppLanguage.entries.forEach { language ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${language.displayName} (${language.code})")
                                    },
                                    trailingIcon = {
                                        if (language == state.currentLanguage) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    onClick = {
                                        languageMenuExpanded = false
                                        onLanguageSelected(language)
                                    }
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppLanguage.entries.forEach { language ->
                            FilterChip(
                                selected = language == state.currentLanguage,
                                onClick = { onLanguageSelected(language) },
                                leadingIcon = if (language == state.currentLanguage) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                label = {
                                    Text(
                                        text = "${language.displayName} (${language.code})",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.profile_theme_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            val labelRes = when (mode) {
                                AppThemeMode.SYSTEM -> R.string.profile_theme_system
                                AppThemeMode.LIGHT -> R.string.profile_theme_light
                                AppThemeMode.DARK -> R.string.profile_theme_dark
                            }
                            FilterChip(
                                selected = mode == state.themeMode,
                                onClick = { onThemeModeSelected(mode) },
                                leadingIcon = if (mode == state.themeMode) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.profile_font_scale_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppFontScale.entries.forEach { scale ->
                            val labelRes = when (scale) {
                                AppFontScale.SMALL -> R.string.profile_font_small
                                AppFontScale.NORMAL -> R.string.profile_font_normal
                                AppFontScale.LARGE -> R.string.profile_font_large
                            }
                            FilterChip(
                                selected = scale == state.fontScale,
                                onClick = { onFontScaleSelected(scale) },
                                leadingIcon = if (scale == state.fontScale) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (state.user != null) {
            item(key = "favorites_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.profile_favorites_section, state.favoriteMovies.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (state.favoriteMovies.isEmpty()) {
                item(key = "favorites_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .movieCardContainer()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.profile_empty_favorites),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = state.favoriteMovies,
                    key = { it.id }
                ) { movie ->
                    SwipeableMovieCard(
                        movie = movie,
                        swipeEnabled = true,
                        compact = true,
                        isFavorite = true,
                        isFavoritesMode = true,
                        onCardClick = { onMovieClick(movie) },
                        onSwipeAction = onRemoveFavorite,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        item(key = "profile_back") {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.back_to_list),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountHeaderCard(
    user: UserProfile,
    favoritesCount: Int,
    createdMoviesCount: Int,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .movieCardContainer(isHighlighted = true)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_section_account),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            UserAvatarBadge(
                name = user.name,
                avatarUrl = user.avatarUrl,
                size = 68.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.profile_user_id_format, user.userId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_stats_format, favoritesCount, createdMoviesCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.metadataBadge(MaterialTheme.colorScheme.primaryContainer)
            )

            user.tokenRefreshedAt?.let { timestamp ->
                Text(
                    text = stringResource(R.string.token_refreshed_badge, timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.metadataBadge(MaterialTheme.colorScheme.tertiaryContainer)
                )
            }
        }

        Button(
            onClick = onSignOutClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.sign_out),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val sampleProfileUser = UserProfile(
    userId = "google_10482910",
    name = "Alisa Shevchenko",
    email = "alisa.shevchenko@gmail.com",
    avatarUrl = null,
    tokenRefreshedAt = "14:40:00"
)

private val sampleProfileMovies = listOf(
    Movie(
        id = "tt0468569",
        title = "The Dark Knight",
        year = 2008,
        rating = 9.0,
        posterUrl = null,
        plot = "When the menace known as the Joker wreaks havoc on Gotham.",
        genres = listOf("Action", "Crime", "Drama")
    )
)

@Preview(showBackground = true, locale = "en")
@Composable
private fun ProfileScreenEnglishPreview() {
    MovieCatalogTheme {
        ProfileContent(
            state = ProfileUiState(
                user = sampleProfileUser,
                favoriteMovies = sampleProfileMovies,
                createdMoviesCount = 2,
                currentLanguage = AppLanguage.EN,
                themeMode = AppThemeMode.SYSTEM,
                fontScale = AppFontScale.NORMAL
            ),
            onLanguageSelected = {},
            onThemeModeSelected = {},
            onFontScaleSelected = {},
            onMovieClick = {},
            onRemoveFavorite = {},
            onSignOutClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "uk")
@Composable
private fun ProfileScreenUkrainianPreview() {
    MovieCatalogTheme {
        ProfileContent(
            state = ProfileUiState(
                user = sampleProfileUser,
                favoriteMovies = sampleProfileMovies,
                createdMoviesCount = 2,
                currentLanguage = AppLanguage.UK,
                themeMode = AppThemeMode.LIGHT,
                fontScale = AppFontScale.NORMAL
            ),
            onLanguageSelected = {},
            onThemeModeSelected = {},
            onFontScaleSelected = {},
            onMovieClick = {},
            onRemoveFavorite = {},
            onSignOutClick = {},
            onBackClick = {}
        )
    }
}
