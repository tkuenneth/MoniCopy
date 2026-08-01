# Backport of Compose Multiplatform default rule (post-1.11.1):
# Skiko keeps JbrSharedTexturesAdapter, whose constructor references an optional
# JBR-only type that is not on the ProGuard program/library classpath.
# -dontnote only silences the note; it does not shrink or remove any classes.
-dontnote org.jetbrains.skiko.swing.JbrSharedTexturesAdapter
-dontnote com.jetbrains.SharedTextures
