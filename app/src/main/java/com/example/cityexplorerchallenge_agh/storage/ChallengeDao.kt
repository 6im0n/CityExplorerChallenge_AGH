package com.example.cityexplorerchallenge_agh.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChallengeDao {

    /** Save a challenge the user added. */
    @Insert
    fun add(challenge: ChallengeEntity)

    /** All challenges in a given state ("current" or "finished"). */
    @Query("SELECT * FROM challenges WHERE state = :state")
    fun byState(state: String): List<ChallengeEntity>

    /** Move a challenge to another state (e.g. current -> finished). */
    @Query("UPDATE challenges SET state = :state WHERE id = :id")
    fun setState(id: Int, state: String)

    @Delete
    fun delete(challenge: ChallengeEntity)

    /** Count challenges per category, to adapt future suggestions. */
    @Query("SELECT category, COUNT(*) AS count FROM challenges GROUP BY category")
    fun categoryCounts(): List<CategoryCount>
}