package com.example.fgfchallenge.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val BadgeCornerRadius = 6.dp
internal val ContainerCornerRadius = 12.dp

internal val FGFChallengeShapes =
    Shapes(
        extraSmall = RoundedCornerShape(BadgeCornerRadius),
        medium = RoundedCornerShape(ContainerCornerRadius),
    )
