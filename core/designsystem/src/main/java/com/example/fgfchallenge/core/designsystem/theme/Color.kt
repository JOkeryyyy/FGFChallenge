package com.example.fgfchallenge.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Neutral slate blue-gray scheme. Color is reserved for severity and density (see
 * [SeverityColors]), so the surrounding chrome stays deliberately desaturated.
 *
 * Every role a component in this module renders is defined explicitly — including the ones
 * Material 3 pulls implicitly (AlertDialog -> surfaceContainerHigh, ModalBottomSheet ->
 * surfaceContainerLow, OutlinedTextField focus -> primary, elevation overlay -> surfaceTint).
 * Leaving those undefined falls back to M3's purple-tinted baseline.
 */

private val LightPrimary = Color(0xFF2C4A5E)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFD3E3EF)
private val LightOnPrimaryContainer = Color(0xFF12303F)
private val LightSecondary = Color(0xFF4C5F6B)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFDCE5EA)
private val LightOnSecondaryContainer = Color(0xFF17262E)
private val LightTertiary = Color(0xFF56606A)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightBackground = Color(0xFFF8F9FA)
private val LightOnBackground = Color(0xFF1A1C1E)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF1A1C1E)
private val LightSurfaceVariant = Color(0xFFEEF1F3)
private val LightOnSurfaceVariant = Color(0xFF434A4F)
private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainerLow = Color(0xFFF7F9FA)
private val LightSurfaceContainer = Color(0xFFF1F4F6)
private val LightSurfaceContainerHigh = Color(0xFFEBEFF1)
private val LightSurfaceContainerHighest = Color(0xFFE5EAEC)
private val LightOutline = Color(0xFF71787D)
private val LightOutlineVariant = Color(0xFFB4BCC2)
private val LightError = Color(0xFFD32F2F)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFCDAD8)
private val LightOnErrorContainer = Color(0xFF5C1414)
private val LightInverseSurface = Color(0xFF2E3235)
private val LightInverseOnSurface = Color(0xFFF0F2F4)

private val DarkPrimary = Color(0xFFA3C7DE)
private val DarkOnPrimary = Color(0xFF12303F)
private val DarkPrimaryContainer = Color(0xFF294657)
private val DarkOnPrimaryContainer = Color(0xFFD3E3EF)
private val DarkSecondary = Color(0xFFB6C6CF)
private val DarkOnSecondary = Color(0xFF21323B)
private val DarkSecondaryContainer = Color(0xFF374852)
private val DarkOnSecondaryContainer = Color(0xFFDCE5EA)
private val DarkTertiary = Color(0xFFBEC7CE)
private val DarkOnTertiary = Color(0xFF29323A)
private val DarkBackground = Color(0xFF101416)
private val DarkOnBackground = Color(0xFFE1E4E6)
private val DarkSurface = Color(0xFF181C1E)
private val DarkOnSurface = Color(0xFFE1E4E6)
private val DarkSurfaceVariant = Color(0xFF262B2E)
private val DarkOnSurfaceVariant = Color(0xFFBEC6CB)
private val DarkSurfaceContainerLowest = Color(0xFF0B0F11)
private val DarkSurfaceContainerLow = Color(0xFF161A1C)
private val DarkSurfaceContainer = Color(0xFF1C2123)
private val DarkSurfaceContainerHigh = Color(0xFF262B2E)
private val DarkSurfaceContainerHighest = Color(0xFF313639)
private val DarkOutline = Color(0xFF8B9297)
private val DarkOutlineVariant = Color(0xFF3F464A)
private val DarkError = Color(0xFFEF6C6C)
private val DarkOnError = Color(0xFF5C1414)
private val DarkErrorContainer = Color(0xFF7A1E1E)
private val DarkOnErrorContainer = Color(0xFFFCDAD8)
private val DarkInverseSurface = Color(0xFFE1E4E6)
private val DarkInverseOnSurface = Color(0xFF2E3235)

internal val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkOnPrimary,
        primaryContainer = DarkPrimaryContainer,
        onPrimaryContainer = DarkOnPrimaryContainer,
        secondary = DarkSecondary,
        onSecondary = DarkOnSecondary,
        secondaryContainer = DarkSecondaryContainer,
        onSecondaryContainer = DarkOnSecondaryContainer,
        tertiary = DarkTertiary,
        onTertiary = DarkOnTertiary,
        background = DarkBackground,
        onBackground = DarkOnBackground,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        surfaceContainerLowest = DarkSurfaceContainerLowest,
        surfaceContainerLow = DarkSurfaceContainerLow,
        surfaceContainer = DarkSurfaceContainer,
        surfaceContainerHigh = DarkSurfaceContainerHigh,
        surfaceContainerHighest = DarkSurfaceContainerHighest,
        surfaceTint = DarkPrimary,
        outline = DarkOutline,
        outlineVariant = DarkOutlineVariant,
        error = DarkError,
        onError = DarkOnError,
        errorContainer = DarkErrorContainer,
        onErrorContainer = DarkOnErrorContainer,
        inverseSurface = DarkInverseSurface,
        inverseOnSurface = DarkInverseOnSurface,
    )

internal val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightOnPrimary,
        primaryContainer = LightPrimaryContainer,
        onPrimaryContainer = LightOnPrimaryContainer,
        secondary = LightSecondary,
        onSecondary = LightOnSecondary,
        secondaryContainer = LightSecondaryContainer,
        onSecondaryContainer = LightOnSecondaryContainer,
        tertiary = LightTertiary,
        onTertiary = LightOnTertiary,
        background = LightBackground,
        onBackground = LightOnBackground,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceContainerLowest = LightSurfaceContainerLowest,
        surfaceContainerLow = LightSurfaceContainerLow,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceContainerHigh,
        surfaceContainerHighest = LightSurfaceContainerHighest,
        surfaceTint = LightPrimary,
        outline = LightOutline,
        outlineVariant = LightOutlineVariant,
        error = LightError,
        onError = LightOnError,
        errorContainer = LightErrorContainer,
        onErrorContainer = LightOnErrorContainer,
        inverseSurface = LightInverseSurface,
        inverseOnSurface = LightInverseOnSurface,
    )
