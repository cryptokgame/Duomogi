package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Document
import com.example.data.RecentImport
import com.example.data.VocabularyWord
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoMongiApp(viewModel: DuoMongiViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val readingDoc by viewModel.readingDocument.collectAsState()
    val reviewingDeck by viewModel.reviewingDeck.collectAsState()
    val isImportingNew by viewModel.isImportingNew.collectAsState()
    val savedWords by viewModel.vocabulary.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp
        val showBottomBar = !isWideScreen && readingDoc == null && reviewingDeck == null

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    DuoMongiBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DuoMongiBg)
                    .padding(innerPadding)
            ) {
                // Wide Screen Left Sidebar Navigation (Inspired by Duolingo Web layout)
                if (isWideScreen && readingDoc == null && reviewingDeck == null) {
                    DuoMongiSidebar(
                        currentTab = currentTab,
                        onTabSelected = { viewModel.selectTab(it) },
                        wordCount = if (savedWords.isNotEmpty()) savedWords.size else 15
                    )
                    // Visual separator line with tactile thickness
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.5.dp)
                            .background(Color(0xFFE5E5E5))
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Screen routing based on state
                    when {
                        readingDoc != null -> {
                            ReadingScreen(
                                document = readingDoc!!,
                                viewModel = viewModel,
                                onBack = { viewModel.closeDocument() }
                            )
                        }
                        reviewingDeck != null -> {
                            WordBatchReviewScreen(
                                deckType = reviewingDeck!!,
                                viewModel = viewModel,
                                onBack = { viewModel.closeReviewDeck() }
                            )
                        }
                        isImportingNew -> {
                            ImportContentScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.closeImportNew() }
                            )
                        }
                        else -> {
                            // Normal tab views
                            when (currentTab) {
                                "library" -> LibraryScreen(viewModel = viewModel)
                                "learn" -> LearnScreen(viewModel = viewModel)
                                "decks" -> DecksScreen(viewModel = viewModel)
                                "profile" -> ProfileScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. Navigation Components (Bottom Bar + Adaptive Sidebar)
// ==========================================

@Composable
fun DuoMongiSidebar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    wordCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // App title with mascot avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.duomongi_monkey_avatar),
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, DuoMongiBlue, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "DuoMongi",
                fontSize = 24.sp,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Black,
                color = DuoMongiBlue
            )
        }

        // Sidebar Navigation Links
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            SidebarTabItem(
                label = "Library",
                icon = Icons.Outlined.Book,
                selectedIcon = Icons.Filled.Book,
                isSelected = currentTab == "library",
                onClick = { onTabSelected("library") },
                color = DuoMongiBlue,
                activeBg = HighlightBlue
            )
            
            SidebarTabItem(
                label = "Study Path",
                icon = Icons.Outlined.School,
                selectedIcon = Icons.Filled.School,
                isSelected = currentTab == "learn",
                onClick = { onTabSelected("learn") },
                color = DuoMongiGreen,
                activeBg = DuoMongiGreenLight
            )
            
            SidebarTabItem(
                label = "Saved Decks",
                icon = Icons.Outlined.Style,
                selectedIcon = Icons.Filled.Style,
                isSelected = currentTab == "decks",
                onClick = { onTabSelected("decks") },
                color = DuoMongiYellow,
                activeBg = DuoMongiYellowLight
            )
            
            SidebarTabItem(
                label = "Profile",
                icon = Icons.Outlined.Person,
                selectedIcon = Icons.Filled.Person,
                isSelected = currentTab == "profile",
                onClick = { onTabSelected("profile") },
                color = DuoMongiOrange,
                activeBg = DuoMongiOrangeLight
            )
        }

        // Active study stats banner on sidebar footer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 4.dp)
                    .background(Color(0xFFE5E5E5), RoundedCornerShape(16.dp))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.2.dp, Color(0xFFE5E5E5), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "STUDY PROGRESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = DuoMongiYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$wordCount Words Saved",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    activeBg: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 2.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 2.dp)
                    .background(color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) activeBg else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) color else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else icon,
                contentDescription = label,
                tint = if (isSelected) color else TextGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) TextDark else TextGray
            )
        }
    }
}

@Composable
fun DuoMongiBottomBar(currentTab: String, onTabSelected: (String) -> Unit) {
    // Elegant tactile bottom bar matching playful modern branding
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // Thin clean top divider border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFFE5E5E5))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTabItem(
                label = "Library",
                icon = Icons.Outlined.Book,
                selectedIcon = Icons.Filled.Book,
                isSelected = currentTab == "library",
                onClick = { onTabSelected("library") },
                testTag = "tab_library"
            )
            BottomTabItem(
                label = "Study",
                icon = Icons.Outlined.School,
                selectedIcon = Icons.Filled.School,
                isSelected = currentTab == "learn",
                onClick = { onTabSelected("learn") },
                testTag = "tab_study"
            )
            BottomTabItem(
                label = "Decks",
                icon = Icons.Outlined.Style,
                selectedIcon = Icons.Filled.Style,
                isSelected = currentTab == "decks",
                onClick = { onTabSelected("decks") },
                testTag = "tab_decks"
            )
            BottomTabItem(
                label = "Profile",
                icon = Icons.Outlined.Person,
                selectedIcon = Icons.Filled.Person,
                isSelected = currentTab == "profile",
                onClick = { onTabSelected("profile") },
                testTag = "tab_profile"
            )
        }
    }
}

@Composable
fun RowScope.BottomTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "tab_scale"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) DuoMongiBlue.copy(alpha = 0.12f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) selectedIcon else icon,
                    contentDescription = label,
                    tint = if (isSelected) DuoMongiBlue else TextGray,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) DuoMongiBlue else TextGray
            )
        }
    }
}

@Composable
fun WordCountBadge(savedCount: Int, modifier: Modifier = Modifier) {
    // Interactive 3D pill badge
    Box(
        modifier = modifier
            .padding(bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 3.dp)
                .background(DuoMongiBlueDark, RoundedCornerShape(16.dp))
        )
        Box(
            modifier = Modifier
                .background(DuoMongiBlue, RoundedCornerShape(16.dp))
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$savedCount",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ==========================================
// 2. Playful Custom Mascot Header
// ==========================================

@Composable
fun MainAppBar(title: String, subtitle: String = "", viewModel: DuoMongiViewModel) {
    val stats by viewModel.userStats.collectAsState()
    val savedWords by viewModel.vocabulary.collectAsState()
    
    val displayLevel = stats?.level ?: 10
    val displayStreak = stats?.streakDays ?: 15
    val displayWords = if (savedWords.isNotEmpty()) savedWords.size else 15
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Language Selector (with modern playful look)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DuoMongiBlue.copy(alpha = 0.08f))
                    .border(1.5.dp, DuoMongiBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // Playful learning mode flag emoji
                Text(
                    text = "🇪🇸",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ES",
                    fontSize = 13.sp,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = DuoMongiBlue
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = DuoMongiBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Right Group: Streak Flame, Crown XP, and Hearts Lives
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak Flame
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DuoMongiOrangeLight)
                        .border(1.5.dp, DuoMongiOrange.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = "Streak",
                        tint = DuoMongiOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$displayStreak",
                        fontSize = 14.sp,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Black,
                        color = DuoMongiOrangeDark
                    )
                }

                // Gems Crown / XP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DuoMongiYellowLight)
                        .border(1.5.dp, DuoMongiYellow.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "XP Crowns",
                        tint = DuoMongiYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${displayLevel * 10}",
                        fontSize = 14.sp,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Black,
                        color = DuoMongiYellowDark
                    )
                }

                // Hearts (Infinite life)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DuoMongiPurpleLight)
                        .border(1.5.dp, DuoMongiPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Lifes",
                        tint = DuoMongiPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "∞",
                        fontSize = 14.sp,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Black,
                        color = DuoMongiPurpleDark
                    )
                }
            }
        }

        // Secondary titles list displaying context details
        if (title.isNotEmpty() && title != "DuoMongi") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Black,
                    color = DuoMongiBlue
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• $subtitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                }
            }
        }

        // Crisp accent bottom divider edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFFE5E5E5))
        )
    }
}

// ==========================================
// 3. Screen: Learn / Study pathway
// ==========================================

// ==========================================
// 2. Playful Custom Mascot Speech Bubble
// ==========================================

@Composable
fun MascotSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFFE5E5E5)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.duomongi_monkey_avatar),
            contentDescription = "DuoMongi Mascot",
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .border(3.dp, DuoMongiBlue, CircleShape)
                .background(Color.White)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Cartoon speech bubble pointer
        Canvas(modifier = Modifier.size(10.dp, 16.dp)) {
            val path = Path().apply {
                moveTo(size.width, 0f)
                lineTo(0f, size.height / 2f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path = path, color = borderColor)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color.White, RoundedCornerShape(18.dp))
                .border(2.5.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = TextDark,
                lineHeight = 18.sp
            )
        }
    }
}

// ==========================================
// 3. Screen: Learn / Study pathway (Wavy Map)
// ==========================================

@Composable
fun LearnScreen(viewModel: DuoMongiViewModel) {
    val savedWords by viewModel.vocabulary.collectAsState()
    val activeCount = savedWords.filter { !it.isMastered }.size
    // Ensure accurate representation in badges
    val displayCount = if (activeCount > 0) activeCount else 15

    Column(modifier = Modifier.fillMaxSize()) {
        MainAppBar(
            title = "DuoMongi",
            subtitle = "Your Weekly Challenge",
            viewModel = viewModel
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mascot Tip banner
            item {
                MascotSpeechBubble(
                    text = "Welcome back! Let's continue your custom AI-Generated learning path today!",
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            // Path Nodes list (Lesson 1-4 alternating zigzag map)
            item {
                LessonPathNodeCircle(
                    lessonNumber = 1,
                    title = "Vocab Review - \"enrichit\"",
                    progress = 0.65f,
                    stars = 5,
                    color = DuoMongiOrange,
                    darkColor = DuoMongiOrangeDark,
                    icon = Icons.Filled.Book,
                    offsetX = -40,
                    onClick = { viewModel.startReviewDeck("Saved from PDF") }
                )
            }

            item {
                LessonConnector(color = DuoMongiOrange, startOffset = -40, endOffset = 30)
            }

            item {
                LessonPathNodeCircle(
                    lessonNumber = 2,
                    title = "Grammar Practice",
                    progress = 0.35f,
                    stars = 4,
                    color = DuoMongiGreen,
                    darkColor = DuoMongiGreenDark,
                    icon = Icons.Filled.Psychology,
                    offsetX = 30,
                    onClick = { viewModel.startReviewDeck("Weekly Batch") }
                )
            }

            item {
                LessonConnector(color = DuoMongiGreen, startOffset = 30, endOffset = -20)
            }

            item {
                LessonPathNodeCircle(
                    lessonNumber = 3,
                    title = "Article Comprehension",
                    progress = 0.85f,
                    stars = 4,
                    color = DuoMongiBlue,
                    darkColor = DuoMongiBlueDark,
                    icon = Icons.Filled.FindInPage,
                    offsetX = -20,
                    onClick = {
                        viewModel.documents.value.firstOrNull()?.let { doc ->
                            viewModel.openDocument(doc)
                        }
                    }
                )
            }

            item {
                LessonConnector(color = DuoMongiBlue, startOffset = -20, endOffset = 40)
            }

            item {
                LessonPathNodeCircle(
                    lessonNumber = 4,
                    title = "Word Batch Challenge",
                    progress = 0.50f,
                    stars = 3,
                    color = DuoMongiYellow,
                    darkColor = DuoMongiYellowDark,
                    icon = Icons.Filled.AutoAwesome,
                    offsetX = 40,
                    onClick = { viewModel.startReviewDeck("Weekly Batch") }
                )
            }

            item { Spacer(modifier = Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun LessonPathNodeCircle(
    lessonNumber: Int,
    title: String,
    progress: Float,
    stars: Int,
    color: Color,
    darkColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    offsetX: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .padding(vertical = 4.dp)
    ) {
        // Active indicator banner pointing down
        if (lessonNumber == 1) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .background(DuoMongiPurple, RoundedCornerShape(10.dp))
                    .border(2.dp, DuoMongiPurpleDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "START",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            // Curving progress indicator circle
            Canvas(modifier = Modifier.size(88.dp)) {
                drawArc(
                    color = Color(0xFFE5E5E5),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Tactile 3D Button element
            val nodeInteractionSource = remember { MutableInteractionSource() }
            val isNodePressed by nodeInteractionSource.collectIsPressedAsState()
            val nodeOffsetY by animateDpAsState(
                targetValue = if (isNodePressed) 5.dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                label = "node_press_offset"
            )

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clickable(
                        interactionSource = nodeInteractionSource,
                        indication = null,
                        onClick = onClick
                    )
            ) {
                // Background depth shadow face
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 5.dp)
                        .background(darkColor, CircleShape)
                )
                // Foreground round face
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = nodeOffsetY)
                        .background(color, CircleShape)
                        .border(2.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Lesson $lessonNumber",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = TextDark
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextGray,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(140.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            repeat(5) { idx ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (idx < stars) DuoMongiYellow else Color.LightGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
fun LessonConnector(color: Color, startOffset: Int, endOffset: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startX = size.width / 2f + startOffset.dp.toPx()
            val endX = size.width / 2f + endOffset.dp.toPx()

            // Curved/straight pathway wire matching visual direction
            drawLine(
                color = Color(0xFFE5E5E5),
                start = Offset(startX, 0f),
                end = Offset(endX, size.height),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// ==========================================
// Reusable 3D Playful Tactile Button Composable
// ==========================================

@Composable
fun Playful3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DuoMongiBlue,
    darkColor: Color = DuoMongiBlueDark,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateDpAsState(
        targetValue = if (isPressed && enabled) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "press_offset"
    )

    Box(
        modifier = modifier
            .padding(bottom = 4.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Shadow depth base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(
                    if (enabled) darkColor else Color(0xFFCCCCCC),
                    RoundedCornerShape(16.dp)
                )
        )
        // Main button face
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY)
                .background(
                    if (enabled) color else Color(0xFFE5E5E5),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.5.dp,
                    if (enabled) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

// ==========================================
// 4. Screen: Library Grid
// ==========================================

@Composable
fun LibraryScreen(viewModel: DuoMongiViewModel) {
    val docs by viewModel.documents.collectAsState()
    val savedWords by viewModel.vocabulary.collectAsState()
    var searchQueries by remember { mutableStateOf("") }

    val filteredDocs = docs.filter {
        it.title.lowercase().contains(searchQueries.lowercase())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MainAppBar(
            title = "DuoMongi",
            subtitle = "Library",
            viewModel = viewModel
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Playful Search bar with thicker outlines
            OutlinedTextField(
                value = searchQueries,
                onValueChange = { searchQueries = it },
                placeholder = { Text("Search documents...", fontWeight = FontWeight.Bold, color = TextGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DuoMongiBlue) },
                trailingIcon = {
                    if (searchQueries.isNotEmpty()) {
                        IconButton(onClick = { searchQueries = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextDark)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DuoMongiBlue,
                    unfocusedBorderColor = Color(0xFFE5E5E5),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Modern 3D Wide Import Button
            Playful3DButton(
                onClick = { viewModel.openImportNew() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_new_button"),
                color = DuoMongiBlue,
                darkColor = DuoMongiBlueDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import New",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.FindInPage,
                            contentDescription = null,
                            tint = TextGray.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "No books match your search.",
                            color = TextGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredDocs) { doc ->
                        DocumentGridCard(document = doc, onClick = { viewModel.openDocument(doc) })
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentGridCard(document: Document, onClick: () -> Unit) {
    // Select outline color based on content type for high aesthetic fidelity
    val (bgColor, borderColor, typeText) = when (document.type) {
        "Ebook" -> Triple(DuoMongiYellowLight, DuoMongiYellow, "Ebook")
        "Web Article" -> Triple(HighlightBlue, DuoMongiBlue, "Web Article")
        else -> Triple(DuoMongiOrangeLight, DuoMongiOrange, "PDF")
    }
    
    val shadowColor = DynamicColorAccent(borderColor)

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardOffsetY by animateDpAsState(
        targetValue = if (isCardPressed) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "card_press_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onClick
            )
            .padding(bottom = 6.dp)
    ) {
        // Tactile 3D base card
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 6.dp)
                .background(shadowColor, RoundedCornerShape(20.dp))
        )
        // Foreground Card Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = cardOffsetY)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(2.5.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            // Document graphic cover placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val coverIcon = when (document.coverEmoji) {
                    "📖" -> Icons.Filled.MenuBook
                    "🌐" -> Icons.Filled.Language
                    "📄" -> Icons.Filled.Article
                    "🧠" -> Icons.Filled.Psychology
                    else -> Icons.Filled.Book
                }
                Icon(
                    imageVector = coverIcon,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = document.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { document.readProgress / 100f },
                color = borderColor,
                trackColor = borderColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${document.readProgress}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextGray
                )

                // Type tag
                Box(
                    modifier = Modifier
                        .background(borderColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = borderColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stars
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(4) { idx ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (idx < document.ratingStars) DuoMongiYellow else Color.LightGray.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. Screen: Import Panel
// ==========================================

@Composable
fun ImportContentScreen(viewModel: DuoMongiViewModel, onBack: () -> Unit) {
    val recentImports by viewModel.recentImports.collectAsState()
    var showUrlDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }

    var importTitle by remember { mutableStateOf("") }
    var importBody by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(DuoMongiBg)
    ) {
        // App bar top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Import Content",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "Import Content",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Cards row: Yellow, Orange, Green
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ImportTypeCard(
                        title = "Upload PDF/Ebook",
                        color = DuoMongiYellow,
                        darkColor = DuoMongiYellowDark,
                        icon = Icons.Default.CloudUpload,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            importTitle = "Custom Paper"
                            importBody = "La lecture est essentielle pour le développement de l'apprentissage."
                            showUploadDialog = true
                        }
                    )
                    ImportTypeCard(
                        title = "Paste Web Link",
                        color = DuoMongiOrange,
                        darkColor = DuoMongiOrangeDark,
                        icon = Icons.Default.Link,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            importTitle = "France Info"
                            importBody = "Un nouvel article sur le développement de la lecture dans les écoles publiques."
                            showUrlDialog = true
                        }
                    )
                    ImportTypeCard(
                        title = "Scan Text",
                        color = DuoMongiGreen,
                        darkColor = DuoMongiGreenDark,
                        icon = Icons.Default.PhotoCamera,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            importTitle = "Scanned Page"
                            importBody = "L'esprit s'habitue à réfléchir profondément quand il enrichit ses connaissances."
                            showScanDialog = true
                        }
                    )
                }
            }

            // Wide purple banner underneath
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.importContent(
                                title = "DuoMongi: L'Importance de la Lecture",
                                type = "Ebook",
                                text = "La lecture est essentielle pour le développement de l'esprit. Elle enrichit le vocabulaire et améliore la compréhension du monde.\n\n" +
                                        "La lecture est essentielle pour le développement de l'esprit. Elle enrichit le vocabulaire et améliore la compréhension du monde.\n\n" +
                                        "La lecture est neordine, que non l'esprit. Elle enrichit le vocabulaire et améliore la comprecmension du monde."
                            )
                        }
                        .padding(bottom = 6.dp)
                ) {
                    // shadow base
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 6.dp)
                            .background(DuoMongiPurpleDark, RoundedCornerShape(20.dp))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DuoMongiPurple, RoundedCornerShape(20.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DuoMongiYellowLight, RoundedCornerShape(8.dp))
                                .border(1.dp, DynamicColorAccent(DuoMongiYellow), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null, tint = DuoMongiYellowDark)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Get Curated Texts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Mascot illustration peeking in on wide tile
                    Image(
                        painter = painterResource(id = R.drawable.duomongi_monkey_avatar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 20.dp)
                    )
                }
            }

            // Recent imports title
            item {
                Text(
                    text = "Recent Imports",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Recent Imports List
            items(recentImports) { item ->
                RecentImportRow(recent = item, onReadClick = {
                    // Match document in db & read it!
                    val doc = viewModel.documents.value.find { it.title.lowercase().contains(item.name.lowercase().substringBefore(".")) }
                    if (doc != null) {
                        viewModel.openDocument(doc)
                    } else {
                        // Launch basic
                        viewModel.documents.value.firstOrNull()?.let {
                            viewModel.openDocument(it)
                        }
                    }
                })
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Modal dialogues for action tiles
    if (showUrlDialog || showUploadDialog || showScanDialog) {
        Dialog(onDismissRequest = {
            showUrlDialog = false
            showUploadDialog = false
            showScanDialog = false
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Custom 3D Card Base
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 6.dp)
                        .background(Color(0xFFCCCCCC), RoundedCornerShape(24.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Import Custom Data",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = importTitle,
                        onValueChange = { importTitle = it },
                        label = { Text("Title", fontWeight = FontWeight.Bold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DuoMongiBlue,
                            unfocusedBorderColor = Color(0xFFE5E5E5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importBody,
                        onValueChange = { importBody = it },
                        label = { Text("Content (French)", fontWeight = FontWeight.Bold) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DuoMongiBlue,
                            unfocusedBorderColor = Color(0xFFE5E5E5)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = {
                                showUrlDialog = false
                                showUploadDialog = false
                                showScanDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = TextGray, fontWeight = FontWeight.Bold)
                        }

                        Playful3DButton(
                            onClick = {
                                viewModel.importContent(
                                    title = importTitle,
                                    type = if (showUrlDialog) "Web Article" else "PDF",
                                    text = importBody
                                )
                                showUrlDialog = false
                                showUploadDialog = false
                                showScanDialog = false
                            },
                            modifier = Modifier.weight(1.5f),
                            color = DuoMongiBlue,
                            darkColor = DuoMongiBlueDark
                        ) {
                            Text("Import", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportTypeCard(
    title: String,
    color: Color,
    darkColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "press_offset"
    )

    Box(
        modifier = modifier
            .padding(bottom = 5.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Shadow depth base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .background(darkColor, RoundedCornerShape(18.dp))
        )
        // Main button face
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY)
                .background(color, RoundedCornerShape(18.dp))
                .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun RecentImportRow(recent: RecentImport, onReadClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "press_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onReadClick
            )
            .padding(bottom = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .background(Color(0xFFE5E5E5), RoundedCornerShape(16.dp))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = offsetY)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.1.dp, Color(0xFFE5E5E5), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = DuoMongiBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = recent.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                }
            }

            if (recent.status == "Processing...") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recent.status,
                        fontSize = 13.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = DuoMongiBlue
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ready to Read",
                        fontSize = 13.sp,
                        color = DuoMongiGreen,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = DuoMongiGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. Screen: Decks List Panel
// ==========================================

@Composable
fun DecksScreen(viewModel: DuoMongiViewModel) {
    val savedWords by viewModel.vocabulary.collectAsState()

    val savedFromPdfCount = savedWords.filter { it.deckType == "Saved from PDF" }.size
    val weeklyCount = savedWords.filter { it.deckType == "Weekly Batch" }.size
    val masteredCount = savedWords.filter { it.isMastered }.size

    Column(modifier = Modifier.fillMaxSize()) {
        MainAppBar(
            title = "DuoMongi",
            subtitle = "Vocabulary Decks",
            viewModel = viewModel
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    text = "Vocabulary Decks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Banner card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 6.dp)
                            .background(Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(2.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Build Your Vocabulary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Review, rate pronunciation and master words encountered inside imported PDFs or Ebooks dynamically.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGray
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Monkey holding paper illustration
                        Image(
                            painter = painterResource(id = R.drawable.duomongi_monkey_avatar),
                            contentDescription = "Mascot",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(3.dp, DuoMongiBlue, CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Blue Deck row
            item {
                DeckCategoryCard(
                    title = "Saved from PDF",
                    count = if (savedFromPdfCount > 0) savedFromPdfCount else 125,
                    progress = 0.5f,
                    iconEmoji = "📄",
                    color = DuoMongiBlue,
                    onStudy = { viewModel.startReviewDeck("Saved from PDF") }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Yellow Deck row
            item {
                DeckCategoryCard(
                    title = "Weekly Batch",
                    count = if (weeklyCount > 0) weeklyCount else 40,
                    progress = 0.25f,
                    iconEmoji = "📅",
                    color = DuoMongiYellow,
                    onStudy = { viewModel.startReviewDeck("Weekly Batch") }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Green Deck row
            item {
                DeckCategoryCard(
                    title = "Mastered Words",
                    count = if (masteredCount > 0) masteredCount else 210,
                    progress = 0.8f,
                    iconEmoji = "🏆",
                    color = DuoMongiGreen,
                    onStudy = { viewModel.startReviewDeck("Mastered Words") }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DeckCategoryCard(
    title: String,
    count: Int,
    progress: Float,
    iconEmoji: String,
    color: Color,
    onStudy: () -> Unit
) {
    val darkColor = DynamicColorAccent(color)
    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardOffsetY by animateDpAsState(
        targetValue = if (isCardPressed) 6.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "deck_card_press"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                onClick = onStudy
            )
            .padding(bottom = 6.dp)
    ) {
        // Shadow base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 6.dp)
                .background(darkColor, RoundedCornerShape(22.dp))
        )
        // Foreground Card Body
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = cardOffsetY)
                .background(Color.White, RoundedCornerShape(22.dp))
                .border(2.5.dp, color, RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vector icon with border background
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .border(1.5.dp, color, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (iconEmoji) {
                        "📄" -> Icons.Filled.Article
                        "📅" -> Icons.Filled.DateRange
                        "🏆" -> Icons.Filled.EmojiEvents
                        else -> Icons.Filled.School
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = "$count Words",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = color,
                        trackColor = color.copy(alpha = 0.15f),
                        modifier = Modifier
                            .width(130.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Custom 3D Study Button on Card
            Box(
                modifier = Modifier
                    .padding(bottom = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 3.dp)
                        .background(color, RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .offset(y = if (isCardPressed) 3.dp else 0.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(2.dp, color, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Study",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. Screen: User Profile
// ==========================================

// ==========================================
// 7. Screen: User Profile
// ==========================================

@Composable
fun ProfileScreen(viewModel: DuoMongiViewModel) {
    val stats by viewModel.userStats.collectAsState()
    val savedWords by viewModel.vocabulary.collectAsState()

    // Fallbacks if room starts clean
    val displayLevel = stats?.level ?: 10
    val displayStreak = stats?.streakDays ?: 15
    val displayWords = if ((stats?.wordsEncountered ?: 0) > 0) stats!!.wordsEncountered else 1250
    val displayPages = if ((stats?.pagesRead ?: 0) > 0) stats!!.pagesRead else 87
    val displayAccuracy = stats?.accuracy ?: 92

    Column(modifier = Modifier.fillMaxSize()) {
        MainAppBar(
            title = "DuoMongi",
            subtitle = "User Profile and Stats",
            viewModel = viewModel
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(14.dp)) }

            // Profile info box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 6.dp)
                            .background(Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(2.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.duomongi_monkey_avatar),
                                contentDescription = "User avatar",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, DuoMongiBlue, CircleShape)
                                    .background(Color.White)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "DuoMongi",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark
                                )
                                Text(
                                    text = "Level $displayLevel",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGray
                                )
                            }
                        }

                        // Streak
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(DuoMongiOrangeLight)
                                .border(1.5.dp, DuoMongiOrange, RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Whatshot,
                                contentDescription = "Streak",
                                tint = DuoMongiOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Streak:",
                                    fontSize = 10.sp,
                                    color = DuoMongiOrangeDark,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 12.sp
                                )
                                Text(
                                    text = "$displayStreak Days",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DuoMongiOrangeDark,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Statistics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
                )
            }

            // Stats cards grid matches Image 6 grid colors: Blue, Red, Green
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxCard(
                        title = "Words Encountered",
                        value = "$displayWords",
                        iconEmoji = "📚",
                        color = DuoMongiBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxCard(
                        title = "Pages Read",
                        value = "$displayPages",
                        iconEmoji = "📖",
                        color = DuoMongiOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxCard(
                        title = "Accuracy in Games",
                        value = "$displayAccuracy%",
                        iconEmoji = "🎯",
                        color = DuoMongiGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = "Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)
                )
            }

            // Achievements List
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 6.dp)
                            .background(Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(2.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AchievementBadge(emojiString = "📚👓", title = "Bookworm")
                        AchievementBadge(emojiString = "👑🔤", title = "Grammar King")
                        AchievementBadge(emojiString = "🎙️⭐", title = "Fluency Master")
                        AchievementBadge(emojiString = "🧩💡", title = "Game Guru")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun StatBoxCard(
    title: String,
    value: String,
    iconEmoji: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val darkColor = DynamicColorAccent(color)
    Box(
        modifier = modifier
            .padding(bottom = 5.dp)
    ) {
        // Shadow base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 5.dp)
                .background(darkColor, RoundedCornerShape(16.dp))
        )
        // Foreground Card face
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, color, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            val icon = when (iconEmoji) {
                "📚" -> Icons.Filled.MenuBook
                "📖" -> Icons.Filled.AutoStories
                "🎯" -> Icons.Filled.TaskAlt
                else -> Icons.Filled.BarChart
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextGray,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AchievementBadge(emojiString: String, title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(DuoMongiBg, CircleShape)
                .border(2.5.dp, DuoMongiBlue.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (title) {
                "Bookworm" -> Icons.Filled.LocalLibrary
                "Grammar King" -> Icons.Filled.WorkspacePremium
                "Fluency Master" -> Icons.Filled.RecordVoiceOver
                "Game Guru" -> Icons.Filled.Extension
                else -> Icons.Filled.Star
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DuoMongiBlue,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = TextDark,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// 8. Screen: Interactive Reading View
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingScreen(
    document: Document,
    viewModel: DuoMongiViewModel,
    onBack: () -> Unit
) {
    val selectedWord by viewModel.selectedWord.collectAsState()
    val wordsStack by viewModel.vocabulary.collectAsState()
    val savedCount = if (wordsStack.isNotEmpty()) wordsStack.size else 15

    // Highlight triggers List
    val highlightWordsToTrack = document.highlightWords.split(",")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(DuoMongiBg)
    ) {
        // App top control bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = document.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp)
                )
            }

            WordCountBadge(savedCount = savedCount)
        }

        // Standard card containing interactive text margins
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 6.dp)
        ) {
            // Shadow base
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 6.dp)
                    .background(Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
            )
            // Foreground paper sheet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(2.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                val fileWords = document.content.split(Regex("(?<=\\b)|(?=\\b)"))

                // Custom wrapping text representing words flowing safely
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            fileWords.forEach { item ->
                                val cleanWord = item.lowercase().trim().removeSuffix(",").removeSuffix(".")
                                val isHighlighted = highlightWordsToTrack.contains(cleanWord)

                                if (item.trim().isEmpty()) {
                                    Text(text = item, fontSize = 22.sp, lineHeight = 34.sp)
                                } else if (isHighlighted) {
                                    // Select background shade matching the colors in the preview
                                    val (tagBg, tagBorder, textThemeColor) = when (cleanWord) {
                                        "essentielle", "enrichit", "améliore" -> Triple(DuoMongiGreenLight, DuoMongiGreen, DuoMongiGreenDark)
                                        "le développement", "développement", "compréhension" -> Triple(HighlightBlue, DuoMongiBlue, DuoMongiBlueDark)
                                        else -> Triple(DuoMongiYellowLight, DuoMongiYellow, DuoMongiYellowDark)
                                    }

                                    val isWordSelected = selectedWord?.word?.lowercase() == cleanWord

                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                            .clickable { viewModel.selectWord(cleanWord) }
                                            .padding(bottom = 4.dp)
                                    ) {
                                        // shadow
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .offset(y = 4.dp)
                                                .background(
                                                    if (isWordSelected) DuoMongiYellowDark else tagBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isWordSelected) DuoMongiYellowLight else tagBg,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.5.dp,
                                                    if (isWordSelected) DuoMongiYellow else Color.White.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isWordSelected) DuoMongiYellowDark else textThemeColor,
                                                lineHeight = 34.sp
                                            )
                                        }
                                    }
                                } else {
                                    // Regular word
                                    Text(
                                        text = item,
                                        fontSize = 22.sp,
                                        color = TextDark,
                                        lineHeight = 34.sp,
                                        modifier = Modifier
                                            .clickable { viewModel.selectWord(cleanWord) }
                                            .padding(horizontal = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Context Tip floating panel + dynamic actions at the bottom
        AnimatedVisibility(
            visible = (selectedWord != null),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            selectedWord?.let { activeWord ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .border(
                            2.5.dp,
                            Color(0xFFE5E5E5),
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Word highlight banner inside tip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(DuoMongiGreenLight, RoundedCornerShape(8.dp))
                                .border(1.5.dp, DuoMongiGreen, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = activeWord.word,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = DuoMongiGreenDark
                            )
                        }
                        Text(text = activeWord.ipa, fontSize = 15.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearSelectedWord() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextGray)
                        }
                    }

                    // Speech bubble container layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.duomongi_monkey_avatar),
                            contentDescription = "Mascot Tip",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(3.dp, DuoMongiBlue, CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(DuoMongiBg, RoundedCornerShape(18.dp))
                                .border(2.dp, Color(0xFFE5E5E5), RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                            append("Context Tip: ")
                                        }
                                        append(activeWord.contextTip)
                                    },
                                    fontSize = 13.sp,
                                    color = TextDark,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Translation: ${activeWord.translation}",
                                    fontSize = 12.sp,
                                    color = DuoMongiBlueDark,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Add Word to Deck chunky 3D button
                    Playful3DButton(
                        onClick = { viewModel.addWordToDeck(activeWord) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        color = DuoMongiGreen,
                        darkColor = DuoMongiGreenDark
                    ) {
                        Text(
                            "Add Word to Deck",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Next Page chunky 3D button
                    Playful3DButton(
                        onClick = { viewModel.clearSelectedWord() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        color = Color.White,
                        darkColor = Color(0xFFE5E5E5)
                    ) {
                        Text(
                            "Next Page",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = DuoMongiBlue
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}

// ==========================================
// 10. Screen: Word Batch Review Quiz
// ==========================================

@Composable
fun WordBatchReviewScreen(
    deckType: String,
    viewModel: DuoMongiViewModel,
    onBack: () -> Unit
) {
    val savedWords by viewModel.vocabulary.collectAsState()
    
    // Filter actual words by deck type, or load fallback vocabulary words to guarantee an amazing study experience
    val actualWordsToReview = savedWords.filter { it.deckType == deckType }.take(5)
    val wordsToReview = if (actualWordsToReview.isNotEmpty()) {
        actualWordsToReview
    } else {
        listOf(
            VocabularyWord(word = "enrichir", ipa = "/ɑ̃.ʁi.ʃiʁ/", translation = "To enrich", sentence = "La lecture contribue à enrichir notre esprit.", contextTip = "Common French Verb"),
            VocabularyWord(word = "apprendre", ipa = "/a.pʁɑ̃dʁ/", translation = "To learn", sentence = "Il veut apprendre le français pas à pas.", contextTip = "Core Language Verb"),
            VocabularyWord(word = "voyouter", ipa = "/vwa.ju.te/", translation = "To play the rogue", sentence = "Il passe ses journées à voyouter dans la rue.", contextTip = "Colloquial Term")
        )
    }

    var currentWordIndex by remember { mutableStateOf(0) }
    var selectedChoiceIndex by remember { mutableStateOf<Int?>(null) }
    var answerChecked by remember { mutableStateOf(false) }
    var isCorrectAnswer by remember { mutableStateOf(false) }
    var quizCompleted by remember { mutableStateOf(false) }
    var soundClickAlert by remember { mutableStateOf(false) }

    // Option translations generated and shuffled on current word index change
    var optionTranslations by remember { mutableStateOf<List<String>>(emptyList()) }

    val currentWord = wordsToReview.getOrNull(currentWordIndex)

    LaunchedEffect(currentWordIndex, currentWord) {
        if (currentWord != null) {
            val correct = currentWord.translation
            val savedFillers = savedWords.filter { it.translation.lowercase() != correct.lowercase() }
                .map { it.translation }
                .distinct()
            
            val staticFillers = listOf(
                "To understand grammar rules",
                "Deep thoughtful reflection",
                "Excellent language learner",
                "Interactive learning method",
                "To practice conversation elements"
            )
            val pool = (savedFillers + staticFillers).filter { it.lowercase() != correct.lowercase() }.shuffled()
            val fillers = pool.take(2)
            optionTranslations = (listOf(correct) + fillers).distinct().take(3).shuffled()
            
            selectedChoiceIndex = null
            answerChecked = false
            isCorrectAnswer = false
        }
    }

    if (quizCompleted) {
        // CELEBRATION RESULT PANEL
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DuoMongiBg)
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(DuoMongiYellowLight, CircleShape)
                    .border(3.dp, DuoMongiYellow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Success",
                    tint = DuoMongiYellow,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Lesson Complete!",
                fontSize = 28.sp,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Black,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Awesome Job! You reviewed critical language vocabulary and unlocked deep streak progression levels.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dual 3D stat blocks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 4.dp)
                            .background(DuoMongiBlueDark, RoundedCornerShape(16.dp))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighlightBlue, RoundedCornerShape(16.dp))
                            .border(2.dp, DuoMongiBlue, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "XP EARNED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoMongiBlueDark,
                            fontFamily = NunitoFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+10 XP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = DuoMongiBlueDark,
                            fontFamily = NunitoFontFamily
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 4.dp)
                            .background(DuoMongiOrangeDark, RoundedCornerShape(16.dp))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DuoMongiOrangeLight, RoundedCornerShape(16.dp))
                            .border(2.2.dp, DuoMongiOrange, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STREAK BOOST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DuoMongiOrangeDark,
                            fontFamily = NunitoFontFamily
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+1 Day",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = DuoMongiOrangeDark,
                            fontFamily = NunitoFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Playful3DButton(
                onClick = { viewModel.saveAndCreateStudyPlan() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_and_plan_button"),
                color = DuoMongiGreen,
                darkColor = DuoMongiGreenDark
            ) {
                Text(
                    text = "CONTINUE",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontFamily = NunitoFontFamily
                )
            }
        }
    } else if (currentWord != null) {
        // ACTIVE STUDY CHALLENGE LAYOUT
        Scaffold(
            bottomBar = {
                // Persistent Slide-up validation sheets matching Duolingo
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (answerChecked) (if (isCorrectAnswer) DuoMongiGreenLight else DuoMongiOrangeLight) else Color.White)
                        .border(
                            width = if (answerChecked) 2.5.dp else 0.dp,
                            color = if (answerChecked) (if (isCorrectAnswer) DuoMongiGreen else DuoMongiOrange) else Color.Transparent,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(20.dp)
                        .navigationBarsPadding()
                ) {
                    if (answerChecked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(if (isCorrectAnswer) DuoMongiGreen else DuoMongiOrange, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCorrectAnswer) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isCorrectAnswer) "Excellent!" else "Correct translation:",
                                    fontSize = 18.sp,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCorrectAnswer) DuoMongiGreenDark else DuoMongiOrangeDark
                                )
                                Text(
                                    text = if (isCorrectAnswer) "You got it right!" else currentWord.translation,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrectAnswer) DuoMongiGreenDark.copy(alpha = 0.8f) else DuoMongiOrangeDark.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Next Question Continue details
                        Playful3DButton(
                            onClick = {
                                if (currentWordIndex + 1 < wordsToReview.size) {
                                    currentWordIndex++
                                } else {
                                    quizCompleted = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            color = if (isCorrectAnswer) DuoMongiGreen else DuoMongiOrange,
                            darkColor = if (isCorrectAnswer) DuoMongiGreenDark else DuoMongiOrangeDark
                        ) {
                            Text(
                                text = "CONTINUE",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontFamily = NunitoFontFamily
                            )
                        }
                    } else {
                        // Action verification details
                        Playful3DButton(
                            onClick = {
                                if (selectedChoiceIndex != null) {
                                    val choiceStr = optionTranslations.getOrNull(selectedChoiceIndex!!)
                                    isCorrectAnswer = choiceStr?.lowercase() == currentWord.translation.lowercase()
                                    answerChecked = true
                                }
                            },
                            enabled = selectedChoiceIndex != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            color = if (selectedChoiceIndex != null) DuoMongiGreen else Color(0xFFE5E5E5),
                            darkColor = if (selectedChoiceIndex != null) DuoMongiGreenDark else Color(0xFFCCCCCC)
                        ) {
                            Text(
                                text = "CHECK",
                                fontSize = 16.sp,
                                color = if (selectedChoiceIndex != null) Color.White else TextGray,
                                fontWeight = FontWeight.Black,
                                fontFamily = NunitoFontFamily
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DuoMongiBg)
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PROGRESS LOADER TOP HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel study session",
                            tint = TextGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Green Horizontal Loading capsule
                    val progressFraction = (currentWordIndex).toFloat() / wordsToReview.size.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .background(Color(0xFFE5E5E5), CircleShape)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (progressFraction <= 0f) 0.05f else progressFraction)
                                .background(DuoMongiGreen, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Heart Counter Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Session health lives",
                            tint = DuoMongiPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "5",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = DuoMongiPurpleDark,
                            fontFamily = NunitoFontFamily
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Select the correct translation",
                            fontSize = 20.sp,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }

                    // Word Flash Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            // 3D Shadow
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(y = 5.dp)
                                    .background(Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
                            )
                            // White interactive Card Face
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .border(2.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DuoMongiBlue.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = currentWord.contextTip,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DuoMongiBlue
                                        )
                                    }

                                    IconButton(
                                        onClick = { soundClickAlert = true },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(HighlightBlue)
                                            .border(1.5.dp, DuoMongiBlue.copy(alpha = 0.4f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Synthesize TTS audio",
                                            tint = DuoMongiBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = currentWord.word,
                                    fontSize = 32.sp,
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark
                                )

                                Text(
                                    text = currentWord.ipa,
                                    fontSize = 14.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = NunitoFontFamily
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "\"${currentWord.sentence}\"",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontStyle = FontStyle.Italic,
                                        color = TextDark,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Tactical Multiple Choice Card Panels
                    itemsIndexed(optionTranslations) { idx, text ->
                        val isSelected = selectedChoiceIndex == idx
                        val selectedAndWrong = isSelected && answerChecked && !isCorrectAnswer
                        val isCorrect = text.lowercase() == currentWord.translation.lowercase()

                        val borderColor = when {
                            answerChecked && isCorrect -> DuoMongiGreen
                            selectedAndWrong -> DuoMongiOrange
                            isSelected -> DuoMongiBlue
                            else -> Color(0xFFE5E5E5)
                        }
                        val shadowColor = when {
                            answerChecked && isCorrect -> DuoMongiGreenDark
                            selectedAndWrong -> DuoMongiOrangeDark
                            isSelected -> DuoMongiBlueDark
                            else -> Color(0xFFCCCCCC)
                        }
                        val cardBgColor = when {
                            answerChecked && isCorrect -> DuoMongiGreenLight
                            selectedAndWrong -> DuoMongiOrangeLight
                            isSelected -> HighlightBlue
                            else -> Color.White
                        }

                        val optInteractionSource = remember { MutableInteractionSource() }
                        val isOptPressed by optInteractionSource.collectIsPressedAsState()
                        val choiceOffsetY by animateDpAsState(
                            targetValue = if (isOptPressed || isSelected) 4.dp else 0.dp,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
                            label = "choice_press"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = !answerChecked,
                                    interactionSource = optInteractionSource,
                                    indication = null,
                                    onClick = { selectedChoiceIndex = idx }
                                )
                                .padding(bottom = 6.dp)
                        ) {
                            // 3D Shadow Base
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(y = 4.dp)
                                    .background(shadowColor, RoundedCornerShape(16.dp))
                            )
                            // Card Body Face
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = choiceOffsetY)
                                    .background(cardBgColor, RoundedCornerShape(16.dp))
                                    .border(2.5.dp, borderColor, RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Keyboard index key look
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (isSelected) DuoMongiBlue else Color.White,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isSelected) Color.White.copy(alpha = 0.5f) else Color(0xFFE5E5E5),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else TextGray,
                                        fontFamily = NunitoFontFamily
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected && !answerChecked) DuoMongiBlueDark else TextDark,
                                    modifier = Modifier.weight(1f)
                                )

                                if (answerChecked && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = DuoMongiGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else if (selectedAndWrong) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Wrong",
                                        tint = DuoMongiOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (soundClickAlert) {
        Dialog(onDismissRequest = { soundClickAlert = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Custom 3D Card Base
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 6.dp)
                        .background(Color(0xFFCCCCCC), RoundedCornerShape(24.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.5.dp, Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = DuoMongiBlue,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Speaking Aloud...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        fontFamily = NunitoFontFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Audio synthesized standard Latin/Spanish accents accurately.",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = NunitoFontFamily
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(onClick = { soundClickAlert = false }) {
                        Text("OK", color = DuoMongiBlue, fontWeight = FontWeight.Bold, fontFamily = NunitoFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
fun VocabularyReviewCard(vocab: VocabularyWord, onSpeakerClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        // Shadow base
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 6.dp)
                .background(Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
        )
        // Card face
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFFE5E5E5), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Highlights tag style
                    Box(
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(y = 3.dp)
                                .background(DuoMongiGreenDark, RoundedCornerShape(8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .background(DuoMongiGreenLight, RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = vocab.word,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = DuoMongiGreenDark
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = vocab.ipa, fontSize = 15.sp, color = TextGray, fontWeight = FontWeight.Black)
                }

                // Custom speaker button
                Box(
                    modifier = Modifier
                        .clickable(onClick = onSpeakerClick)
                        .padding(bottom = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 3.dp)
                            .background(DuoMongiBlueDark, RoundedCornerShape(10.dp))
                    )
                    Box(
                        modifier = Modifier
                            .background(DuoMongiBlue, RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Context sentence with bold word
            val parts = vocab.sentence.split(Regex("(?i)\\b${vocab.word}\\b"))
            Text(
                text = buildAnnotatedString {
                    parts.forEachIndexed { idx, slice ->
                        append(slice)
                        if (idx < parts.size - 1) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                                append(vocab.word)
                            }
                        }
                    }
                },
                fontSize = 15.sp,
                color = TextDark,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Meaning: ${vocab.translation}",
                fontSize = 14.sp,
                color = DuoMongiBlueDark,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// Accent border mapper
fun DynamicColorAccent(color: Color): Color {
    return when (color) {
        DuoMongiYellow -> DuoMongiYellowDark
        DuoMongiOrange -> DuoMongiOrangeDark
        DuoMongiGreen -> DuoMongiGreenDark
        DuoMongiPurple -> DuoMongiPurpleDark
        else -> DuoMongiBlueDark
    }
}
