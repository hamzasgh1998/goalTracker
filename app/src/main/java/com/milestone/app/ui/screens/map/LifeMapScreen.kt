package com.milestone.app.ui.screens.map

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.ui.components.EmptyState
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.SelectableChip
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.resolve
import kotlin.math.sin

/**
 * The Life Map: every goal drawn as a route you can travel along.
 *
 * Each goal gets a meandering horizontal path with a node per milestone. Ground
 * already covered is drawn in the life area's colour; what is ahead is a faint
 * hairline. The result is that a glance down the screen shows which parts of a
 * life are moving and which have gone quiet — something a list of percentages
 * never communicates.
 *
 * Zoom is a two-state control rather than a pinch gesture. Pinching inside a
 * vertically scrolling list of horizontally scrolling rows is a gesture conflict
 * with no good resolution, and two labelled states ("Whole life" / "Up close")
 * are both predictable and reachable by a screen reader.
 */
@Composable
fun LifeMapScreen(
    onOpenGoal: (Long) -> Unit,
    contentPadding: PaddingValues,
    viewModel: LifeMapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = Spacing.gutter)
                .padding(top = Spacing.screenTop),
        ) {
            Eyebrow(text = "Where you're going")
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Life Map",
                style = MilestoneTheme.type.displayMedium,
                color = colors.ink,
            )

            if (!state.isEmpty) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = state.reachedNodes.toString() + " of " + state.totalNodes +
                        " stops reached",
                    style = MilestoneTheme.type.bodyMedium,
                    color = colors.inkMuted,
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MapZoom.entries.forEach { zoom ->
                        SelectableChip(
                            text = zoom.label,
                            selected = state.zoom == zoom,
                            onClick = { viewModel.onZoomChange(zoom) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.heading))

        if (state.isEmpty && !state.isLoading) {
            EmptyState(
                glyph = "🧭",
                headline = "No routes yet.",
                body = "Once a goal has milestones, it appears here as a path you " +
                    "can follow.",
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Spacing.section,
            ),
        ) {
            items(items = state.paths, key = { it.goalId }) { path ->
                GoalRoute(
                    path = path,
                    zoom = state.zoom,
                    onClick = { onOpenGoal(path.goalId) },
                )
            }
        }
    }
}

/**
 * One goal's route.
 *
 * The header is fixed to the gutter while the path itself scrolls horizontally,
 * so a long goal stays identifiable however far along it the user has scrolled.
 */
@Composable
private fun GoalRoute(path: MapPath, zoom: MapZoom, onClick: () -> Unit) {
    val colors = MilestoneTheme.colors
    val spacing = if (zoom == MapZoom.Overview) OVERVIEW_SPACING else DETAIL_SPACING
    val routeHeight = if (zoom == MapZoom.Overview) OVERVIEW_HEIGHT else DETAIL_HEIGHT

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.heading)
            // Paused routes are dimmed rather than hidden: a set-down goal is
            // still part of the map of a life.
            .alpha(if (path.isPaused) PAUSED_ALPHA else 1f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = path.emoji, style = MilestoneTheme.type.headlineMedium)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = path.title,
                    style = MilestoneTheme.type.titleLarge,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (path.isPaused) {
                        path.areaName + " · paused"
                    } else {
                        path.areaName
                    },
                    style = MilestoneTheme.type.labelMedium,
                    color = path.color,
                )
            }
            Text(
                text = (path.progress * 100).toInt().toString() + "%",
                style = MilestoneTheme.type.titleMedium,
                color = colors.inkMuted,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        val scrollState = rememberScrollState()
        val currentIndex = path.nodes.indexOfFirst { it.isCurrent }
        val density = LocalDensity.current

        // Open each route already centred on "you are here". A map whose most
        // important mark starts off-screen makes the user hunt for the one thing
        // they came to see. Keyed on the index, so finishing a milestone slides
        // the route along to the next stop rather than leaving it behind.
        LaunchedEffect(currentIndex, scrollState.maxValue, spacing) {
            if (currentIndex <= 0 || scrollState.maxValue == 0) return@LaunchedEffect
            val nodeX = with(density) { (ROUTE_PADDING + spacing * currentIndex).toPx() }
            val target = (nodeX - scrollState.viewportSize / 2f)
                .toInt()
                .coerceIn(0, scrollState.maxValue)
            scrollState.animateScrollTo(target)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
        ) {
            RouteCanvas(
                path = path,
                nodeSpacing = spacing,
                height = routeHeight,
                compact = zoom == MapZoom.Overview,
            )
        }
    }
}

/**
 * Draws the route and its stops.
 *
 * The curve is built segment by segment so each stretch can take its own colour —
 * covered ground in the area's colour, the road ahead in hairline. A single path
 * with one colour could not show the boundary, and the boundary is the point.
 */
@Composable
private fun RouteCanvas(
    path: MapPath,
    nodeSpacing: Dp,
    height: Dp,
    compact: Boolean,
) {
    val colors = MilestoneTheme.colors
    val density = LocalDensity.current

    val reachedFraction by animateFloatAsState(
        targetValue = path.progress,
        animationSpec = MilestoneMotion.settle<Float>().resolve(),
        label = "routeProgress",
    )

    val nodeCount = path.nodes.size
    val canvasWidth = remember(nodeCount, nodeSpacing) {
        nodeSpacing * (nodeCount - 1) + ROUTE_PADDING * 2
    }

    val labelStyle = MilestoneTheme.type.labelSmall
    val currentLabelStyle = MilestoneTheme.type.labelMedium

    Box(modifier = Modifier.width(canvasWidth).height(height)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val points = routePoints(
                count = nodeCount,
                spacingPx = with(density) { nodeSpacing.toPx() },
                startX = with(density) { ROUTE_PADDING.toPx() },
                centerY = size.height * WAVE_CENTER_FRACTION,
                amplitude = with(density) {
                    (if (compact) OVERVIEW_AMPLITUDE else DETAIL_AMPLITUDE).toPx()
                },
            )

            drawRoute(
                points = points,
                nodes = path.nodes,
                coveredColor = path.color,
                aheadColor = colors.hairline,
                strokeWidth = with(density) { ROUTE_STROKE.toPx() },
            )

            drawNodes(
                points = points,
                nodes = path.nodes,
                coveredColor = path.color,
                aheadColor = colors.hairline,
                backgroundColor = colors.background,
                compact = compact,
                density = density,
                reachedFraction = reachedFraction,
            )
        }

        // Labels sit above the canvas as real text so they scale with the user's
        // font size and are readable by a screen reader.
        if (!compact) {
            path.nodes.forEachIndexed { index, node ->
                val x = ROUTE_PADDING + nodeSpacing * index
                val isCurrent = node.isCurrent
                Text(
                    text = node.title,
                    style = if (isCurrent) currentLabelStyle else labelStyle,
                    color = when {
                        isCurrent -> colors.ink
                        node.isCompleted -> colors.inkFaint
                        else -> colors.inkMuted
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .offset(x = (x - nodeSpacing / 2).coerceAtLeast(0.dp), y = LABEL_TOP)
                        .width(nodeSpacing)
                        .padding(horizontal = Spacing.xs),
                )
            }
        }
    }
}

/** Evenly spaced points along a gentle wave. */
private fun routePoints(
    count: Int,
    spacingPx: Float,
    startX: Float,
    centerY: Float,
    amplitude: Float,
): List<Offset> = List(count) { index ->
    Offset(
        x = startX + spacingPx * index,
        // A slow sine keeps the path feeling like a journey rather than a chart
        // axis, without ever doubling back on itself.
        y = centerY + amplitude * sin(index * WAVE_FREQUENCY),
    )
}

private fun DrawScope.drawRoute(
    points: List<Offset>,
    nodes: List<MapNode>,
    coveredColor: Color,
    aheadColor: Color,
    strokeWidth: Float,
) {
    if (points.size < 2) return

    for (index in 0 until points.lastIndex) {
        val from = points[index]
        val to = points[index + 1]
        // A segment counts as covered only when the stop that starts it is done.
        val covered = nodes[index].isCompleted

        val segment = Path().apply {
            moveTo(from.x, from.y)
            val midX = (from.x + to.x) / 2f
            cubicTo(midX, from.y, midX, to.y, to.x, to.y)
        }

        drawPath(
            path = segment,
            color = if (covered) coveredColor else aheadColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawNodes(
    points: List<Offset>,
    nodes: List<MapNode>,
    coveredColor: Color,
    aheadColor: Color,
    backgroundColor: Color,
    compact: Boolean,
    density: androidx.compose.ui.unit.Density,
    reachedFraction: Float,
) {
    val doneRadius = with(density) { (if (compact) 5.dp else 7.dp).toPx() }
    val aheadRadius = with(density) { (if (compact) 3.5.dp else 5.dp).toPx() }
    val currentRadius = with(density) { (if (compact) 7.dp else 10.dp).toPx() }

    points.forEachIndexed { index, point ->
        val node = nodes[index]
        when {
            node.isCurrent -> {
                // Halo scales with how far along the goal is, so a nearly
                // finished route visibly glows brighter at its live edge.
                drawCircle(
                    color = coveredColor.copy(alpha = 0.12f + 0.18f * reachedFraction),
                    radius = currentRadius * 2.1f,
                    center = point,
                )
                drawCircle(color = backgroundColor, radius = currentRadius, center = point)
                drawCircle(
                    color = coveredColor,
                    radius = currentRadius,
                    center = point,
                    style = Stroke(width = with(density) { 2.dp.toPx() }),
                )
                drawCircle(color = coveredColor, radius = currentRadius * 0.45f, center = point)
            }

            node.isCompleted -> {
                drawCircle(color = coveredColor, radius = doneRadius, center = point)
            }

            else -> {
                drawCircle(color = backgroundColor, radius = aheadRadius, center = point)
                drawCircle(
                    color = aheadColor,
                    radius = aheadRadius,
                    center = point,
                    style = Stroke(width = with(density) { 1.5.dp.toPx() }),
                )
            }
        }
    }
}

private val DETAIL_SPACING = 108.dp
private val OVERVIEW_SPACING = 46.dp
private val DETAIL_HEIGHT = 172.dp
private val OVERVIEW_HEIGHT = 104.dp
private val DETAIL_AMPLITUDE = 26.dp
private val OVERVIEW_AMPLITUDE = 20.dp
private val ROUTE_PADDING = 28.dp
private val ROUTE_STROKE = 2.5.dp

/**
 * Where labels start, measured from the top of the route box.
 *
 * Must clear the lowest a node can sit — the wave's trough plus the current
 * node's halo. With the wave centred at [WAVE_CENTER_FRACTION] of a
 * [DETAIL_HEIGHT] box that bottom edge is about 109dp, so labels begin below it
 * and never collide with a stop.
 */
private val LABEL_TOP = 116.dp

/**
 * Vertical position of the wave's centre line, as a fraction of route height.
 *
 * Deliberately above centre: labels need a clear lane underneath, and a centred
 * wave left dead space above it instead.
 */
private const val WAVE_CENTER_FRACTION = 0.36f

private const val WAVE_FREQUENCY = 0.85f
private const val PAUSED_ALPHA = 0.45f
