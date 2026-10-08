package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.milestone.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observe(id: Long = UserProfileEntity.SINGLETON_ID): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun get(id: Long = UserProfileEntity.SINGLETON_ID): UserProfileEntity?

    /** Upsert: there is only ever one profile row, pinned to a known id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET name = :name WHERE id = :id")
    suspend fun updateName(name: String, id: Long = UserProfileEntity.SINGLETON_ID)

    @Query("UPDATE user_profile SET hasOnboarded = :value WHERE id = :id")
    suspend fun setOnboarded(value: Boolean, id: Long = UserProfileEntity.SINGLETON_ID)
}
