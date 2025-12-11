package xyz.xiao6.myboard.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_words",
    indices = [Index(value = ["text"], unique = true)]
)
data class UserWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    var frequency: Int = 1
)
