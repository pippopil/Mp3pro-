package com.tohsoft.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY updatedAt DESC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks")
    fun getAllTracksSync(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE trackId = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE trackId = :trackId LIMIT 1")
    fun getTrackByIdSync(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT :limit")
    suspend fun getMostPlayedTracks(limit: Int = 50): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE tags LIKE '%' || :tag || '%'")
    suspend fun findTracksByTag(tag: String): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceSync(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplaceAllSync(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedAt = :playedAt, updatedAt = :playedAt WHERE trackId = :trackId")
    fun incrementPlayCountSync(trackId: Long, playedAt: Long)

    @Query("DELETE FROM tracks WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: Long)

    @Query("SELECT COUNT(*) FROM tracks")
    fun countTracksSync(): Int
}
