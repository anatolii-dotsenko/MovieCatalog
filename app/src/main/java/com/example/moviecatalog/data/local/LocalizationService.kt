package com.example.moviecatalog.data.local

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moviecatalog.data.auth.IAuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    UK("uk", "Українська");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EN
    }
}

enum class AppThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}

enum class AppFontScale(val storageKey: String, val scaleFactor: Float) {
    SMALL("small", 0.92f),
    NORMAL("normal", 1.0f),
    LARGE("large", 1.12f);

    companion object {
        fun fromKey(key: String?): AppFontScale =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: NORMAL
    }
}

interface ILocalizationService {
    val currentLanguage: StateFlow<AppLanguage>
    val themeMode: StateFlow<AppThemeMode>
    val fontScale: StateFlow<AppFontScale>

    fun setLanguage(language: AppLanguage)
    fun setThemeMode(mode: AppThemeMode)
    fun setFontScale(scale: AppFontScale)
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
    fun createLocalizedContext(
        baseContext: Context,
        language: AppLanguage = currentLanguage.value
    ): Context
}

/**
 * Manages app localization (EN/UK), theme mode, and accessibility font scale,
 * persisting preferences per signed-in user via [IPreferencesStorage].
 */
class LocalizationService(
    context: Context,
    private val preferencesStorage: IPreferencesStorage,
    private val authService: IAuthService
) : ILocalizationService {

    private val appContext = context.applicationContext
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val resourceLock = Any()

    private val initialLanguage = AppLanguage.fromCode(preferencesStorage.get(KEY_GLOBAL_LOCALE))
    private val initialTheme = AppThemeMode.fromKey(preferencesStorage.get(KEY_GLOBAL_THEME))
    private val initialFontScale = AppFontScale.fromKey(preferencesStorage.get(KEY_GLOBAL_FONT_SCALE))

    private var cachedLanguage: AppLanguage = initialLanguage
    private var cachedResources: Resources = createResources(appContext, initialLanguage)

    private val _currentLanguage = MutableStateFlow(initialLanguage)
    override val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _themeMode = MutableStateFlow(initialTheme)
    override val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _fontScale = MutableStateFlow(initialFontScale)
    override val fontScale: StateFlow<AppFontScale> = _fontScale.asStateFlow()

    private var activeUserId: String? = null

    init {
        Locale.setDefault(Locale.forLanguageTag(initialLanguage.code))
        serviceScope.launch {
            authService.currentUser
                .map { it?.userId }
                .distinctUntilChanged()
                .collect { userId ->
                    onActiveUserChanged(userId)
                }
        }
    }

    private fun onActiveUserChanged(userId: String?) {
        activeUserId = userId
        if (userId.isNullOrBlank()) {
            return
        }

        val safeId = sanitizeUserId(userId)
        val savedLocale = preferencesStorage.get("$KEY_USER_LOCALE_PREFIX$safeId")
        val savedTheme = preferencesStorage.get("$KEY_USER_THEME_PREFIX$safeId")
        val savedFontScale = preferencesStorage.get("$KEY_USER_FONT_SCALE_PREFIX$safeId")

        val resolvedLanguage = if (savedLocale != null) {
            AppLanguage.fromCode(savedLocale)
        } else {
            _currentLanguage.value.also {
                preferencesStorage.set("$KEY_USER_LOCALE_PREFIX$safeId", it.code)
            }
        }

        val resolvedTheme = if (savedTheme != null) {
            AppThemeMode.fromKey(savedTheme)
        } else {
            _themeMode.value.also {
                preferencesStorage.set("$KEY_USER_THEME_PREFIX$safeId", it.storageKey)
            }
        }

        val resolvedFontScale = if (savedFontScale != null) {
            AppFontScale.fromKey(savedFontScale)
        } else {
            _fontScale.value.also {
                preferencesStorage.set("$KEY_USER_FONT_SCALE_PREFIX$safeId", it.storageKey)
            }
        }

        applyLanguage(resolvedLanguage)
        _themeMode.value = resolvedTheme
        _fontScale.value = resolvedFontScale
        preferencesStorage.set(KEY_GLOBAL_LOCALE, resolvedLanguage.code)
        preferencesStorage.set(KEY_GLOBAL_THEME, resolvedTheme.storageKey)
        preferencesStorage.set(KEY_GLOBAL_FONT_SCALE, resolvedFontScale.storageKey)
    }

    override fun setLanguage(language: AppLanguage) {
        applyLanguage(language)
        preferencesStorage.set(KEY_GLOBAL_LOCALE, language.code)
        activeUserId?.let { userId ->
            preferencesStorage.set("$KEY_USER_LOCALE_PREFIX${sanitizeUserId(userId)}", language.code)
        }
    }

    override fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        preferencesStorage.set(KEY_GLOBAL_THEME, mode.storageKey)
        activeUserId?.let { userId ->
            preferencesStorage.set("$KEY_USER_THEME_PREFIX${sanitizeUserId(userId)}", mode.storageKey)
        }
    }

    override fun setFontScale(scale: AppFontScale) {
        _fontScale.value = scale
        preferencesStorage.set(KEY_GLOBAL_FONT_SCALE, scale.storageKey)
        activeUserId?.let { userId ->
            preferencesStorage.set("$KEY_USER_FONT_SCALE_PREFIX${sanitizeUserId(userId)}", scale.storageKey)
        }
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        val resources = synchronized(resourceLock) {
            val lang = _currentLanguage.value
            if (cachedLanguage != lang) {
                cachedLanguage = lang
                cachedResources = createResources(appContext, lang)
            }
            cachedResources
        }
        return if (formatArgs.isEmpty()) {
            resources.getString(resId)
        } else {
            resources.getString(resId, *formatArgs)
        }
    }

    override fun createLocalizedContext(baseContext: Context, language: AppLanguage): Context {
        val localizedResources = createResources(baseContext, language)
        return object : ContextWrapper(baseContext) {
            override fun getResources(): Resources = localizedResources
            override fun getAssets(): AssetManager = localizedResources.assets
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        synchronized(resourceLock) {
            cachedLanguage = language
            cachedResources = createResources(appContext, language)
        }
        Locale.setDefault(Locale.forLanguageTag(language.code))
        _currentLanguage.value = language
    }

    private fun createResources(baseContext: Context, language: AppLanguage): Resources {
        val config = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language.code))
        }
        return baseContext.createConfigurationContext(config).resources
    }

    private fun sanitizeUserId(userId: String): String =
        userId.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    private companion object {
        const val KEY_GLOBAL_LOCALE = "app_locale_default"
        const val KEY_GLOBAL_THEME = "app_theme_default"
        const val KEY_GLOBAL_FONT_SCALE = "app_font_scale_default"

        const val KEY_USER_LOCALE_PREFIX = "user_locale_"
        const val KEY_USER_THEME_PREFIX = "user_theme_"
        const val KEY_USER_FONT_SCALE_PREFIX = "user_font_scale_"
    }
}

@Composable
fun ProvideAppLocalization(
    localizationService: ILocalizationService,
    content: @Composable (AppThemeMode) -> Unit
) {
    val language by localizationService.currentLanguage.collectAsStateWithLifecycle()
    val themeMode by localizationService.themeMode.collectAsStateWithLifecycle()
    val fontScale by localizationService.fontScale.collectAsStateWithLifecycle()

    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val baseDensity = LocalDensity.current

    val localizedContext = remember(baseContext, baseConfig, language) {
        localizationService.createLocalizedContext(baseContext, language)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalResources provides localizedContext.resources,
        LocalDensity provides Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * fontScale.scaleFactor
        )
    ) {
        content(themeMode)
    }
}
