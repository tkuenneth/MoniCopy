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
package com.thomaskuenneth.monicopy.copy

import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.clear_destination_folder
import com.thomaskuenneth.monicopy.generated.resources.clear_source_folder
import com.thomaskuenneth.monicopy.generated.resources.destination_folder
import com.thomaskuenneth.monicopy.generated.resources.recent_destination_folders
import com.thomaskuenneth.monicopy.generated.resources.recent_source_folders
import com.thomaskuenneth.monicopy.generated.resources.source_folder
import org.jetbrains.compose.resources.StringResource

enum class DirectoryRole(
    val title: StringResource,
    val clearLabel: StringResource,
    val recentLabel: StringResource,
) {
    Source(Res.string.source_folder, Res.string.clear_source_folder, Res.string.recent_source_folders),
    Destination(Res.string.destination_folder, Res.string.clear_destination_folder, Res.string.recent_destination_folders),
}
