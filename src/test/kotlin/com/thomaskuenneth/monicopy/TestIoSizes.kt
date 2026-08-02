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
package com.thomaskuenneth.monicopy

/**
 * Named fixture sizes for I/O tests.
 *
 * Boundary sizes track [IoBuffers.DEFAULT_LENGTH]. Smoke sizes stay fixed so they
 * remain meaningful regardless of how large the production buffer is.
 */
internal object TestIoSizes {
    val buffer: Int = IoBuffers.DEFAULT_LENGTH

    /** Tiny smoke payload; independent of buffer size. */
    const val tiny = 1

    /** Small smoke payload; independent of buffer size. */
    const val small = 1024

    /** Under-buffer payload for MD5-differ / light hashing cases. */
    val fitsModerately: Int = minOf(4 * 1024, buffer / 64).coerceAtLeast(1)

    /** Under-buffer payload for same-MD5 / skip-rewrite cases. */
    val fitsComfortably: Int = minOf(8 * 1024, buffer / 32).coerceAtLeast(1)

    /** Under-buffer payload that still exercises the in-memory MD5 reuse path. */
    val fitsEasily: Int = minOf(64 * 1024, buffer / 16).coerceAtLeast(1)

    val half: Int = buffer / 2
    val justUnder: Int = buffer - 1
    val exact: Int = buffer
    val justOver: Int = buffer + 1
    val moderatelyOver: Int = buffer + 128 * 1024
    val wellOver: Int = buffer + 256 * 1024
}
