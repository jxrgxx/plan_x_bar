package com.los_jorges.plan_bar.ui.theme

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun premiumInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Platinum40.copy(alpha = 0.70f),
    unfocusedBorderColor = DarkSurface4,
    focusedLabelColor = Platinum40.copy(alpha = 0.90f),
    unfocusedLabelColor = WarmMuted,
    cursorColor = Platinum40,
    focusedTextColor = WarmWhite,
    unfocusedTextColor = WarmWhite,
    focusedContainerColor = DarkSurface2,
    unfocusedContainerColor = DarkSurface2,
    errorBorderColor = Color(0xFFC0574A),
    errorLabelColor = Color(0xFFC0574A),
    errorTextColor = WarmWhite,
    errorContainerColor = DarkSurface2,
)
