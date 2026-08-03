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

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption

class SymbolicLinkPreserver {

    fun preserve(
        links: List<File>,
        fromRoot: File,
        toRoot: File,
        onError: (source: File, message: String) -> Unit,
    ) {
        for (link in links) {
            val destination = File(toRoot, relativePathUnder(fromRoot, link))
            try {
                if (!Files.isSymbolicLink(link.toPath())) {
                    onError(link, "not a symbolic link")
                    continue
                }
                destination.parentFile?.mkdirs()
                Files.copy(
                    link.toPath(),
                    destination.toPath(),
                    LinkOption.NOFOLLOW_LINKS,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (e: IOException) {
                onError(link, e.localizedMessage)
            }
        }
    }
}
