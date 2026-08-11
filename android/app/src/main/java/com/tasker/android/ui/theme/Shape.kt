package com.tasker.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────
//  Shape scale — design.md corner radii
//  sm=4dp, md=8dp, lg=12dp, xl=16dp, 2xl=24dp
// ─────────────────────────────────────────────────────────────────
val TaskerShapes = Shapes(
    // extraSmall → 4dp (sm)
    extraSmall = RoundedCornerShape(4.dp),
    // small → 8dp (md)
    small = RoundedCornerShape(8.dp),
    // medium → 12dp (lg)
    medium = RoundedCornerShape(12.dp),
    // large → 16dp (xl)
    large = RoundedCornerShape(16.dp),
    // extraLarge → 24dp (2xl)
    extraLarge = RoundedCornerShape(24.dp),
)
