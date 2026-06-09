package com.example.cityexplorerchallenge_agh.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChallengeDao {

    //Save a challenge the user added.
    @Insert
    fun add(challenge: ChallengeEntity)

    //ll challenges in a given state ("current" or "finished").
    @Query("SELECT * FROM challenges WHERE state = :state")
    fun byState(state: String): List<ChallengeEntity>

    //Every challenge saved, any state. Used to avoid suggesting one twice.
    @Query("SELECT * FROM challenges")
    fun allChallenges(): List<ChallengeEntity>

    //One challenge by its id.
    @Query("SELECT * FROM challenges WHERE id = :id")
    fun byId(id: Int): ChallengeEntity?

    //Attach (or change) the photo path of a challenge.
    @Query("UPDATE challenges SET imagePath = :path WHERE id = :id")
    fun setImage(id: Int, path: String)

    //Move a challenge to another state (e.g. current -> finished)
    @Query("UPDATE challenges SET state = :state WHERE id = :id")
    fun setState(id: Int, state: String)

    //Mark a challenge finished and remember the time it was reached
    @Query("UPDATE challenges SET state = 'finished', finishedAt = :finishedAt WHERE id = :id")
    fun markFinished(id: Int, finishedAt: Long)

    //lect every challenge (only one may be the "go to" target)
    @Query("UPDATE challenges SET selected = 0")
    fun clearSelection()

    //Mark one challenge as the selected "go to" target
    @Query("UPDATE challenges SET selected = 1 WHERE id = :id")
    fun select(id: Int)

    //The challenge currently chosen as the "go to" target, if any.
    @Query("SELECT * FROM challenges WHERE selected = 1 LIMIT 1")
    fun selectedChallenge(): ChallengeEntity?

    @Delete
    fun delete(challenge: ChallengeEntity)

    //count challenges per category, to adapt future suggestions
    @Query("SELECT category, COUNT(*) AS count FROM challenges GROUP BY category")
    fun categoryCounts(): List<CategoryCount>
}