package com.milestone.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Corner radii.
 *
 * Generous but not pill-shaped: large surfaces get 24–28dp so they feel soft, while
 * small controls stay at 10–14dp so they still read as controls. Fully rounded is
 * reserved for [MilestoneShapes.pill] (chips, progress tracks) where the shape
 * itself carries meaning.
 */
@Immutable
object MilestoneShapes {
    val chip = RoundedCornerShape(10.dp)
    val control = RoundedCornerShape(14.dp)
    val card = RoundedCornerShape(20.dp)
    val panel = RoundedCornerShape(26.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = RoundedCornerShape(percent = 50)
}

val MilestoneMaterialShapes = Shapes(
    extraSmall = MilestoneShapes.chip,
    small = MilestoneShapes.control,
    medium = MilestoneShapes.card,
    large = MilestoneShapes.panel,
    extraLarge = RoundedCornerShape(28.dp),
)
