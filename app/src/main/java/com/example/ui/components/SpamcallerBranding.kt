package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.remote.SpamVerdict
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricAzure
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RoyalCobalt
import com.example.ui.theme.SafeEmerald
import com.example.ui.theme.SpamCrimson
import com.example.ui.theme.SuspectAmber

/**
 * Custom vector-drawn Spamcaller logo: a white phone handset crossed out
 * inside a crimson prohibition ("divieto di accesso") ring on a black-blue circle.
 */
@Composable
fun SpamcallerProhibitionLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(RoyalCobalt, ObsidianBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.10f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = w * 0.11f

            // White handset silhouette tilted diagonally
            val handsetPath = Path().apply {
                moveTo(w * 0.28f, h * 0.34f)
                cubicTo(w * 0.20f, h * 0.42f, w * 0.25f, h * 0.56f, w * 0.36f, h * 0.66f)
                cubicTo(w * 0.46f, h * 0.76f, w * 0.60f, h * 0.80f, w * 0.68f, h * 0.72f)
                lineTo(w * 0.74f, h * 0.65f)
                cubicTo(w * 0.77f, h * 0.62f, w * 0.75f, h * 0.57f, w * 0.70f, h * 0.54f)
                lineTo(w * 0.62f, h * 0.50f)
                lineTo(w * 0.57f, h * 0.55f)
                cubicTo(w * 0.52f, h * 0.53f, w * 0.47f, h * 0.48f, w * 0.45f, h * 0.43f)
                lineTo(w * 0.50f, h * 0.38f)
                lineTo(w * 0.46f, h * 0.30f)
                cubicTo(w * 0.43f, h * 0.25f, w * 0.38f, h * 0.23f, w * 0.35f, h * 0.26f)
                close()
            }
            drawPath(
                path = handsetPath,
                color = Color.White,
                style = Stroke(width = w * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = handsetPath,
                color = Color.White
            )

            // Crimson prohibition circle + diagonal slash
            drawCircle(
                color = SpamCrimson,
                radius = w * 0.46f,
                style = Stroke(width = strokeW)
            )
            drawLine(
                color = SpamCrimson,
                start = Offset(w * 0.18f, h * 0.82f),
                end = Offset(w * 0.82f, h * 0.18f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ContactOrCallerAvatar(
    name: String,
    photoUri: String?,
    isSpam: Boolean = false,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val initials = name
        .trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    val bgBrush = if (isSpam) {
        Brush.linearGradient(listOf(Color(0xFF450A0A), SpamCrimson))
    } else {
        Brush.linearGradient(listOf(ObsidianBlack, DeepNavy, ElectricAzure))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                width = 1.5.dp,
                color = if (isSpam) SpamCrimson.copy(alpha = 0.7f) else LuminousCyan.copy(alpha = 0.45f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank() && !isSpam) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto contatto $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = if (isSpam) "!" else initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SpamVerdictBadge(
    verdict: SpamVerdict,
    score: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text) = when (verdict) {
        SpamVerdict.HIGH_RISK -> Triple(
            SpamCrimson.copy(alpha = 0.18f),
            SpamCrimson,
            "SPAM $score%"
        )
        SpamVerdict.SUSPICIOUS -> Triple(
            SuspectAmber.copy(alpha = 0.18f),
            SuspectAmber,
            "Sospetto $score%"
        )
        SpamVerdict.SAFE -> Triple(
            SafeEmerald.copy(alpha = 0.18f),
            SafeEmerald,
            "Sicuro"
        )
        SpamVerdict.UNKNOWN -> Triple(
            ElectricAzure.copy(alpha = 0.15f),
            LuminousCyan,
            "Non segnalato"
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BrandedTopHeader(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpamcallerProhibitionLogo(size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
