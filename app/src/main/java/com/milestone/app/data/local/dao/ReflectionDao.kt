package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.milestone.app.data.local.entity.ReflectionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ReflectionDao {

    @Query("SELECT * FROM reflection WHERE weekStart = :weekStart LIMIT 1")
    fun observeForWeek(weekStart: LocalDate): Flow<ReflectionEntity?>

    @Query("SELECT * FROM reflection WHERE weekStart = :weekStart LIMIT 1")
    suspend fun getForWeek(weekStart: LocalDate): ReflectionEntity?

    @Query("SELECT * FROM reflection ORDER BY weekStart DESC")
    fun observeAll(): Flow<List<ReflectionEntity>>

    @Query("SELECT COUNT(*) FROM reflection")
    suspend fun count(): Int

    /** REPLACE pairs with the unique index on weekStart: one reflection per week. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(reflection: ReflectionEntity): Long

    @Update
    suspend fun update(reflection: ReflectionEntity)
}
