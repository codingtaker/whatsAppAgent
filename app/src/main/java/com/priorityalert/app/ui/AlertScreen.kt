package com.priorityalert.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class AlertInfo(val contact: String, val message: String, val receivedAt: Long)

@Composable
fun AlertScreen(info: AlertInfo, onOpenWhatsApp: () -> Unit, onStop: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            now = System.currentTimeMillis()
        }
    }
    val minutes = ((now - info.receivedAt) / 60_000L).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3A1D33), Navy, NavyDeep))),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            RingingLabel()
            Spacer(Modifier.weight(1f))
            PulsingAvatar(info.contact)
            Spacer(Modifier.height(28.dp))
            Text(
                info.contact,
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (minutes < 1) "vous a écrit il y a moins d'une minute" else "vous a écrit il y a $minutes min",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                "et n'a pas encore eu de réponse",
                color = Amber,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (info.message.isNotBlank()) {
                Spacer(Modifier.height(28.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                ) {
                    Text(
                        // Non-breaking spaces keep the quote marks attached to the text.
                        "« ${info.message.trim()} »",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onOpenWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Répondre sur WhatsApp", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onStop,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Arrêter l'alerte", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** "MESSAGE IMPORTANT" pill with a bell that rocks like it is ringing. */
@Composable
private fun RingingLabel() {
    val transition = rememberInfiniteTransition(label = "bell")
    val angle by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
        label = "angle",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            Icons.Filled.Notifications,
            contentDescription = null,
            tint = Coral,
            modifier = Modifier.size(18.dp).rotate(angle),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "MESSAGE IMPORTANT",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
    }
}

/** Contact avatar with coral rings radiating outwards, like an incoming call. */
@Composable
private fun PulsingAvatar(name: String) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "phase",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val base = size.minDimension / 2 * 0.53f
            for (k in 0 until 3) {
                val p = (phase + k / 3f) % 1f
                drawCircle(
                    color = Coral.copy(alpha = (1f - p) * 0.45f),
                    radius = base * (1f + p * 0.85f),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            drawCircle(color = Coral.copy(alpha = 0.18f), radius = base * 1.12f)
        }
        Avatar(name.ifBlank { "?" }, size = 120.dp)
    }
}
