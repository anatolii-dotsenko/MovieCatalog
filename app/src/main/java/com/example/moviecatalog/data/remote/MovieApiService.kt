package com.example.moviecatalog.data.remote

import com.example.moviecatalog.domain.model.Movie
import com.example.moviecatalog.domain.model.MoviePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface IMovieApiService {
    suspend fun fetchMoviesPage(pageToken: String? = null): MoviePage
}

class MovieApiService : IMovieApiService {

    override suspend fun fetchMoviesPage(pageToken: String?): MoviePage = withContext(Dispatchers.IO) {
        if (pageToken != null && pageToken.startsWith(FALLBACK_TOKEN_PREFIX)) {
            delay(350)
            return@withContext getFallbackPage(pageToken)
        }

        val remoteResult = runCatching {
            val baseUrl = "https://api.imdbapi.dev/titles"
            val requestUrl = if (pageToken.isNullOrBlank()) {
                baseUrl
            } else {
                "$baseUrl?pageToken=${URLEncoder.encode(pageToken, "UTF-8")}"
            }

            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MovieCatalog-Android/1.0")
            }

            try {
                if (connection.responseCode !in 200..299) {
                    error("HTTP ${connection.responseCode}")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseTitlesResponse(body)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        if (remoteResult != null && remoteResult.movies.isNotEmpty()) {
            remoteResult
        } else if (pageToken == null) {
            getFallbackPage("${FALLBACK_TOKEN_PREFIX}1")
        } else {
            MoviePage(movies = emptyList(), nextPageToken = null)
        }
    }

    private fun parseTitlesResponse(jsonBody: String): MoviePage {
        val root = JSONObject(jsonBody)
        val titlesArray = root.optJSONArray("titles") ?: return MoviePage(emptyList(), null)
        val movies = buildList {
            for (i in 0 until titlesArray.length()) {
                val obj = titlesArray.optJSONObject(i) ?: continue
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                val title = obj.optString("primaryTitle")
                    .ifBlank { obj.optString("originalTitle", "Untitled") }
                val year = if (obj.has("startYear") && !obj.isNull("startYear")) {
                    obj.optInt("startYear").takeIf { it > 0 }
                } else {
                    null
                }
                val ratingObj = obj.optJSONObject("rating")
                val rating = if (ratingObj != null && ratingObj.has("aggregateRating") && !ratingObj.isNull("aggregateRating")) {
                    ratingObj.optDouble("aggregateRating").takeIf { !it.isNaN() && it > 0.0 }
                } else {
                    null
                }
                val posterUrl = obj.optJSONObject("primaryImage")
                    ?.optString("url")
                    ?.takeIf { it.isNotBlank() }
                val plot = obj.optString("plot")
                    .ifBlank { "Короткий опис для цього фільму наразі відсутній." }
                val genres = buildList {
                    val genresArr = obj.optJSONArray("genres")
                    if (genresArr != null) {
                        for (g in 0 until genresArr.length()) {
                            val genre = genresArr.optString(g)
                            if (genre.isNotBlank()) add(genre)
                        }
                    }
                }
                add(
                    Movie(
                        id = id,
                        title = title,
                        year = year,
                        rating = rating,
                        posterUrl = posterUrl,
                        plot = plot,
                        genres = genres
                    )
                )
            }
        }
        val nextToken = root.optString("nextPageToken")
            .ifBlank { root.optString("pageToken") }
            .ifBlank { null }

        return MoviePage(movies = movies, nextPageToken = nextToken)
    }

    private fun getFallbackPage(token: String): MoviePage {
        val pageIndex = token.removePrefix(FALLBACK_TOKEN_PREFIX).toIntOrNull() ?: 1
        val pageMovies = fallbackPages.getOrNull(pageIndex - 1).orEmpty()
        val nextToken = if (pageIndex < fallbackPages.size) {
            "$FALLBACK_TOKEN_PREFIX${pageIndex + 1}"
        } else {
            null
        }
        return MoviePage(movies = pageMovies, nextPageToken = nextToken)
    }

    private companion object {
        const val FALLBACK_TOKEN_PREFIX = "page_"

        val fallbackPages: List<List<Movie>> = listOf(
            listOf(
                Movie(
                    id = "tt0111161",
                    title = "The Shawshank Redemption",
                    year = 1994,
                    rating = 9.3,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMDAyY2FhYjctNDc5OS00MDNlLThiMGUtY2UxYWVkNGY2ZjljXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "Over the course of several years, two convicts form a friendship, seeking consolation and, eventually, redemption through basic compassion.",
                    genres = listOf("Drama")
                ),
                Movie(
                    id = "tt0068646",
                    title = "The Godfather",
                    year = 1972,
                    rating = 9.2,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNGEwYjgwOGQtYjg5ZS00Njc1LTk2ZGEtM2QwZWQ2NjdhZTE5XkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant youngest son.",
                    genres = listOf("Crime", "Drama")
                ),
                Movie(
                    id = "tt0468569",
                    title = "The Dark Knight",
                    year = 2008,
                    rating = 9.0,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMTMxNTMwODM0NF5BMl5BanBnXkFtZTcwODAyMTk2Mw@@._V1_FMjpg_UX500_.jpg",
                    plot = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological tests.",
                    genres = listOf("Action", "Crime", "Drama")
                ),
                Movie(
                    id = "tt0071562",
                    title = "The Godfather Part II",
                    year = 1974,
                    rating = 9.0,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMDIxMzBlZDktZjMxNy00ZGI4LTgxNDEtYWRlNzRjMjJmOGQ5XkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "The early life and career of Vito Corleone in 1920s New York City is portrayed, while his son Michael expands his grip on the family syndicate.",
                    genres = listOf("Crime", "Drama")
                ),
                Movie(
                    id = "tt0050083",
                    title = "12 Angry Men",
                    year = 1957,
                    rating = 9.0,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYjE4NzdmOTYtYjc5Yi00YzBiLWEzNDEtNTgxZGQ2MWVkN2NiXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "The jury in a New York City murder trial is frustrated by a single member whose skeptical caution forces them to carefully consider the evidence.",
                    genres = listOf("Crime", "Drama")
                ),
                Movie(
                    id = "tt0108052",
                    title = "Schindler's List",
                    year = 1993,
                    rating = 9.0,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNjM1ZDQxYWUtMzQyZS00MTE1LWJmZGYtNGUyNTdlYjM3ZmVmXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "In German-occupied Poland during World War II, industrialist Oskar Schindler gradually becomes concerned for his Jewish workforce.",
                    genres = listOf("Biography", "Drama", "History")
                ),
                Movie(
                    id = "tt0167260",
                    title = "The Lord of the Rings: The Return of the King",
                    year = 2003,
                    rating = 9.0,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMTZkMjBjNWMtZGI5OC00MGU0LTk4ZTItODg2NWM3NTVmNWQ4XkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "Gandalf and Aragorn lead the World of Men against Sauron's army to draw his gaze from Frodo and Sam as they approach Mount Doom.",
                    genres = listOf("Adventure", "Drama", "Fantasy")
                ),
                Movie(
                    id = "tt0110912",
                    title = "Pulp Fiction",
                    year = 1994,
                    rating = 8.9,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYTViYTE3ZGQtNDBlMC00ZTAyLTkyODMtZGRiZDg0MjA2YThkXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "The lives of two mob hitmen, a boxer, a gangster and his wife, and a pair of diner bandits intertwine in four tales of violence and redemption.",
                    genres = listOf("Crime", "Drama")
                )
            ),
            listOf(
                Movie(
                    id = "tt0120737",
                    title = "The Lord of the Rings: The Fellowship of the Ring",
                    year = 2001,
                    rating = 8.9,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNzIxMDQ2YTctNDY4MC00ZTRhLTk4ODQtMTVlOWY4NTdiYmMwXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "A meek Hobbit from the Shire and eight companions set out on a journey to destroy the powerful One Ring and save Middle-earth.",
                    genres = listOf("Adventure", "Drama", "Fantasy")
                ),
                Movie(
                    id = "tt0060196",
                    title = "The Good, the Bad and the Ugly",
                    year = 1966,
                    rating = 8.8,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMWM5ZjQxM2YtNDlmYi00ZDNhLWI4MWUtN2VkYjBlMTY1ZTkwXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "A bounty hunting scam joins two men in an uneasy alliance against a third in a race to find a fortune in gold buried in a remote cemetery.",
                    genres = listOf("Adventure", "Western")
                ),
                Movie(
                    id = "tt0109830",
                    title = "Forrest Gump",
                    year = 1994,
                    rating = 8.8,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNDYwNzVjMTItZmU5YS00YjQ5LTljYjgtMjY2NDVmYWMyNWFmXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "The history of the United States from the 1950s to the '70s unfolds from the perspective of an Alabama man with an IQ of 75.",
                    genres = listOf("Drama", "Romance")
                ),
                Movie(
                    id = "tt0137523",
                    title = "Fight Club",
                    year = 1999,
                    rating = 8.8,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BOTgyOGQ1NDItNGU3Ny00MjU3LTg2YWEtNmEyYjBiMjI1Y2M5XkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "An insomniac office worker and a devil-may-care soap maker form an underground fight club that evolves into much more.",
                    genres = listOf("Drama")
                ),
                Movie(
                    id = "tt0167261",
                    title = "The Lord of the Rings: The Two Towers",
                    year = 2002,
                    rating = 8.8,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMGQxMDdiOWUtYjc1Ni00YzM1LWE2NjMtZTg3Y2JkMjEzMTJjXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "While Frodo and Sam edge closer to Mordor with the help of the shifty Gollum, the divided fellowship makes a stand against Sauron's new ally.",
                    genres = listOf("Adventure", "Drama", "Fantasy")
                ),
                Movie(
                    id = "tt1375666",
                    title = "Inception",
                    year = 2010,
                    rating = 8.8,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_FMjpg_UX500_.jpg",
                    plot = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea.",
                    genres = listOf("Action", "Adventure", "Sci-Fi")
                ),
                Movie(
                    id = "tt0080684",
                    title = "Star Wars: Episode V - The Empire Strikes Back",
                    year = 1980,
                    rating = 8.7,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMTkxNGFlNDktZmJkNC00MDdhLTg0MTEtZjZiYWI3MGE5NWIwXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "After the Rebels are overpowered by the Empire, Luke Skywalker begins his Jedi training with Yoda while his friends are pursued by Darth Vader.",
                    genres = listOf("Action", "Adventure", "Fantasy")
                ),
                Movie(
                    id = "tt0133093",
                    title = "The Matrix",
                    year = 1999,
                    rating = 8.7,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BN2NmN2VhMTQtMDNiOS00NDlhLTliMjgtODE2ZTY0ODQyNDRhXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "When a beautiful stranger leads computer hacker Neo to a forbidding underworld, he discovers the shocking truth about his reality.",
                    genres = listOf("Action", "Sci-Fi")
                )
            ),
            listOf(
                Movie(
                    id = "tt0099685",
                    title = "Goodfellas",
                    year = 1990,
                    rating = 8.7,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BN2E5NzI2ZGMtY2VjNi00YTRjLWI1MDUtZGY5OWU1MWJjZjRjXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "The story of Henry Hill and his life in the mafia, covering his relationship with his wife Karen and his mob partners.",
                    genres = listOf("Biography", "Crime", "Drama")
                ),
                Movie(
                    id = "tt0073486",
                    title = "One Flew Over the Cuckoo's Nest",
                    year = 1975,
                    rating = 8.7,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYjBkMjgzMzYtNzRiMS00NDc3LWE4YTUtZjYxYjZhNjNhYzhhXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "In the Fall of 1963, a Korean War veteran and criminal pleads insanity and is admitted to a mental institution, where he rallies the patients.",
                    genres = listOf("Drama")
                ),
                Movie(
                    id = "tt0816692",
                    title = "Interstellar",
                    year = 2014,
                    rating = 8.7,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYzdjMDAxZGItMjI2My00ODA1LTlkNzItOWFjMDU5ZDJlYWY3XkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot is tasked to pilot a spacecraft through a wormhole.",
                    genres = listOf("Adventure", "Drama", "Sci-Fi")
                ),
                Movie(
                    id = "tt0114369",
                    title = "Se7en",
                    year = 1995,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BY2IzNzMxZjctZjUxZi00YzAxLTk3ZjMtODFjODdhMDU5NDM1XkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "Two detectives, a rookie and a veteran, hunt a serial killer who uses the seven deadly sins as his motives.",
                    genres = listOf("Crime", "Drama", "Mystery")
                ),
                Movie(
                    id = "tt0102926",
                    title = "The Silence of the Lambs",
                    year = 1991,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNDdhOGJhYzctYzYwZC00YmI2LWI0MjctYjg4ODdlMDExYjBlXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "A young F.B.I. cadet must receive the help of an incarcerated and manipulative cannibal killer to help catch another serial killer.",
                    genres = listOf("Crime", "Drama", "Thriller")
                ),
                Movie(
                    id = "tt0120815",
                    title = "Saving Private Ryan",
                    year = 1998,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BZGZhZGQ1ZWUtZTZjYS00MDJhLWFkYjctN2ZlYjE5NWZmZDM2XkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "Following the Normandy Landings, a group of U.S. soldiers go behind enemy lines to retrieve a paratrooper whose brothers have been killed.",
                    genres = listOf("Drama", "War")
                ),
                Movie(
                    id = "tt0317248",
                    title = "City of God",
                    year = 2002,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYjY4NGI5OTUtY2ZlZS00Zjk4LTk5N2MtN2JmYWVjNGU1NGIwXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "In the slums of Rio, two kids' paths diverge as one struggles to become a photographer and the other a kingpin.",
                    genres = listOf("Crime", "Drama")
                ),
                Movie(
                    id = "tt0118799",
                    title = "Life Is Beautiful",
                    year = 1997,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BZTBhOGYzZjQtY2Y5MS00Njg4LTk3NWMtNzYwZTg1ODZkYjJiXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "When an open-minded Jewish waiter and his son become victims of the Holocaust, he uses a mixture of will, humor, and imagination.",
                    genres = listOf("Comedy", "Drama", "Romance")
                )
            ),
            listOf(
                Movie(
                    id = "tt0120689",
                    title = "The Green Mile",
                    year = 1999,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMTUxMzQyNjA5MF5BMl5BanBnXkFtZTYwOTU2NTY3._V1_FMjpg_UX500_.jpg",
                    plot = "A death row guard learns that a gentle giant in his charge possesses a mysterious gift.",
                    genres = listOf("Crime", "Drama", "Fantasy")
                ),
                Movie(
                    id = "tt0076759",
                    title = "Star Wars: Episode IV - A New Hope",
                    year = 1977,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BOGUwMDk0Y2MtNjBlNi00NmRiLTk2MWYtMGMyMDlhYmI4ZDBjXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "Luke Skywalker joins forces with a Jedi Knight, a cocky pilot, a Wookiee and two droids to save the galaxy from the Empire.",
                    genres = listOf("Action", "Adventure", "Fantasy")
                ),
                Movie(
                    id = "tt0103064",
                    title = "Terminator 2: Judgment Day",
                    year = 1991,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNGMyMGNkMDUtMjc2Ni00NWFlLTgyODEtZTY2MzBiZTg0OWZiXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "A cyborg, identical to the one who failed to kill Sarah Connor, must now protect her ten-year-old son John from an even more advanced cyborg.",
                    genres = listOf("Action", "Adventure", "Sci-Fi")
                ),
                Movie(
                    id = "tt0245429",
                    title = "Spirited Away",
                    year = 2001,
                    rating = 8.6,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BNTEyNmEwOWUtYzkyOC00ZTQ4LTllZmUtMjk0Y2YwOGUzYjRiXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "During her family's move to the suburbs, a sullen 10-year-old girl wanders into a world ruled by gods, witches, and spirits.",
                    genres = listOf("Animation", "Adventure", "Family")
                ),
                Movie(
                    id = "tt0088763",
                    title = "Back to the Future",
                    year = 1985,
                    rating = 8.5,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BZmM3ZjE0NzctNjBiOC00MDZmLTgzMTUtNGVlOWFlOTRmZTMxXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "Marty McFly, a 17-year-old high school student, is accidentally sent 30 years into the past in a time-traveling DeLorean.",
                    genres = listOf("Adventure", "Comedy", "Sci-Fi")
                ),
                Movie(
                    id = "tt0253474",
                    title = "The Pianist",
                    year = 2002,
                    rating = 8.5,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BMjEwNmEwYjgtNTk3ZC00NjljLTg5ZDctZTY3ZGQwZjRkZmQxXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "During WWII, acclaimed Polish musician Wladyslaw faces various struggles as he loses contact with his family.",
                    genres = listOf("Biography", "Drama", "Music")
                ),
                Movie(
                    id = "tt6751668",
                    title = "Parasite",
                    year = 2019,
                    rating = 8.5,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYjk1Y2U4MjQtY2ZiNS00OWQyLWI3MmYtZWUwNmRjYWRiNWNhXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan.",
                    genres = listOf("Drama", "Thriller")
                ),
                Movie(
                    id = "tt0172495",
                    title = "Gladiator",
                    year = 2000,
                    rating = 8.5,
                    posterUrl = "https://m.media-amazon.com/images/M/MV5BYWQ4YmNjYjEtZWExNC00NmY2LTkxZDQtY2I5MTQ4YzY3YzQyXkEyXkFqcGc@._V1_FMjpg_UX500_.jpg",
                    plot = "A former Roman General sets out to exact vengeance against the corrupt emperor who murdered his family and sent him into slavery.",
                    genres = listOf("Action", "Adventure", "Drama")
                )
            )
        )
    }
}
