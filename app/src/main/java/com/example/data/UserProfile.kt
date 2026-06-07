package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val bio: String,
    val avatarUrl: String,
    val linksJson: String, // Serialized list of links
    val totalClicks: Int = 0,
    val themeColor: String = "Default",
    val themeFont: String = "Normal",
    val backgroundImageUrl: String = "",
    val gradientTheme: String = "None",
    val isDarkMode: Boolean = false,
    val qrForegroundColor: String = "BLACK",
    val qrBackgroundColor: String = "WHITE",
    val qrStyle: String = "SQUARE"
)
