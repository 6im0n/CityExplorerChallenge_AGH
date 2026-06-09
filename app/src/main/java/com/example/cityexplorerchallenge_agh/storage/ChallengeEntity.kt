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
    val selected: Boolean = false
) {
    fun categoryLabel(): String =
        runCatching { ChallengeCategory.valueOf(category).label }.getOrDefault(category)

    companion object {
        const val STATE_CURRENT = "current"
        const val STATE_FINISHED = "finished"
    }
}

data class CategoryCount(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "count") val count: Int
)