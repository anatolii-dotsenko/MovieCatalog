package com.example.moviecatalog.core.navigation

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object Routes {
    const val CATALOG = "catalog"
    const val DETAILS = "details/{movieId}"
    const val GENRE = "genre/{genre}"
    const val CREATE_MOVIE = "create_movie"
    const val PROFILE = "profile"

    fun details(movieId: String): String = "details/${Uri.encode(movieId)}"
    fun genre(genre: String): String = "genre/${Uri.encode(genre)}"
}

sealed interface NavigationCommand {
    data class ToDetails(val movieId: String) : NavigationCommand
    data class ToGenre(val genre: String) : NavigationCommand
    data object ToCreateMovie : NavigationCommand
    data object ToProfile : NavigationCommand
    data object Back : NavigationCommand
}

interface INavigationService {
    val commands: SharedFlow<NavigationCommand>
    fun navigateToDetails(movieId: String)
    fun navigateToGenre(genre: String)
    fun navigateToCreateMovie()
    fun navigateToProfile()
    fun navigateBack()
}

/**
 * Centralized navigation service decoupling ViewModels from NavController.
 */
class NavigationService : INavigationService {

    private val _commands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 16)
    override val commands: SharedFlow<NavigationCommand> = _commands.asSharedFlow()

    override fun navigateToDetails(movieId: String) {
        _commands.tryEmit(NavigationCommand.ToDetails(movieId))
    }

    override fun navigateToGenre(genre: String) {
        _commands.tryEmit(NavigationCommand.ToGenre(genre))
    }

    override fun navigateToCreateMovie() {
        _commands.tryEmit(NavigationCommand.ToCreateMovie)
    }

    override fun navigateToProfile() {
        _commands.tryEmit(NavigationCommand.ToProfile)
    }

    override fun navigateBack() {
        _commands.tryEmit(NavigationCommand.Back)
    }
}
