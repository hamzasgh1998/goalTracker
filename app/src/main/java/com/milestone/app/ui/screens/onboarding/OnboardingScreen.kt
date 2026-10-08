package com.milestone.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.MilestoneTextField
import com.milestone.app.ui.components.PrimaryButton
import com.milestone.app.ui.components.SelectableChip
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.durationScale
import kotlinx.coroutines.launch
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** One welcome page. */
private data class IntroPage(
    val glyph: String,
    val headline: String,
    val body: String,
)

/**
 * The four opening pages.
 *
 * They state what the app is *for* rather than listing features, and the last one
 * sets up the idea the whole product rests on: a season at a time.
 */
private val introPages = listOf(
    IntroPage(
        glyph = "🧭",
        headline = "Your life is a journey.",
        body = "Not a to-do list. This app is for the handful of things you actually " +
            "want to look back on.",
    ),
    IntroPage(
        glyph = "◆",
        headline = "Turn ambitions into milestones.",
        body = "Big things become a sequence of small, finishable steps — and you " +
            "always know which one is next.",
    ),
    IntroPage(
        glyph = "📈",
        headline = "See how far you've come.",
        body = "Your progress is recorded as you go, because it is easy to forget " +
            "how much ground you covered.",
    ),
    IntroPage(
        glyph = "🌱",
        headline = "Choose what matters now.",
        body = "Work in seasons. Three goals you mean, instead of thirty you don't.",
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onFinished()
    }

    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope = rememberCoroutineScope()

    // Hoisted out of the transitionSpec below: that lambda is not composable, so
    // it cannot read the reduce-motion preference itself.
    val enterMillis = durationScale(MilestoneMotion.DurationStandard)
    val exitMillis = durationScale(MilestoneMotion.DurationQuick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MilestoneTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Spacing.gutter),
        ) {
            StepIndicator(
                step = state.step.ordinal,
                total = OnboardingStep.entries.size,
                modifier = Modifier.padding(top = Spacing.lg),
            )

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    fadeIn(tween(enterMillis)) togetherWith fadeOut(tween(exitMillis))
                },
                label = "onboardingStep",
                modifier = Modifier.weight(1f),
            ) { step ->
                when (step) {
                    OnboardingStep.Intro -> IntroPager(pagerState = pagerState)

                    OnboardingStep.Name -> NameStep(
                        name = state.name,
                        onNameChange = viewModel::onNameChange,
                    )

                    OnboardingStep.Areas -> AreasStep(
                        state = state,
                        onToggle = viewModel::onToggleArea,
                    )

                    OnboardingStep.Season -> SeasonStep(
                        state = state,
                        onTitleChange = viewModel::onSeasonTitleChange,
                        onThemeChange = viewModel::onSeasonThemeChange,
                        onIntentionChange = viewModel::onSeasonIntentionChange,
                    )
                }
            }

            // On the intro step the button walks the pager forward, so the user
            // never has to discover that the pages swipe.
            val isLastIntroPage = pagerState.currentPage == introPages.lastIndex
            PrimaryButton(
                text = when {
                    state.step == OnboardingStep.Intro && !isLastIntroPage -> "Continue"
                    state.step == OnboardingStep.Intro -> "Get started"
                    state.step == OnboardingStep.Season -> "Begin my first season"
                    else -> "Continue"
                },
                enabled = state.canAdvance && !state.isSaving,
                trailingIcon = MilestoneIcons.ArrowRight,
                onClick = {
                    if (state.step == OnboardingStep.Intro && !isLastIntroPage) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        viewModel.onNext()
                    }
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.minTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
                if (!state.step.isFirst) {
                    TextActionButton(
                        text = "Back",
                        color = MilestoneTheme.colors.inkFaint,
                        onClick = viewModel::onBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroPager(pagerState: PagerState) {
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val intro = introPages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.sm),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = intro.glyph, style = MilestoneTheme.type.displayLarge)
                Spacer(modifier = Modifier.height(Spacing.heading))
                Text(
                    text = intro.headline,
                    style = MilestoneTheme.type.displayMedium,
                    color = MilestoneTheme.colors.ink,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text = intro.body,
                    style = MilestoneTheme.type.bodyLarge,
                    color = MilestoneTheme.colors.inkMuted,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.xxl),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(introPages.size) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .background(
                            color = if (selected) {
                                MilestoneTheme.colors.accent
                            } else {
                                MilestoneTheme.colors.hairline
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Eyebrow(text = "First, the basics")
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "What should we call you?",
            style = MilestoneTheme.type.displaySmall,
            color = MilestoneTheme.colors.ink,
        )
        Spacer(modifier = Modifier.height(Spacing.heading))
        MilestoneTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "Your name",
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
            textStyle = MilestoneTheme.type.headlineMedium,
            minHeight = 64.dp,
            supportingText = "Only ever stored on this device.",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreasStep(state: OnboardingUiState, onToggle: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Eyebrow(text = "Step 2 of 3")
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Which parts of your life matter most right now?",
            style = MilestoneTheme.type.displaySmall,
            color = MilestoneTheme.colors.ink,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Pick a few. You can change this whenever you like.",
            style = MilestoneTheme.type.bodyMedium,
            color = MilestoneTheme.colors.inkMuted,
        )
        Spacer(modifier = Modifier.height(Spacing.heading))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            state.areas.forEach { area ->
                SelectableChip(
                    text = area.name,
                    leading = area.emoji,
                    selected = area.id in state.selectedAreaIds,
                    onClick = { onToggle(area.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonStep(
    state: OnboardingUiState,
    onTitleChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onIntentionChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Eyebrow(text = "Step 3 of 3")
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = "Name this season.",
            style = MilestoneTheme.type.displaySmall,
            color = MilestoneTheme.colors.ink,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "A season is a stretch of time with one idea behind it. " +
                "Yours ends around ${state.seasonEnd?.monthValue?.let { monthName(it) } ?: "later"}.",
            style = MilestoneTheme.type.bodyMedium,
            color = MilestoneTheme.colors.inkMuted,
        )

        Spacer(modifier = Modifier.height(Spacing.heading))

        MilestoneTextField(
            value = state.seasonTitle,
            onValueChange = onTitleChange,
            label = "Season",
            placeholder = "Autumn 2026",
            imeAction = ImeAction.Next,
        )

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Eyebrow(text = "Theme")
        Spacer(modifier = Modifier.height(Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            state.themeOptions.forEach { option ->
                SelectableChip(
                    text = option,
                    selected = state.seasonTheme == option,
                    onClick = { onThemeChange(option) },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        MilestoneTextField(
            value = state.seasonIntention,
            onValueChange = onIntentionChange,
            label = "What is this season for?",
            placeholder = "Optional — one sentence to come back to.",
            maxLines = 3,
            minHeight = 88.dp,
        )

        Spacer(modifier = Modifier.height(Spacing.xxl))
    }
}

/** Thin progress rail across the top, one segment per step. */
@Composable
private fun StepIndicator(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        color = if (index <= step) {
                            MilestoneTheme.colors.accent
                        } else {
                            MilestoneTheme.colors.hairline
                        },
                        shape = MilestoneTheme.shapes.pill,
                    ),
            )
        }
    }
}

private fun monthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
