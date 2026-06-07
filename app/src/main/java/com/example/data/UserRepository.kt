package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    val userProfile: Flow<UserProfile?> = userDao.getUserProfile()

    suspend fun insert(userProfile: UserProfile) = userDao.insertUserProfile(userProfile)
}
