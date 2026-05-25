package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Detect if running under JVM Robolectric unit tests to avoid asset loading failures
private val isUnderTest: Boolean by lazy {
    try {
        Class.forName("org.robolectric.Robolectric") != null
    } catch (e: ClassNotFoundException) {
        false
    }
}

// Define the rounded Nunito font family reminiscent of Duolingo Custom Sans with safe test fallback
val NunitoFontFamily = if (isUnderTest) {
    FontFamily.SansSerif
} else {
    try {
        FontFamily(
            Font(resId = R.font.nunito_regular, weight = FontWeight.Normal),
            Font(resId = R.font.nunito_bold, weight = FontWeight.Bold),
            Font(resId = R.font.nunito_black, weight = FontWeight.Black)
        )
    } catch (e: Throwable) {
        FontFamily.SansSerif
    }
}

// Set of Material typography styles mapped to Nunito
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp
    ),
    displaySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
)

