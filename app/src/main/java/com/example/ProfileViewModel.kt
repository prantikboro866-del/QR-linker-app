package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserProfile
import com.example.data.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: UserRepository) : ViewModel() {
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveProfile(userProfile: UserProfile) {
        viewModelScope.launch {
            repository.insert(userProfile)
        }
    }

    fun incrementLinkClick(linkIndex: Int) {
        viewModelScope.launch {
            userProfile.value?.let { profile ->
                val links = profile.linksJson.split(",")
                    .map { it.split("|") }
                    .filter { it.size >= 2 }
                    .map { 
                        // Current format P|U|C, handle legacy P|U
                        val platform = it[0]
                        val url = it[1]
                        val clicks = if (it.size >= 3) it[2].toIntOrNull() ?: 0 else 0
                        Triple(platform, url, clicks)
                    }.toMutableList()
                
                if (linkIndex in links.indices) {
                    val link = links[linkIndex]
                    links[linkIndex] = Triple(link.first, link.second, link.third + 1)
                    val newLinksJson = links.joinToString(",") { "${it.first}|${it.second}|${it.third}" }
                    repository.insert(profile.copy(linksJson = newLinksJson))
                }
            }
        }
    }

}
