package com.example.moviecatalog.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val tokenRefreshedAt: String? = null
)
