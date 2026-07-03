package com.mindos.app.features.arena

import java.io.File

/**
 * Locates arena source directories from the JVM test working directory. Works whether tests run with
 * the working dir at the repo root or at the `shared` module dir. Used by source-scanning boundary
 * tests (Detekt does not scan desktopTest, so file IO here is fine).
 */
object ArenaSourceLocator {

    private const val MAIN = "src/commonMain/kotlin/com/mindos/app/features/arena"
    private const val COMMON_TEST = "src/commonTest/kotlin/com/mindos/app/features/arena"
    private const val DESKTOP_TEST = "src/desktopTest/kotlin/com/mindos/app/features/arena"

    fun mainDir(): File = requireNotNull(find(MAIN)) {
        "Arena main source dir not found from ${File(".").absolutePath}"
    }

    fun engineDir(): File = File(mainDir(), "rating/engine")

    fun testDirs(): List<File> = listOfNotNull(find(COMMON_TEST), find(DESKTOP_TEST))

    fun kotlinFiles(dir: File): List<File> =
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    /** Source with block and line comments removed, so doc-comment mentions don't fool token scans. */
    fun codeWithoutComments(file: File): String {
        val noBlock = file.readText().replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
        return noBlock.lineSequence().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.take(idx) else line
        }
    }

    /** Trimmed `import ...` lines from a file (used for dependency-boundary scanning). */
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

    private const val MAX_DEPTH = 8
}
