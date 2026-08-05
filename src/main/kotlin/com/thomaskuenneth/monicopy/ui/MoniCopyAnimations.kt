/*
 * Copyright 2017 - 2026 Thomas Kuenneth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thomaskuenneth.monicopy.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object MoniCopyAnimations {
    private val fallbackEffectsSpec: FiniteAnimationSpec<Float> =
        MotionScheme.expressive().defaultEffectsSpec()

    private val fallbackSpatialSpec: FiniteAnimationSpec<IntSize> =
        MotionScheme.expressive().defaultSpatialSpec()

    val crossfadeSpec: FiniteAnimationSpec<Float> = fallbackEffectsSpec

    val spatialSpec: FiniteAnimationSpec<IntSize> = fallbackSpatialSpec

    fun fadeTransition(): ContentTransform =
        fadeIn(animationSpec = fallbackEffectsSpec) togetherWith
            fadeOut(animationSpec = fallbackEffectsSpec)

    @Composable
    fun rememberCrossfadeSpec(): FiniteAnimationSpec<Float> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultEffectsSpec() }
    }

    @Composable
    fun rememberSpatialSpec(): FiniteAnimationSpec<IntSize> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultSpatialSpec() }
    }

    @Composable
    fun rememberPlacementSpec(): FiniteAnimationSpec<IntOffset> {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.defaultSpatialSpec() }
    }

    @Composable
    fun rememberFadeTransition(): ContentTransform {
        val effectsSpec = rememberCrossfadeSpec()
        return remember(effectsSpec) {
            fadeIn(animationSpec = effectsSpec) togetherWith
                fadeOut(animationSpec = effectsSpec)
        }
    }
}
