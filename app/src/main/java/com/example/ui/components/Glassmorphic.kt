package com.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueLight

@Composable
fun HisaabCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "card_scale"
    )

    val view = LocalView.current

    val cardModifier = modifier
        .scale(scale)
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.1f),
            spotColor = Color.Black.copy(alpha = 0.2f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.1f),
                    RoyalBlue.copy(alpha = 0.15f)
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
        .background(backgroundColor)
        .run {
            if (onClick != null) {
                this.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            } catch (e: Exception) {}
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            } else {
                this
            }
        }
        .padding(20.dp)

    Column(
        modifier = cardModifier,
        content = content
    )
}

@Composable
fun HisaabButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = RoyalBlue,
    contentColor: Color = Color.White
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "btn_scale"
    )

    val view = LocalView.current

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(containerColor, containerColor.copy(alpha = 0.85f)))
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f).asBrush()
                }
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        } catch (e: Exception) {}
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(vertical = 16.dp, horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisaabTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RoyalBlue,
            focusedLabelColor = RoyalBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// Utility extension
private fun Color.asBrush(): Brush = Brush.linearGradient(listOf(this, this))
