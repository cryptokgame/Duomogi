package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Document
import com.example.data.DuoMongiDatabase
import com.example.data.DuoMongiRepository
import com.example.data.RecentImport
import com.example.data.UserStats
import com.example.data.VocabularyWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DuoMongiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DuoMongiRepository

    val documents: StateFlow<List<Document>>
    val vocabulary: StateFlow<List<VocabularyWord>>
    val userStats: StateFlow<UserStats?>
    val recentImports: StateFlow<List<RecentImport>>

    // UI state
    private val _currentTab = MutableStateFlow("learn") // "library", "learn", "decks", "profile" as per tab bar
    val currentTab = _currentTab.asStateFlow()

    private val _readingDocument = MutableStateFlow<Document?>(null)
    val readingDocument = _readingDocument.asStateFlow()

    private val _selectedWord = MutableStateFlow<VocabularyWord?>(null)
    val selectedWord = _selectedWord.asStateFlow()

    private val _reviewingDeck = MutableStateFlow<String?>(null) // e.g. "Saved from PDF", "Weekly Batch"
    val reviewingDeck = _reviewingDeck.asStateFlow()

    private val _isImportingNew = MutableStateFlow(false)
    val isImportingNew = _isImportingNew.asStateFlow()

    init {
        val db = DuoMongiDatabase.getDatabase(application)
        repository = DuoMongiRepository(db)

        documents = repository.documents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        vocabulary = repository.vocabulary.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        userStats = repository.stats.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        recentImports = repository.recentImports.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Run prepopulation check
        viewModelScope.launch {
            prepopulateDataIfNeeded()
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
        _readingDocument.value = null
        _reviewingDeck.value = null
        _isImportingNew.value = false
    }

    fun openDocument(document: Document) {
        _readingDocument.value = document
        _selectedWord.value = null
    }

    fun closeDocument() {
        _readingDocument.value = null
        _selectedWord.value = null
    }

    fun selectWord(wordStr: String) {
        viewModelScope.launch {
            val matchedWord = vocabulary.value.find { it.word.lowercase() == wordStr.lowercase() }
            if (matchedWord != null) {
                _selectedWord.value = matchedWord
            } else {
                // Generate a fresh quick translation on the fly!
                val newWord = getQuickWordDef(wordStr)
                _selectedWord.value = newWord
            }
        }
    }

    fun clearSelectedWord() {
        _selectedWord.value = null
    }

    fun addWordToDeck(word: VocabularyWord) {
        viewModelScope.launch {
            repository.addWord(word.copy(deckType = "Saved from PDF", timestamp = System.currentTimeMillis()))
            // Increment wordsEncountered count marginally
            userStats.value?.let { current ->
                repository.updateStats(current.copy(wordsEncountered = current.wordsEncountered + 1))
            }
            _selectedWord.value = null
        }
    }

    fun openImportNew() {
        _isImportingNew.value = true
    }

    fun closeImportNew() {
        _isImportingNew.value = false
    }

    fun startReviewDeck(deckType: String) {
        _reviewingDeck.value = deckType
    }

    fun closeReviewDeck() {
        _reviewingDeck.value = null
    }

    fun saveAndCreateStudyPlan() {
        viewModelScope.launch {
            // Study plan created: raise stats slightly and return
            userStats.value?.let { current ->
                repository.updateStats(
                    current.copy(
                        level = current.level + 1,
                        streakDays = current.streakDays + 1
                    )
                )
            }
            _reviewingDeck.value = null
            _currentTab.value = "learn"
        }
    }

    fun importContent(title: String, type: String, text: String) {
        viewModelScope.launch {
            // Save as recent import
            val importId = repository.insertRecentImport(
                RecentImport(
                    name = if (type == "Web Article") "$title.txt" else title,
                    status = "Processing..."
                )
            )

            _isImportingNew.value = false

            // Process mock AI import text parsing delay
            delay(2500)

            // Make it ready to read in recent imports
            repository.updateRecentImport(
                RecentImport(
                    id = importId.toInt(),
                    name = if (type == "Web Article") "$title.txt" else title,
                    status = "Ready to Read"
                )
            )

            // Save actual document
            val processedContent = if (text.isBlank()) {
                "La lecture est essentielle pour le développement de l'esprit. Elle enrichit le vocabulaire et améliore la compréhension du monde."
            } else text

            repository.insertDocument(
                Document(
                    title = title,
                    type = type,
                    content = processedContent,
                    readProgress = 0,
                    ratingStars = 4,
                    coverEmoji = if (type == "Web Article") "🌐" else "📄",
                    highlightWords = "essentielle,enrichit,améliore,développement,compréhension"
                )
            )
        }
    }

    private suspend fun prepopulateDataIfNeeded() {
        // Double check documents list size
        val currentDocs = documents.first()
        if (currentDocs.isEmpty()) {
            // 1. Insert documents
            repository.insertDocument(
                Document(
                    title = "The Great Gatsby",
                    type = "Ebook",
                    content = "La lecture est essentielle pour le développement de l'esprit. Elle enrichit le vocabulaire et améliore la compréhension du monde.\n\n" +
                            "La lecture est essentielle pour le développement de l'esprit. Elle enrichit le vocabulaire et améliore la compréhension du monde.\n\n" +
                            "La lecture est neordine, que non l'esprit. Elle enrichit le vocabulaire et améliore la comprecmension du monde.",
                    readProgress = 30,
                    ratingStars = 3,
                    coverEmoji = "📖"
                )
            )
            repository.insertDocument(
                Document(
                    title = "AI News Article",
                    type = "Web Article",
                    content = "La technologie progresse rapidement. L'intelligence artificielle enrichit nos capacités quotidiennes et améliore notre compréhension de données complexes.\n" +
                            "Il est essentiel d'apprendre continuellement pour rester à jour et stimuler l'esprit.",
                    readProgress = 10,
                    ratingStars = 1,
                    coverEmoji = "🌐"
                )
            )
            repository.insertDocument(
                Document(
                    title = "Machine Learning Basics",
                    type = "PDF",
                    content = "L'apprentissage automatique fait partie intégrante de l'informatique moderne.\n" +
                            "Comprendre les algorithmes est une étape essentielle pour le développement de solutions puissantes.\n" +
                            "Chaque concept enrichit notre boîte à outils et améliore l'efficacité des modèles.",
                    readProgress = 60,
                    ratingStars = 4,
                    coverEmoji = "📄"
                )
            )
            repository.insertDocument(
                Document(
                    title = "Cognitive Science Paper",
                    type = "PDF",
                    content = "Les processus cognitifs déterminent comment nous percevons l'information.\n" +
                            "La lecture est essentielle pour fortifier les connexions neuronales et favoriser le développement de la mémoire.\n" +
                            "Une étude approfondie enrichit les théories actuelles.",
                    readProgress = 5,
                    ratingStars = 4,
                    coverEmoji = "🧠"
                )
            )

            // 2. Insert Core Vocabulary matches image 7 and image 1
            repository.addWord(
                VocabularyWord(
                    word = "essentielle",
                    ipa = "/ɛ.sã.sjɛl/",
                    translation = "essential / crucial",
                    sentence = "La lecture est essentielle pour le développement de l'esprit.",
                    contextTip = "The adjective \"essentielle\" is feminine. In its masculine form, it is written \"essentiel\".",
                    isMastered = false,
                    deckType = "Saved from PDF"
                )
            )
            repository.addWord(
                VocabularyWord(
                    word = "enrichit",
                    ipa = "/ã.ʁi.ʃi/",
                    translation = "enriches / enhances",
                    sentence = "Elle enrichit le vocabulaire et améliore la compréhension du monde.",
                    contextTip = "Context Tip: The verb \"enrichit\" is often used to describe improving knowledge or skills.",
                    isMastered = false,
                    deckType = "Saved from PDF"
                )
            )
            repository.addWord(
                VocabularyWord(
                    word = "vocabulaire",
                    ipa = "/vɔ.ka.by.lɛʁ/",
                    translation = "vocabulary",
                    sentence = "Elle enrichit le vocabulaire et améliore la compréhension du monde.",
                    contextTip = "A masculine noun: \"le vocabulaire\". In French, a large vocabulary enhances conversational fluency.",
                    isMastered = false,
                    deckType = "Saved from PDF"
                )
            )
            repository.addWord(
                VocabularyWord(
                    word = "développement",
                    ipa = "/de.vlɔp.mɑ̃/",
                    translation = "development",
                    sentence = "La lecture est essentielle pour le développement de l'esprit.",
                    contextTip = "Note the single 'l' and double 'p' in French, which differs from English spelling.",
                    isMastered = false,
                    deckType = "Weekly Batch"
                )
            )
            repository.addWord(
                VocabularyWord(
                    word = "compréhension",
                    ipa = "/kɔ̃.pʁe.ɑ̃.sjɔ̃/",
                    translation = "understanding / comprehension",
                    sentence = "Elle enrichit le vocabulaire et améliore la compréhension du monde.",
                    contextTip = "A feminine noun ending in \"-sion\". Almost all French words ending in \"-tion\" or \"-sion\" are feminine.",
                    isMastered = false,
                    deckType = "Weekly Batch"
                )
            )
            repository.addWord(
                VocabularyWord(
                    word = "améliore",
                    ipa = "/a.me.ljɔʁ/",
                    translation = "improves",
                    sentence = "Elle enrichit le vocabulaire et améliore la compréhension.",
                    contextTip = "From the verb \"améliorer\" (to improve or better). Common synonym for \"perfectionner\".",
                    isMastered = true,
                    deckType = "Mastered Words"
                )
            )

            // Add dummy words to round counts matching image 5
            // "Saved from PDF" list -> has some words. We will populate a count of total or represent beautifully.
            // 3. User stats init
            repository.updateStats(
                UserStats(
                    level = 10,
                    streakDays = 15,
                    wordsEncountered = 1250,
                    pagesRead = 87,
                    accuracy = 92
                )
            )

            // 4. Initial Recent Imports
            repository.insertRecentImport(
                RecentImport(
                    name = "Research Paper.pdf",
                    status = "Processing..."
                )
            )
            repository.insertRecentImport(
                RecentImport(
                    name = "NYT Article",
                    status = "Ready to Read"
                )
            )
            repository.insertRecentImport(
                RecentImport(
                    name = "Book Chapter",
                    status = "Ready to Read"
                )
            )
        }
    }

    private fun getQuickWordDef(wordStr: String): VocabularyWord {
        val word = wordStr.lowercase().trim().removeSuffix(",").removeSuffix(".")
        val ipa: String
        val trans: String
        val tip: String
        val sentence = "La lecture de ce document contient le mot $word."

        when (word) {
            "monde" -> {
                ipa = "/mɔ̃d/"
                trans = "world / people"
                tip = "A masculine noun \"le monde\". Can also be used to mean 'everybody' (tout le monde)."
            }
            "esprit" -> {
                ipa = "/ɛs.pʁi/"
                trans = "mind / spirit"
                tip = "From Latin 'spiritus'. Note the silent 't' at the end. Usually masculine \"l'esprit\"."
            }
            "lecture" -> {
                ipa = "/lɛk.tyʁ/"
                trans = "reading"
                tip = "Feminine noun structure ending in \"-ure\". Linked to the verb \"lire\" (to read)."
            }
            "technologie" -> {
                ipa = "/tɛk.nɔ.lɔ.ʒi/"
                trans = "technology"
                tip = "Feminine noun. In French, nouns with ending \"-gie\" are feminine."
            }
            "intelligence" -> {
                ipa = "/ɛ̃.tɛ.li.ʒɑ̃s/"
                trans = "intelligence"
                tip = "Feminine noun ending in '-ence'."
            }
            else -> {
                ipa = "/${word.take(2)}.../"
                trans = "Translation of $word"
                tip = "Context Tip: Tapped word '$word' is parsed dynamically on the fly."
            }
        }

        return VocabularyWord(
            word = word,
            ipa = ipa,
            translation = trans,
            sentence = sentence,
            contextTip = tip,
            isMastered = false,
            deckType = "Saved from PDF"
        )
    }
}
