package com.example.moviecatalog.core.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.moviecatalog.MovieCatalogApp
import com.example.moviecatalog.R
import com.example.moviecatalog.data.auth.AuthService
import com.example.moviecatalog.data.auth.GoogleAccountOption
import com.example.moviecatalog.feature.catalog.CatalogScreen
import com.example.moviecatalog.feature.create.CreateMovieScreen
import com.example.moviecatalog.feature.details.DetailsScreen
import com.example.moviecatalog.feature.genre.GenreMoviesScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieCatalogNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val app = LocalContext.current.applicationContext as MovieCatalogApp
    val navigationService = app.navigationService
    val authService = app.authService
    val currentUser by authService.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showSignInDialog by remember { mutableStateOf(false) }

    LaunchedEffect(navigationService, navController) {
        navigationService.commands.collect { command ->
            when (command) {
                is NavigationCommand.ToDetails -> {
                    navController.navigate(Routes.details(command.movieId)) {
                        launchSingleTop = true
                    }
                }
                is NavigationCommand.ToGenre -> {
                    navController.navigate(Routes.genre(command.genre)) {
                        launchSingleTop = true
                    }
                }
                NavigationCommand.ToCreateMovie -> {
                    navController.navigate(Routes.CREATE_MOVIE) {
                        launchSingleTop = true
                    }
                }
                NavigationCommand.Back -> {
                    navController.popBackStack()
                }
            }
        }
    }

    if (showSignInDialog) {
        GoogleSignInDialog(
            accounts = authService.availableGoogleAccounts,
            onAccountSelected = { account ->
                showSignInDialog = false
                scope.launch {
                    authService.signInWithGoogleAccount(account)
                }
            },
            onOpenGoogleBrowser = { clientId ->
                showSignInDialog = false
                authService.openGoogleAuthInBrowser(clientId)
            },
            onDismiss = { showSignInDialog = false }
        )
    }

    val currentGenre = Uri.decode(backStackEntry?.arguments?.getString("genre").orEmpty())
    val showBackButton = currentRoute != null && currentRoute != Routes.CATALOG
    val screenTitle = when (currentRoute) {
        Routes.DETAILS -> stringResource(R.string.details_title)
        Routes.GENRE -> stringResource(R.string.genre_title_format, currentGenre)
        Routes.CREATE_MOVIE -> stringResource(R.string.create_movie_title)
        else -> stringResource(R.string.catalog_title)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navigationService.navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_to_list)
                            )
                        }
                    }
                },
                actions = {
                    val user = currentUser
                    if (user == null) {
                        FilledTonalButton(
                            onClick = { showSignInDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.sign_in),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            UserAvatarBadge(
                                name = user.name,
                                avatarUrl = user.avatarUrl,
                                size = 30.dp
                            )
                            Text(
                                text = user.name.substringBefore(' '),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 86.dp)
                            )
                            TextButton(
                                onClick = {
                                    scope.launch { authService.signOut() }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.sign_out),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CATALOG,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(260)) +
                    slideInHorizontally(animationSpec = tween(260)) { fullWidth -> fullWidth / 5 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) +
                    slideOutHorizontally(animationSpec = tween(200)) { fullWidth -> -fullWidth / 8 }
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(240)) +
                    slideInHorizontally(animationSpec = tween(240)) { fullWidth -> -fullWidth / 8 }
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(240)) +
                    slideOutHorizontally(animationSpec = tween(240)) { fullWidth -> fullWidth / 5 }
            }
        ) {
            composable(Routes.CATALOG) {
                CatalogScreen(onSignInClick = { showSignInDialog = true })
            }
            composable(
                route = Routes.DETAILS,
                arguments = listOf(navArgument("movieId") { type = NavType.StringType })
            ) { entry ->
                val movieId = Uri.decode(entry.arguments?.getString("movieId").orEmpty())
                DetailsScreen(movieId = movieId)
            }
            composable(
                route = Routes.GENRE,
                arguments = listOf(navArgument("genre") { type = NavType.StringType })
            ) { entry ->
                val genre = Uri.decode(entry.arguments?.getString("genre").orEmpty())
                GenreMoviesScreen(genre = genre)
            }
            composable(Routes.CREATE_MOVIE) {
                CreateMovieScreen()
            }
        }
    }
}

@Composable
fun UserAvatarBadge(
    name: String,
    avatarUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initials = name.split(' ')
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifBlank { "G" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun GoogleSignInDialog(
    accounts: List<GoogleAccountOption>,
    onAccountSelected: (GoogleAccountOption) -> Unit,
    onOpenGoogleBrowser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var clientId by remember { mutableStateOf(AuthService.DEFAULT_GOOGLE_CLIENT_ID) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sign_in_with_google)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.sign_in_dialog_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                accounts.forEach { account ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onAccountSelected(account) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            UserAvatarBadge(
                                name = account.name,
                                avatarUrl = account.avatarUrl,
                                size = 36.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = account.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text(stringResource(R.string.oauth_client_id_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { onOpenGoogleBrowser(clientId) },
                    enabled = clientId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_google_browser))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAccountSelected(accounts.first()) }) {
                Text(stringResource(R.string.sign_in))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
