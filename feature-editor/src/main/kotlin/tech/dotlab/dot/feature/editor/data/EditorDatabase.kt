package tech.dotlab.dot.feature.editor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Room database for saved pixel art and animation frames. */
@Database(
    entities = [PixelArt::class, PixelFrame::class],
    version = 1,
    exportSchema = false,
)
abstract class EditorDatabase : RoomDatabase() {
    abstract fun pixelArtDao(): PixelArtDao

    companion object {
        @Volatile
        private var instance: EditorDatabase? = null

        fun get(context: Context): EditorDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EditorDatabase::class.java,
                    "dot-editor.db",
                ).build().also { instance = it }
            }
    }
}
