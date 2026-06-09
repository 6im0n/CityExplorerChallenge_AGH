package com.example.cityexplorerchallenge_agh.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cityexplorerchallenge_agh.finder.ChallengeCategory

/**
 * One saved challenge.
 *
 * The same row is used for both lists: while you are doing it the state is
 * "current"; once you reach it the state becomes "finished". No second table.
 */
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val state: String,
    // True for the one challenge the user picked as the "go to" target.
    val selected: Boolean = false
) {
    /** Human-readable category name for the list (falls back to the raw value). */
    fun categoryLabel(): String =
        runCatching { ChallengeCategory.valueOf(category).label }.getOrDefault(category)

    companion object {
        const val STATE_CURRENT = "current"
        const val STATE_FINISHED = "finished"
    }
}

/** Result row of "how many challenges per category" (used to adapt suggestions). */
data class CategoryCount(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "count") val count: Int
)