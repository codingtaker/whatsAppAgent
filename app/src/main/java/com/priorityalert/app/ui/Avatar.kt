package com.priorityalert.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val AvatarColors = listOf(
    Coral,
    Color(0xFF6366F1),
    Color(0xFF14B8A6),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF0EA5E9),
    Color(0xFF8B5CF6),
    Color(0xFF10B981),
)

fun avatarColor(name: String): Color = AvatarColors[abs(name.trim().lowercase().hashCode()) % AvatarColors.size]

/** Up to two initials, skipping emoji and symbols ("Gisela 🇬🇦" → "G"). */
fun initials(name: String): String {
    val letters = name.split(Regex("\\s+"))
        .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() }
        .take(2)
    return if (letters.isEmpty()) "?" else letters.joinToString("")
}

@Composable
fun Avatar(name: String, size: Dp, modifier: Modifier = Modifier, color: Color = avatarColor(name)) {
    Box(
        modifier = modifier.size(size).background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(name),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
        )
    }
}
