package com.example.moviecatalog.core.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moviecatalog.MovieCatalogApp
import com.example.moviecatalog.R
import com.example.moviecatalog.feature.catalog.CatalogScreen
import com.example.moviecatalog.feature.details.DetailsScreen
import com.example.moviecatalog.feature.genre.GenreMoviesScreen

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
                NavigationCommand.Back -> {
                    navController.popBackStack()
                }
            }
        }
    }

    val currentGenre = Uri.decode(backStackEntry?.arguments?.getString("genre").orEmpty())
    val showBackButton = currentRoute != null && currentRoute != Routes.CATALOG
    val screenTitle = when (currentRoute) {
        Routes.DETAILS -> stringResource(R.string.details_title)
        Routes.GENRE -> stringResource(R.string.genre_title_format, currentGenre)
        else -> stringResource(R.string.catalog_title)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CATALOG,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.CATALOG) {
                CatalogScreen()
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
        }
    }
}
