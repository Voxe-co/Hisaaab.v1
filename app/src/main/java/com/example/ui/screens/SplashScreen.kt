package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RichBlack
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueLight
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    val opacity = remember { Animatable(0.0f) }

    LaunchedEffect(key1 = true) {
        // Run parallel animations
        delay(100)
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1000, easing = { t ->
                // Custom elastic out easing
                val s = 1.70158f
                val p = t - 1.0f
                p * p * ((s + 1f) * p + s) + 1f
            })
        )
    }

    LaunchedEffect(key1 = true) {
        opacity.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 800)
        )
        // Keep splash active for standard preview, then transition
        delay(1600)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RichBlack), // Force rich black for splash to set premium tone
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .scale(scale.value)
                .alpha(opacity.value)
        ) {
            // High-fidelity custom drawing instead of standard icon to give a beautiful branded signature
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Drawing an elegant "H" intertwined logo with Royal Blue gradients
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Dynamic background soft glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(RoyalBlue.copy(alpha = 0.4f), Color.Transparent),
                            center = center,
                            radius = width * 0.8f
                        ),
                        radius = width * 0.8f
                    )

                    // Left vertical stem of "H"
                    drawLine(
                        brush = Brush.verticalGradient(listOf(RoyalBlue, RoyalBlueLight)),
                        start = androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.15f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.85f),
                        strokeWidth = 12.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Right vertical stem of "H"
                    drawLine(
                        brush = Brush.verticalGradient(listOf(RoyalBlue, RoyalBlueLight)),
                        start = androidx.compose.ui.geometry.Offset(width * 0.7f, height * 0.15f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.7f, height * 0.85f),
                        strokeWidth = 12.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Crossbar of "H"
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(RoyalBlue, RoyalBlueLight)),
                        start = androidx.compose.ui.geometry.Offset(width * 0.3f, height * 0.5f),
                        end = androidx.compose.ui.geometry.Offset(width * 0.7f, height * 0.5f),
                        strokeWidth = 10.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Orbit ring illustrating rotation/infinite wealth cycle
                    drawArc(
                        brush = Brush.linearGradient(listOf(RoyalBlueLight, Color.Transparent)),
                        startAngle = -45f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.1f),
                        size = androidx.compose.ui.geometry.Size(width * 0.8f, height * 0.8f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HISAAB",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Premium Lending Intelligence",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    lineHeight = 22.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }

        // Elegant minimal version text at bottom
        Text(
            text = "FOUNDATION v1.0",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        )
    }
}
