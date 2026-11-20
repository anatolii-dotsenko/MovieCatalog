package com.example.moviecatalog.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.moviecatalog.domain.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.UUID

interface IStorageService {
    suspend fun copyPosterToAppData(sourceUriString: String): String?
    suspend fun deletePosterFromAppData(posterUrl: String?)
    suspend fun saveCreatedMovies(movies: List<Movie>)
    suspend fun loadCreatedMovies(): List<Movie>
    suspend fun saveFavoriteMovies(movies: List<Movie>)
    suspend fun loadFavoriteMovies(): List<Movie>
}

/**
 * Local file storage service managing camelCase JSON files and copied posters inside AppData (filesDir).
 */
class StorageService(
    private val context: Context
) : IStorageService {

    private val postersDir: File
        get() = File(context.filesDir, POSTERS_DIR_NAME).apply { mkdirs() }

    private val createdMoviesFile: File
        get() = File(context.filesDir, CREATED_MOVIES_FILE)

    private val favoriteMoviesFile: File
        get() = File(context.filesDir, FAVORITE_MOVIES_FILE)

    override suspend fun copyPosterToAppData(sourceUriString: String): String? = withContext(Dispatchers.IO) {
        if (sourceUriString.isBlank()) return@withContext null

        val uri = Uri.parse(sourceUriString)
        if (uri.scheme == "file") {
            val path = uri.path ?: return@withContext null
            val existingFile = File(path)
            if (existingFile.canonicalPath.startsWith(postersDir.canonicalPath)) {
                return@withContext sourceUriString
            }
        }

        runCatching {
            val originalName = resolveFileName(uri)
            val sanitizedName = originalName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val fileNameWithExt = if (sanitizedName.contains('.')) sanitizedName else "$sanitizedName.jpg"
            val destFile = File(postersDir, "${UUID.randomUUID()}_$fileNameWithExt")

            val inputStream = if (uri.scheme == "file") {
                val path = uri.path ?: return@runCatching null
                FileInputStream(File(path))
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return@runCatching null

            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.toURI().toString()
        }.getOrNull()
    }

    override suspend fun deletePosterFromAppData(posterUrl: String?) {
        withContext(Dispatchers.IO) {
            if (posterUrl.isNullOrBlank()) return@withContext
            val uri = Uri.parse(posterUrl)
            if (uri.scheme != "file") return@withContext
            val path = uri.path ?: return@withContext
            val file = File(path)
            if (file.canonicalPath.startsWith(postersDir.canonicalPath) && file.exists()) {
                file.delete()
            }
        }
    }

    override suspend fun saveCreatedMovies(movies: List<Movie>) {
        saveMoviesList(createdMoviesFile, movies)
    }

    override suspend fun loadCreatedMovies(): List<Movie> = loadMoviesList(createdMoviesFile)

    override suspend fun saveFavoriteMovies(movies: List<Movie>) {
        saveMoviesList(favoriteMoviesFile, movies)
    }

    override suspend fun loadFavoriteMovies(): List<Movie> = loadMoviesList(favoriteMoviesFile)

    private suspend fun saveMoviesList(targetFile: File, movies: List<Movie>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        movies.forEach { movie ->
            array.put(movieToJson(movie))
        }
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(array.toString(2))
    }

    private suspend fun loadMoviesList(sourceFile: File): List<Movie> = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext emptyList()
        runCatching {
            val content = sourceFile.readText()
            if (content.isBlank()) return@runCatching emptyList()
            val array = JSONArray(content)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    jsonToMovie(obj)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun resolveFileName(uri: Uri): String {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val name = cursor.getString(idx)
                            if (!name.isNullOrBlank()) return name
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "poster.jpg"
    }

    private fun movieToJson(movie: Movie): JSONObject = JSONObject().apply {
        put("id", movie.id)
        put("title", movie.title)
        put("year", movie.year ?: JSONObject.NULL)
        put("rating", movie.rating ?: JSONObject.NULL)
        put("posterUrl", movie.posterUrl ?: JSONObject.NULL)
        put("plot", movie.plot)
        put("genres", JSONArray(movie.genres))
        put("originalTitle", movie.originalTitle ?: JSONObject.NULL)
        put("runtimeMinutes", movie.runtimeMinutes ?: JSONObject.NULL)
        put("voteCount", movie.voteCount ?: JSONObject.NULL)
        put("director", movie.director ?: JSONObject.NULL)
        put("cast", JSONArray(movie.cast))
        put("country", movie.country ?: JSONObject.NULL)
        put("isUserCreated", movie.isUserCreated)
    }

    private fun jsonToMovie(obj: JSONObject): Movie? {
        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return null
        val title = obj.optString("title").takeIf { it.isNotBlank() } ?: return null
        return Movie(
            id = id,
            title = title,
            year = obj.optNullableInt("year"),
            rating = obj.optNullableDouble("rating"),
            posterUrl = obj.optNullableString("posterUrl"),
            plot = obj.optString("plot", ""),
            genres = obj.optStringList("genres"),
            originalTitle = obj.optNullableString("originalTitle"),
            runtimeMinutes = obj.optNullableInt("runtimeMinutes"),
            voteCount = obj.optNullableInt("voteCount"),
            director = obj.optNullableString("director"),
            cast = obj.optStringList("cast"),
            country = obj.optNullableString("country"),
            isUserCreated = obj.optBoolean("isUserCreated", false)
        )
    }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    private fun JSONObject.optStringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) add(item)
            }
        }
    }

    private companion object {
        const val POSTERS_DIR_NAME = "Posters"
        const val CREATED_MOVIES_FILE = "created_movies.json"
        const val FAVORITE_MOVIES_FILE = "favorite_movies.json"
    }
}
