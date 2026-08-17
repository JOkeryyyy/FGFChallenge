package com.example.fgfchallenge.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone

private val DebugContainer = Color(0xFF57636C)
private val DebugContent = Color(0xFFFFFFFF)

private val InfoContainer = Color(0xFF1F6FEB)
private val InfoContent = Color(0xFFFFFFFF)

private val WarnContainer = Color(0xFFB88600)
private val WarnContent = Color(0xFF1F1400)

private val ErrorContainer = Color(0xFFD32F2F)
private val ErrorContent = Color(0xFFFFFFFF)

private val FatalContainer = Color(0xFF7B1E3A)
private val FatalContent = Color(0xFFFFFFFF)

private val UnknownContainer = Color(0xFF6B7280)
private val UnknownContent = Color(0xFFFFFFFF)

// Severity tones use fixed, deterministic colors in both light and dark themes so density
// readings and badge meaning never shift with the surrounding theme.
internal val SeverityBadgeTone.containerColor: Color
    get() =
        when (this) {
            SeverityBadgeTone.Debug -> DebugContainer
            SeverityBadgeTone.Info -> InfoContainer
            SeverityBadgeTone.Warn -> WarnContainer
            SeverityBadgeTone.Error -> ErrorContainer
            SeverityBadgeTone.Fatal -> FatalContainer
            SeverityBadgeTone.Unknown -> UnknownContainer
        }

internal val SeverityBadgeTone.contentColor: Color
    get() =
        when (this) {
            SeverityBadgeTone.Debug -> DebugContent
            SeverityBadgeTone.Info -> InfoContent
            SeverityBadgeTone.Warn -> WarnContent
            SeverityBadgeTone.Error -> ErrorContent
            SeverityBadgeTone.Fatal -> FatalContent
            SeverityBadgeTone.Unknown -> UnknownContent
        }
