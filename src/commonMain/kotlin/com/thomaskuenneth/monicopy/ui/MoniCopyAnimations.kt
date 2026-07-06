package com.thomaskuenneth.monicopy.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

object MoniCopyAnimations {
    const val FadeDurationMillis = 450

    val crossfadeSpec: FiniteAnimationSpec<Float> = tween(FadeDurationMillis)

    fun fadeTransition(): ContentTransform =
        fadeIn(animationSpec = tween(FadeDurationMillis)) togetherWith
            fadeOut(animationSpec = tween(FadeDurationMillis))
}
