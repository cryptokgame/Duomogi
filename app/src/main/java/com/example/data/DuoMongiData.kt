package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entities
// ==========================================

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "Ebook", "Web Article", "PDF"
    val content: String,
    val readProgress: Int, // 0 - 100
    val ratingStars: Int, // 1 - 5
    val coverEmoji: String = "📘",
    val highlightWords: String = "essentielle,enrichit,améliore,développement,compréhension" // Comma-separated list of words to highlight
)

@Entity(tableName = "vocabulary_words")
data class VocabularyWord(
    @PrimaryKey val word: String,
    val ipa: String,
    val translation: String,
    val sentence: String,
    val contextTip: String,
    val isMastered: Boolean = false,
    val deckType: String = "Saved from PDF", // "Saved from PDF", "Weekly Batch", "Mastered Words"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 10,
    val streakDays: Int = 15,
    val wordsEncountered: Int = 1250,
    val pagesRead: Int = 87,
    val accuracy: Int = 92
)

@Entity(tableName = "recent_imports")
data class RecentImport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: String, // "Processing...", "Ready to Read"
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// 2. DAOs
// ==========================================

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY id DESC")
    fun getAllDocumentsFlow(): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): Document?

    @Query("UPDATE documents SET readProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Int, progress: Int)
}

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_words ORDER BY timestamp DESC")
    fun getAllWordsFlow(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary_words WHERE deckType = :deckType ORDER BY timestamp DESC")
    fun getWordsByDeckFlow(deckType: String): Flow<List<VocabularyWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: VocabularyWord)

    @Update
    suspend fun updateWord(word: VocabularyWord)

    @Query("DELETE FROM vocabulary_words WHERE word = :word")
    suspend fun deleteWord(word: String)
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: UserStats)

    @Query("UPDATE user_stats SET streakDays = :streak WHERE id = 1")
    suspend fun updateStreak(streak: Int)

    @Query("UPDATE user_stats SET wordsEncountered = :words WHERE id = 1")
    suspend fun updateWordsEncountered(words: Int)
}

@Dao
interface RecentImportDao {
    @Query("SELECT * FROM recent_imports ORDER BY timestamp DESC")
    fun getAllRecentImportsFlow(): Flow<List<RecentImport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentImport(recentImport: RecentImport): Long

    @Update
    suspend fun updateRecentImport(recentImport: RecentImport)
}

// ==========================================
// 3. Database
// ==========================================

@Database(
    entities = [Document::class, VocabularyWord::class, UserStats::class, RecentImport::class],
    version = 1,
    exportSchema = false
)
abstract class DuoMongiDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun recentImportDao(): RecentImportDao

    companion object {
        @Volatile
        private var INSTANCE: DuoMongiDatabase? = null

        fun getDatabase(context: Context): DuoMongiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DuoMongiDatabase::class.java,
                    "duomongi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. Repository
// ==========================================

class DuoMongiRepository(private val db: DuoMongiDatabase) {
    val documents: Flow<List<Document>> = db.documentDao().getAllDocumentsFlow()
    val vocabulary: Flow<List<VocabularyWord>> = db.vocabularyDao().getAllWordsFlow()
    val stats: Flow<UserStats?> = db.userStatsDao().getUserStatsFlow()
    val recentImports: Flow<List<RecentImport>> = db.recentImportDao().getAllRecentImportsFlow()

    fun getWordsByDeck(deckType: String): Flow<List<VocabularyWord>> =
        db.vocabularyDao().getWordsByDeckFlow(deckType)

    suspend fun getDocumentById(id: Int): Document? =
        db.documentDao().getDocumentById(id)

    suspend fun updateDocumentProgress(id: Int, progress: Int) {
        db.documentDao().updateProgress(id, progress)
    }

    suspend fun insertDocument(document: Document): Long {
        return db.documentDao().insertDocument(document)
    }

    suspend fun addWord(word: VocabularyWord) {
        db.vocabularyDao().insertWord(word)
    }

    suspend fun deleteWord(word: String) {
        db.vocabularyDao().deleteWord(word)
    }

    suspend fun updateStats(stats: UserStats) {
        db.userStatsDao().insertStats(stats)
    }

    suspend fun insertRecentImport(recentImport: RecentImport): Long {
        return db.recentImportDao().insertRecentImport(recentImport)
    }

    suspend fun updateRecentImport(recentImport: RecentImport) {
        db.recentImportDao().updateRecentImport(recentImport)
    }

    // Prepopulate with gorgeous data
    suspend fun prepopulateIfNeeded() {
        // Simple logic: if stats is empty, we populate
        // Handled in ViewModel or DatabaseCallback
    }
}
