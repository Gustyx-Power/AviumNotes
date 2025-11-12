package id.avium.aviumnotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val color: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val spanCount: Int = 1,
    val hasDrawing: Boolean = false,
    val drawingPath: String? = null
)
