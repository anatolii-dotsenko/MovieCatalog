package com.example.moviecatalog.domain.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * Mutable-friendly form DTO with string-to-number converters (Compose analog of MAUI DTO + IValueConverter).
 */
@Immutable
data class MovieCreateDto(
    val title: String = "",
    val originalTitle: String = "",
    val yearText: String = "",
    val runtimeText: String = "",
    val ratingText: String = "",
    val genresText: String = "",
    val director: String = "",
    val castText: String = "",
    val country: String = "",
    val plot: String = "",
    val posterUri: String? = null
) {
    fun parsedYear(): Int? = yearText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

    fun parsedRuntime(): Int? = runtimeText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()

    fun parsedRating(): Double? =
        ratingText.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

    fun parsedGenres(): List<String> =
        genresText.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun parsedCast(): List<String> =
        castText.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun toMovie(): Movie = Movie(
        id = "user_${UUID.randomUUID()}",
        title = title.trim(),
        originalTitle = originalTitle.trim().takeIf { it.isNotEmpty() },
        year = parsedYear(),
        rating = parsedRating(),
        posterUrl = posterUri,
        plot = plot.trim().ifBlank { "Фільм створено користувачем." },
        genres = parsedGenres(),
        runtimeMinutes = parsedRuntime(),
        director = director.trim().takeIf { it.isNotEmpty() },
        cast = parsedCast(),
        country = country.trim().takeIf { it.isNotEmpty() },
        isUserCreated = true
    )
}
