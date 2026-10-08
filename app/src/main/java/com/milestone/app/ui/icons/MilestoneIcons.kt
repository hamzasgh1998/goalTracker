package com.milestone.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The app's icon set, drawn in code.
 *
 * Hand-rolled rather than taken from `material-icons-extended`, for two reasons:
 * that artifact is frozen upstream and no longer gaining icons, and a single
 * consistent stroke weight across every glyph is a large part of what makes an
 * interface look designed rather than assembled. These are all 1.8px strokes on a
 * 24px grid with round caps, so they sit together correctly at any size.
 *
 * Each vector is drawn in black and tinted at the call site, the same convention
 * Material's own icons use.
 */
object MilestoneIcons {

    // ------------------------------------------------------------ navigation

    /** Home: a roof over an open body, so it stays legible at 22dp. */
    val Home: ImageVector by lazy {
        strokeIcon("Home") {
            moveTo(3f, 11.5f)
            lineTo(12f, 4f)
            lineTo(21f, 11.5f)
            moveTo(5.5f, 10f)
            lineTo(5.5f, 20f)
            lineTo(18.5f, 20f)
            lineTo(18.5f, 10f)
        }
    }

    /** Life Map: a winding route with stops along it. */
    val Map: ImageVector by lazy {
        strokeIcon("Map") {
            moveTo(4f, 20f)
            curveTo(9f, 20f, 8f, 13.5f, 12f, 13.5f)
            curveTo(16f, 13.5f, 15f, 5f, 20f, 5f)
            circle(4f, 20f, 1.6f)
            circle(12f, 13.5f, 1.6f)
            circle(20f, 5f, 1.6f)
        }
    }

    /** Goals: concentric rings, read as a target without the arrow cliché. */
    val Goals: ImageVector by lazy {
        strokeIcon("Goals") {
            circle(12f, 12f, 8.5f)
            circle(12f, 12f, 4.6f)
            circle(12f, 12f, 1.2f)
        }
    }

    /** Journey: ascending bars — ground covered, not a chart. */
    val Journey: ImageVector by lazy {
        strokeIcon("Journey") {
            moveTo(4.5f, 18.5f)
            lineTo(4.5f, 14f)
            moveTo(10.5f, 18.5f)
            lineTo(10.5f, 9.5f)
            moveTo(16.5f, 18.5f)
            lineTo(16.5f, 5.5f)
            moveTo(3f, 21f)
            lineTo(21f, 21f)
        }
    }

    /** Profile. */
    val Profile: ImageVector by lazy {
        strokeIcon("Profile") {
            circle(12f, 8f, 3.9f)
            moveTo(5f, 20.5f)
            arcTo(7.2f, 7.2f, 0f, false, true, 19f, 20.5f)
        }
    }

    // --------------------------------------------------------------- actions

    val Plus: ImageVector by lazy {
        strokeIcon("Plus") {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("Check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 6.5f)
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("Close") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    val ArrowLeft: ImageVector by lazy {
        strokeIcon("ArrowLeft") {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val ArrowRight: ImageVector by lazy {
        strokeIcon("ArrowRight") {
            moveTo(5f, 12f)
            lineTo(19f, 12f)
            moveTo(13f, 6f)
            lineTo(19f, 12f)
            lineTo(13f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon("ChevronRight") {
            moveTo(9f, 5f)
            lineTo(16f, 12f)
            lineTo(9f, 19f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        strokeIcon("ChevronDown") {
            moveTo(5f, 9f)
            lineTo(12f, 16f)
            lineTo(19f, 9f)
        }
    }

    val Edit: ImageVector by lazy {
        strokeIcon("Edit") {
            moveTo(4f, 20f)
            lineTo(4f, 16f)
            lineTo(16f, 4f)
            lineTo(20f, 8f)
            lineTo(8f, 20f)
            close()
            moveTo(13f, 7f)
            lineTo(17f, 11f)
        }
    }

    val Trash: ImageVector by lazy {
        strokeIcon("Trash") {
            moveTo(4f, 7f)
            lineTo(20f, 7f)
            moveTo(9f, 7f)
            lineTo(9f, 4.5f)
            lineTo(15f, 4.5f)
            lineTo(15f, 7f)
            moveTo(6f, 7f)
            lineTo(7f, 20f)
            lineTo(17f, 20f)
            lineTo(18f, 7f)
        }
    }

    val Pause: ImageVector by lazy {
        strokeIcon("Pause") {
            moveTo(9.5f, 5f)
            lineTo(9.5f, 19f)
            moveTo(14.5f, 5f)
            lineTo(14.5f, 19f)
        }
    }

    val Play: ImageVector by lazy {
        strokeIcon("Play") {
            moveTo(7f, 4.5f)
            lineTo(19f, 12f)
            lineTo(7f, 19.5f)
            close()
        }
    }

    /** Past Chapters: a closed book. */
    val Book: ImageVector by lazy {
        strokeIcon("Book") {
            moveTo(5f, 4.5f)
            lineTo(5f, 19.5f)
            arcTo(1.5f, 1.5f, 0f, false, false, 6.5f, 21f)
            lineTo(19f, 21f)
            moveTo(5f, 4.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, 6.5f, 3f)
            lineTo(19f, 3f)
            lineTo(19f, 21f)
            moveTo(5.6f, 17.5f)
            lineTo(19f, 17.5f)
        }
    }

    val Calendar: ImageVector by lazy {
        strokeIcon("Calendar") {
            moveTo(4.5f, 6.5f)
            lineTo(19.5f, 6.5f)
            lineTo(19.5f, 20f)
            lineTo(4.5f, 20f)
            close()
            moveTo(4.5f, 10.5f)
            lineTo(19.5f, 10.5f)
            moveTo(8.5f, 4f)
            lineTo(8.5f, 8f)
            moveTo(15.5f, 4f)
            lineTo(15.5f, 8f)
        }
    }

    val Settings: ImageVector by lazy {
        strokeIcon("Settings") {
            circle(12f, 12f, 3.2f)
            moveTo(12f, 3f)
            lineTo(12f, 6f)
            moveTo(12f, 18f)
            lineTo(12f, 21f)
            moveTo(3f, 12f)
            lineTo(6f, 12f)
            moveTo(18f, 12f)
            lineTo(21f, 12f)
            moveTo(5.6f, 5.6f)
            lineTo(7.8f, 7.8f)
            moveTo(16.2f, 16.2f)
            lineTo(18.4f, 18.4f)
            moveTo(18.4f, 5.6f)
            lineTo(16.2f, 7.8f)
            moveTo(7.8f, 16.2f)
            lineTo(5.6f, 18.4f)
        }
    }

    /** Achievements: a four-point star, calmer than a trophy. */
    val Sparkle: ImageVector by lazy {
        strokeIcon("Sparkle") {
            moveTo(12f, 3f)
            curveTo(12f, 8f, 13f, 11f, 20f, 12f)
            curveTo(13f, 13f, 12f, 16f, 12f, 21f)
            curveTo(12f, 16f, 11f, 13f, 4f, 12f)
            curveTo(11f, 11f, 12f, 8f, 12f, 3f)
            close()
        }
    }

    /**
     * Reflection: a pen held at a writing angle.
     *
     * Drawn as a closed body tapering to a nib rather than as a line with a
     * chevron on the end, which is what makes it read as a pen and not an arrow.
     */
    val Pen: ImageVector by lazy {
        strokeIcon("Pen") {
            // Body, from the nib point up to the squared-off top.
            moveTo(4f, 20f)
            lineTo(5.2f, 15.8f)
            lineTo(16.5f, 4.5f)
            lineTo(19.5f, 7.5f)
            lineTo(8.2f, 18.8f)
            close()
            // The shoulder that separates the nib from the barrel.
            moveTo(5.2f, 15.8f)
            lineTo(8.2f, 18.8f)
            // Band near the top, so the barrel doesn't read as a plain shape.
            moveTo(14.5f, 6.5f)
            lineTo(17.5f, 9.5f)
        }
    }

    /** Reorder affordance. */
    val DragHandle: ImageVector by lazy {
        strokeIcon("DragHandle") {
            moveTo(5f, 9.5f)
            lineTo(19f, 9.5f)
            moveTo(5f, 14.5f)
            lineTo(19f, 14.5f)
        }
    }

    val More: ImageVector by lazy {
        strokeIcon("More") {
            circle(12f, 5.5f, 1.3f)
            circle(12f, 12f, 1.3f)
            circle(12f, 18.5f, 1.3f)
        }
    }

    val ChevronUp: ImageVector by lazy {
        strokeIcon("ChevronUp") {
            moveTo(5f, 15f)
            lineTo(12f, 8f)
            lineTo(19f, 15f)
        }
    }

    /** Reminders. An open bell, so the stroke weight matches the rest. */
    val Bell: ImageVector by lazy {
        strokeIcon("Bell") {
            moveTo(5f, 17.5f)
            curveTo(6.6f, 15.8f, 6.6f, 14f, 6.6f, 11f)
            curveTo(6.6f, 7.5f, 9f, 5f, 12f, 5f)
            curveTo(15f, 5f, 17.4f, 7.5f, 17.4f, 11f)
            curveTo(17.4f, 14f, 17.4f, 15.8f, 19f, 17.5f)
            lineTo(5f, 17.5f)
            moveTo(10f, 20f)
            curveTo(10.7f, 21.1f, 13.3f, 21.1f, 14f, 20f)
        }
    }

    /** Export: something leaving the device, upward and out. */
    val Share: ImageVector by lazy {
        strokeIcon("Share") {
            moveTo(12f, 15.5f)
            lineTo(12f, 4f)
            moveTo(8f, 8f)
            lineTo(12f, 4f)
            lineTo(16f, 8f)
            moveTo(5f, 13.5f)
            lineTo(5f, 19.5f)
            lineTo(19f, 19.5f)
            lineTo(19f, 13.5f)
        }
    }

    /** Import: the same shape, arrow reversed — coming back in. */
    val Restore: ImageVector by lazy {
        strokeIcon("Restore") {
            moveTo(12f, 4f)
            lineTo(12f, 15.5f)
            moveTo(8f, 11.5f)
            lineTo(12f, 15.5f)
            lineTo(16f, 11.5f)
            moveTo(5f, 13.5f)
            lineTo(5f, 19.5f)
            lineTo(19f, 19.5f)
            lineTo(19f, 13.5f)
        }
    }
}

/**
 * Appends a full circle to the path.
 *
 * [PathBuilder] has no oval primitive, so a circle is two half-arcs. Factored out
 * because concentric rings and node dots appear in most of the icons above.
 */
private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcTo(radius, radius, 0f, true, true, centerX + radius, centerY)
    arcTo(radius, radius, 0f, true, true, centerX - radius, centerY)
    close()
}

/**
 * Builds a 24dp stroked icon.
 *
 * The uniform [STROKE_WIDTH] is the point of this helper: it is what lets a
 * hand-drawn set look like one family.
 */
private fun strokeIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        )
    }.build()

private const val VIEWPORT = 24f
private const val STROKE_WIDTH = 1.8f
