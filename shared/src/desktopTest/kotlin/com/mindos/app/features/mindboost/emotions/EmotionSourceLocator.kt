package com.mindos.app.features.mindboost.emotions

import java.io.File

/**
 * Locates emotions source directories from the JVM test working dir (repo root or `shared` module).
 * Mirrors `ArenaSourceLocator`. Used by source-scanning boundary tests; Detekt does not scan
 * desktopTest, so file IO here is fine.
 */
object EmotionSourceLocator {

    private const val MAIN = "src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions"
    private const val MAX_DEPTH = 8

    fun mainDir(): File = requireNotNull(find(MAIN)) {
        "Emotions main source dir not found from ${File(".").absolutePath}"
    }

    fun kotlinFiles(dir: File): List<File> =
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    fun importLines(file: File): List<String> =
        file.readText().lineSequence().map { it.trim() }.filter { it.startsWith("import ") }.toList()

    private fun find(relative: String): File? {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: return null
            val direct = File(current, relative)
            if (direct.exists()) return direct
            val underShared = File(current, "shared/$relative")
            if (underShared.exists()) return underShared
            base = current.parentFile
        }
        return null
    }
}
