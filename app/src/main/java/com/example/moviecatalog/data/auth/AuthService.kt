package com.example.moviecatalog.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.example.moviecatalog.data.local.IPreferencesStorage
import com.example.moviecatalog.data.local.ISecureStorage
import com.example.moviecatalog.domain.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class GoogleAccountOption(
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String
)

interface IAuthService {
    val currentUser: StateFlow<UserProfile?>
    val authStatusMessage: StateFlow<String?>
    val availableGoogleAccounts: List<GoogleAccountOption>

    fun buildAuthorizationUrl(clientId: String): String
    fun openGoogleAuthInBrowser(clientId: String)
    suspend fun handleOAuthRedirect(uri: Uri)
    suspend fun signInWithGoogleAccount(account: GoogleAccountOption)
    suspend fun restoreSessionWithRefreshToken()
    suspend fun signOut()
}

/**
 * Handles Google OAuth2 authorization code flow with PKCE, JWT id_token parsing,
 * refresh-token session restoration, SecureStorage for tokens, and Preferences for profile fields.
 */
class AuthService(
    private val context: Context,
    private val preferencesStorage: IPreferencesStorage,
    private val secureStorage: ISecureStorage
) : IAuthService {

    private val sessionMutex = Mutex()
    private var sessionRestored = false
    private var pendingCodeVerifier: String? = null
    private var pendingClientId: String? = null

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _authStatusMessage = MutableStateFlow<String?>(null)
    override val authStatusMessage: StateFlow<String?> = _authStatusMessage.asStateFlow()

    override val availableGoogleAccounts: List<GoogleAccountOption> = listOf(
        GoogleAccountOption(
            userId = "google_10482910",
            name = "Alisa Shevchenko",
            email = "alisa.shevchenko@gmail.com",
            avatarUrl = "https://ui-avatars.com/api/?name=Alisa+Shevchenko&background=1D3557&color=fff&size=256"
        ),
        GoogleAccountOption(
            userId = "google_20918472",
            name = "Maksym Kovalenko",
            email = "maksym.kovalenko@gmail.com",
            avatarUrl = "https://ui-avatars.com/api/?name=Maksym+Kovalenko&background=8C5D00&color=fff&size=256"
        )
    )

    override fun buildAuthorizationUrl(clientId: String): String {
        val trimmedClientId = clientId.trim().ifBlank { DEFAULT_GOOGLE_CLIENT_ID }
        val verifier = generateCodeVerifier()
        val challenge = computeCodeChallenge(verifier)
        pendingCodeVerifier = verifier
        pendingClientId = trimmedClientId

        return Uri.parse(GOOGLE_AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("client_id", trimmedClientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "openid email profile")
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    override fun openGoogleAuthInBrowser(clientId: String) {
        val url = buildAuthorizationUrl(clientId)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure { error ->
            _authStatusMessage.value = "Не вдалося відкрити браузер: ${error.localizedMessage}"
        }
    }

    override suspend fun handleOAuthRedirect(uri: Uri) {
        val isSupportedScheme = uri.scheme == PACKAGE_SCHEME || uri.scheme == REVERSE_CLIENT_SCHEME
        if (!isSupportedScheme) return
        val isOAuthPath = uri.path == "/oauth2redirect" || uri.host == "oauth2redirect"
        if (!isOAuthPath) return

        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            _authStatusMessage.value = "Помилка авторизації Google: $error"
            return
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) return

        val clientId = pendingClientId ?: preferencesStorage.get(KEY_OAUTH_CLIENT_ID) ?: DEFAULT_GOOGLE_CLIENT_ID
        val verifier = pendingCodeVerifier
        pendingCodeVerifier = null

        val tokenPayload = exchangeCodeForTokens(
            code = code,
            clientId = clientId,
            codeVerifier = verifier
        )

        if (tokenPayload != null) {
            completeSignInWithTokens(
                idToken = tokenPayload.idToken,
                accessToken = tokenPayload.accessToken,
                refreshToken = tokenPayload.refreshToken,
                clientId = clientId,
                statusNote = "Авторизовано через Google OAuth2 (${currentTimestamp()})"
            )
        } else {
            _authStatusMessage.value = "Не вдалося обміняти код авторизації на токени"
        }
    }

    override suspend fun signInWithGoogleAccount(account: GoogleAccountOption) {
        val idToken = buildJwtToken(account)
        val accessToken = "ya29.access.${UUID.randomUUID()}"
        val refreshToken = "1//refresh.${account.userId}.${UUID.randomUUID()}"
        completeSignInWithTokens(
            idToken = idToken,
            accessToken = accessToken,
            refreshToken = refreshToken,
            clientId = DEFAULT_DEMO_CLIENT_ID,
            statusNote = "Google Sign-In успішний (${currentTimestamp()})"
        )
    }

    override suspend fun restoreSessionWithRefreshToken() {
        sessionMutex.withLock {
            if (sessionRestored) return
            sessionRestored = true

            val savedRefreshToken = secureStorage.getAsync(KEY_REFRESH_TOKEN)
            if (savedRefreshToken.isNullOrBlank()) {
                _currentUser.value = null
                return
            }

            val savedClientId = secureStorage.getAsync(KEY_CLIENT_ID) ?: DEFAULT_DEMO_CLIENT_ID
            val refreshedTimestamp = currentTimestamp()

            if (savedClientId != DEFAULT_DEMO_CLIENT_ID) {
                val refreshed = refreshRemoteTokens(savedRefreshToken, savedClientId)
                if (refreshed != null) {
                    completeSignInWithTokens(
                        idToken = refreshed.idToken,
                        accessToken = refreshed.accessToken,
                        refreshToken = refreshed.refreshToken ?: savedRefreshToken,
                        clientId = savedClientId,
                        statusNote = refreshedTimestamp
                    )
                    return
                }
            }

            // Refresh tokens locally if using demo Google OAuth or if offline
            val savedIdToken = secureStorage.getAsync(KEY_ID_TOKEN)
            val parsedUser = savedIdToken?.let(::parseJwtIdToken)
            val fallbackId = secureStorage.getAsync(KEY_USER_ID)
            val fallbackName = preferencesStorage.get(KEY_USER_NAME)
            val fallbackEmail = preferencesStorage.get(KEY_USER_EMAIL)
            val fallbackAvatar = preferencesStorage.get(KEY_USER_AVATAR)

            val restoredUser = parsedUser ?: if (!fallbackId.isNullOrBlank() && !fallbackName.isNullOrBlank()) {
                UserProfile(
                    userId = fallbackId,
                    name = fallbackName,
                    email = fallbackEmail.orEmpty(),
                    avatarUrl = fallbackAvatar
                )
            } else {
                null
            }

            if (restoredUser != null) {
                val rotatedAccessToken = "ya29.refreshed.${UUID.randomUUID()}"
                val rotatedRefreshToken = "1//refresh.${restoredUser.userId}.${UUID.randomUUID()}"
                secureStorage.setAsync(KEY_ACCESS_TOKEN, rotatedAccessToken)
                secureStorage.setAsync(KEY_REFRESH_TOKEN, rotatedRefreshToken)
                preferencesStorage.set(KEY_TOKEN_REFRESHED_AT, refreshedTimestamp)

                _currentUser.value = restoredUser.copy(tokenRefreshedAt = refreshedTimestamp)
                _authStatusMessage.value = refreshedTimestamp
            } else {
                clearStoredAuth()
            }
        }
    }

    override suspend fun signOut() {
        sessionMutex.withLock {
            clearStoredAuth()
            _currentUser.value = null
            _authStatusMessage.value = null
        }
    }

    private suspend fun completeSignInWithTokens(
        idToken: String,
        accessToken: String,
        refreshToken: String?,
        clientId: String,
        statusNote: String
    ) {
        val parsedProfile = parseJwtIdToken(idToken)
            ?: UserProfile(
                userId = "google_${UUID.randomUUID()}",
                name = "Google User",
                email = "user@gmail.com",
                avatarUrl = null
            )

        val timestamp = currentTimestamp()
        val finalProfile = parsedProfile.copy(tokenRefreshedAt = timestamp)

        // Sensitive tokens and internal user id -> SecureStorage (AndroidKeyStore AES-GCM)
        secureStorage.setAsync(KEY_ID_TOKEN, idToken)
        secureStorage.setAsync(KEY_ACCESS_TOKEN, accessToken)
        if (!refreshToken.isNullOrBlank()) {
            secureStorage.setAsync(KEY_REFRESH_TOKEN, refreshToken)
        }
        secureStorage.setAsync(KEY_USER_ID, finalProfile.userId)
        secureStorage.setAsync(KEY_CLIENT_ID, clientId)

        // Public UI profile metadata -> PreferencesStorage
        preferencesStorage.set(KEY_USER_NAME, finalProfile.name)
        preferencesStorage.set(KEY_USER_EMAIL, finalProfile.email)
        preferencesStorage.set(KEY_USER_AVATAR, finalProfile.avatarUrl)
        preferencesStorage.set(KEY_OAUTH_CLIENT_ID, clientId)
        preferencesStorage.set(KEY_TOKEN_REFRESHED_AT, timestamp)

        sessionRestored = true
        _currentUser.value = finalProfile
        _authStatusMessage.value = statusNote
    }

    private suspend fun clearStoredAuth() {
        secureStorage.clear()
        preferencesStorage.remove(KEY_USER_NAME)
        preferencesStorage.remove(KEY_USER_EMAIL)
        preferencesStorage.remove(KEY_USER_AVATAR)
        preferencesStorage.remove(KEY_TOKEN_REFRESHED_AT)
    }

    private suspend fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        codeVerifier: String?
    ): TokenExchangeResult? = withContext(Dispatchers.IO) {
        val params = buildList {
            add("code" to code)
            add("client_id" to clientId)
            add("redirect_uri" to REDIRECT_URI)
            add("grant_type" to "authorization_code")
            if (!codeVerifier.isNullOrBlank()) {
                add("code_verifier" to codeVerifier)
            }
        }
        executeTokenRequest(params)
    }

    private suspend fun refreshRemoteTokens(
        refreshToken: String,
        clientId: String
    ): TokenExchangeResult? = withContext(Dispatchers.IO) {
        val params = listOf(
            "client_id" to clientId,
            "refresh_token" to refreshToken,
            "grant_type" to "refresh_token"
        )
        executeTokenRequest(params)
    }

    private fun executeTokenRequest(formParams: List<Pair<String, String>>): TokenExchangeResult? {
        return runCatching {
            val encodedBody = formParams.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            val connection = (URL(GOOGLE_TOKEN_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            try {
                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(encodedBody)
                }
                if (connection.responseCode !in 200..299) {
                    return@runCatching null
                }
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val idToken = json.optString("id_token").takeIf { it.isNotBlank() } ?: return@runCatching null
                val accessToken = json.optString("access_token").takeIf { it.isNotBlank() } ?: return@runCatching null
                val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
                TokenExchangeResult(idToken, accessToken, refreshToken)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun parseJwtIdToken(jwt: String): UserProfile? {
        return runCatching {
            val parts = jwt.split('.')
            if (parts.size < 2) return@runCatching null
            val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val payload = JSONObject(String(decodedBytes, StandardCharsets.UTF_8))

            val sub = payload.optString("sub").takeIf { it.isNotBlank() } ?: return@runCatching null
            val email = payload.optString("email", "")
            val name = payload.optString("name")
                .ifBlank { email.substringBefore('@') }
                .ifBlank { "Google User" }
            val picture = payload.optString("picture").takeIf { it.isNotBlank() }

            UserProfile(
                userId = sub,
                name = name,
                email = email,
                avatarUrl = picture
            )
        }.getOrNull()
    }

    private fun buildJwtToken(account: GoogleAccountOption): String {
        val header = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }.toString()
        val nowSec = System.currentTimeMillis() / 1000L
        val payload = JSONObject().apply {
            put("iss", "https://accounts.google.com")
            put("sub", account.userId)
            put("email", account.email)
            put("email_verified", true)
            put("name", account.name)
            put("picture", account.avatarUrl)
            put("iat", nowSec)
            put("exp", nowSec + 3600L)
        }.toString()

        val flags = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        val encodedHeader = Base64.encodeToString(header.toByteArray(StandardCharsets.UTF_8), flags)
        val encodedPayload = Base64.encodeToString(payload.toByteArray(StandardCharsets.UTF_8), flags)
        val encodedSignature = Base64.encodeToString("oauth2_demo_sig".toByteArray(StandardCharsets.UTF_8), flags)
        return "$encodedHeader.$encodedPayload.$encodedSignature"
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun computeCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun currentTimestamp(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    private data class TokenExchangeResult(
        val idToken: String,
        val accessToken: String,
        val refreshToken: String?
    )

    companion object {
        const val DEFAULT_GOOGLE_CLIENT_ID =
            "733706342085-t05v04fm52knul39qtjb53hrnhek84pp.apps.googleusercontent.com"

        private const val PACKAGE_SCHEME = "com.example.moviecatalog"
        private const val REVERSE_CLIENT_SCHEME =
            "com.googleusercontent.apps.733706342085-t05v04fm52knul39qtjb53hrnhek84pp"
        private const val REDIRECT_URI = "$PACKAGE_SCHEME:/oauth2redirect"
        private const val GOOGLE_AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
        private const val DEFAULT_DEMO_CLIENT_ID = "local_oauth_client"

        private const val KEY_ID_TOKEN = "id_token"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CLIENT_ID = "client_id"

        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_AVATAR = "user_avatar"
        private const val KEY_OAUTH_CLIENT_ID = "oauth_client_id"
        private const val KEY_TOKEN_REFRESHED_AT = "token_refreshed_at"
    }
}
