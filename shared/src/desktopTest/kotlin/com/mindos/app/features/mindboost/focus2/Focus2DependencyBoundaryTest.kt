package com.mindos.app.features.mindboost.focus2

import java.io.DataInputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Focus V5 · S3 — the MECHANICAL legacy boundary (plan §4.2, finding P2-18).
 *
 * The plan is explicit that this must be "una frontera legacy MECANICA por analisis de dependencias, **no por
 * scanner de texto**" and that "el grep CI queda como defensa secundaria, nunca la garantia". So this test does
 * not read Kotlin source. It reads the COMPILED class files, parses each constant pool, collects every
 * `CONSTANT_Class` entry, and walks the resulting graph TRANSITIVELY.
 *
 * Why that distinction earns its complexity: a source grep sees `import`s. It does not see a type reached
 * through a typealias, through an inline function's body, through a default argument that constructs a legacy
 * object, or through a second-hop helper in an allowed package that itself drags v1 in. The constant pool sees
 * all of those, because they are all resolved by the time bytecode exists.
 *
 * What is forbidden is the v1 `focus` package as a whole — `features/mindboost/focus/`, which is a different
 * package from `focus2` despite the shared prefix. Plan §4.1 permits reuse of transversal authorities only
 * (`core/scoring`, `core/session`, `core/timing`, secure storage, `DeterministicRng`/`SessionSeed`) and says of
 * v1: "jamas los tipos v1 de focus contaminados".
 */
class Focus2DependencyBoundaryTest {

    @Test
    fun no_focus2_class_reaches_the_v1_focus_package_directly_or_transitively() {
        val classes = loadOwnClasses()
        val vacuityMessage = "Only ${classes.size} compiled classes found. This test is vacuous if the tree " +
            "is not built; it must not pass by finding nothing."
        assertTrue(classes.size >= MIN_EXPECTED_CLASSES, vacuityMessage)

        val seeds = classes.keys.filter { it.startsWith(FOCUS2_PREFIX) }
        assertTrue(seeds.isNotEmpty(), "no focus2 classes on the classpath — the boundary test would be vacuous")

        val violations = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, List<String>>>()
        seeds.forEach { queue.add(it to listOf(it)) }

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            if (!seen.add(current)) continue
            val refs = classes[current] ?: continue
            for (ref in refs) {
                if (ref.startsWith(FORBIDDEN_V1_PREFIX) && !ref.startsWith(MISLOCATED_STORAGE_PREFIX)) {
                    violations += (path + ref).joinToString(" -> ")
                    continue
                }
                // Follow own code only. Stopping at the repo boundary keeps the walk finite and is sound:
                // a stdlib or Compose class cannot itself reach back into this repo's v1 package.
                if (ref.startsWith(OWN_PREFIX) && ref !in seen) queue.add(ref to path + ref)
            }
        }

        if (violations.isNotEmpty()) {
            fail(
                "focus2 reaches the v1 focus package. Each line is a real dependency path, not a text match:\n" +
                    violations.distinct().sorted().joinToString("\n").prependIndent("  "),
            )
        }
    }

    /**
     * The carve-out is an exception with a fixed size, not a door.
     *
     * It pins the EXACT set of v1 symbols focus2 reaches. A new one — even inside the permitted storage
     * package — turns this red and forces a deliberate decision, which is the difference between a documented
     * exception and a boundary that quietly stopped meaning anything. If `MB-FOCUS-0003` is ever resolved by
     * relocating the primitive, this test goes red too, and the correct fix is to delete the carve-out.
     */
    @Test
    fun the_storage_carve_out_has_not_grown() {
        val classes = loadOwnClasses()
        val reached = classes
            .filterKeys { it.startsWith(FOCUS2_PREFIX) }
            .values
            .flatten()
            .filter { it.startsWith(FORBIDDEN_V1_PREFIX) }
            .map { it.removePrefix(FORBIDDEN_V1_PREFIX) }
            .toSortedSet()

        val expected = sortedSetOf(
            "data/local/SecureKeyValueStore",
            "data/local/SecureReadResult",
            "data/local/SecureReadResult\$Failure",
            "data/local/SecureReadResult\$Missing",
            "data/local/SecureReadResult\$Value",
        )
        assertEquals(
            expected,
            reached,
            "The v1 surface focus2 depends on changed. Every entry must be the mislocated secure-storage " +
                "primitive (MB-FOCUS-0003) and nothing else. Adding a symbol here is an architectural " +
                "decision, not a test fix.",
        )
    }

    /**
     * The signature half (plan §4.2): the PUBLIC surface of focus2 must not carry the aggregator's currency.
     *
     * This is the lesson the `evidenceBoundaryScanner` was built from — a type whose SHAPE matches
     * `ModuleContribution(normalizedScore, confidence, dataSufficiency)` is wireable into the profile by
     * accident, without violating a single type. "Not wired" is a state of the tree, not a barrier.
     */
    @Test
    fun no_focus2_class_reaches_the_profile_contribution_types() {
        val classes = loadOwnClasses()
        val offenders = classes
            .filterKeys { it.startsWith(FOCUS2_PREFIX) }
            .mapValues { (_, refs) -> refs.filter { ref -> FORBIDDEN_PROFILE_MARKERS.any { ref.contains(it) } } }
            .filterValues { it.isNotEmpty() }

        if (offenders.isNotEmpty()) {
            fail(
                "focus2 references profile/aggregator types:\n" +
                    offenders.entries.joinToString("\n") { (k, v) -> "  $k -> ${v.joinToString()}" },
            )
        }
    }

    // ---------------------------------------------------------------------------------------------------
    // Class file reading. Only the constant pool is parsed; nothing else is needed and nothing else is read.
    // ---------------------------------------------------------------------------------------------------

    private fun loadOwnClasses(): Map<String, Set<String>> {
        val root = classesRoot()
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .mapNotNull { file ->
                val internalName = file.relativeTo(root).invariantSeparatorsPath.removeSuffix(".class")
                runCatching { internalName to referencedClasses(file) }.getOrNull()
            }
            .toMap()
    }

    private fun classesRoot(): File {
        val candidates = listOf(
            File(CLASSES_RELATIVE),
            File("shared/$CLASSES_RELATIVE"),
            File("../shared/$CLASSES_RELATIVE"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: fail("compiled desktop classes not found; looked in ${candidates.map { it.absolutePath }}")
    }

    /** Every `CONSTANT_Class` name in [file]'s constant pool, normalised to an internal name. */
    @Suppress("CyclomaticComplexMethod") // The JVM constant-pool tag table IS a flat dispatch; splitting hides it.
    private fun referencedClasses(file: File): Set<String> {
        DataInputStream(file.inputStream().buffered()).use { input ->
            require(input.readInt() == CLASS_MAGIC) { "not a class file: $file" }
            input.readUnsignedShort() // minor
            input.readUnsignedShort() // major
            val count = input.readUnsignedShort()
            val utf8 = HashMap<Int, String>()
            val classNameIndex = HashMap<Int, Int>()
            var i = 1
            while (i < count) {
                when (val tag = input.readUnsignedByte()) {
                    TAG_UTF8 -> utf8[i] = input.readUTF()
                    TAG_CLASS -> classNameIndex[i] = input.readUnsignedShort()
                    TAG_STRING, TAG_METHOD_TYPE, TAG_MODULE, TAG_PACKAGE -> input.skipNBytes(2)
                    TAG_INTEGER, TAG_FLOAT -> input.skipNBytes(4)
                    TAG_LONG, TAG_DOUBLE -> {
                        input.skipNBytes(8)
                        i++ // long and double occupy two constant-pool slots
                    }
                    TAG_FIELDREF, TAG_METHODREF, TAG_INTERFACE_METHODREF, TAG_NAME_AND_TYPE,
                    TAG_DYNAMIC, TAG_INVOKE_DYNAMIC,
                    -> input.skipNBytes(4)
                    TAG_METHOD_HANDLE -> input.skipNBytes(3)
                    else -> fail("unknown constant pool tag $tag in $file")
                }
                i++
            }
            return classNameIndex.values.mapNotNull { utf8[it] }.map { it.trimStart('[').removeSuffix(";") }
                .map { if (it.startsWith("L")) it.drop(1) else it }
                .toSet()
        }
    }

    private companion object {
        const val CLASSES_RELATIVE = "build/classes/kotlin/desktop/main"
        const val OWN_PREFIX = "com/mindos/app/"
        const val FOCUS2_PREFIX = "com/mindos/app/features/mindboost/focus2/"

        /** Note the trailing slash: `focus2` shares a prefix with `focus` and must not be caught by it. */
        const val FORBIDDEN_V1_PREFIX = "com/mindos/app/features/mindboost/focus/"

        /**
         * The ONE carve-out, and the reason it is a carve-out rather than a pass.
         *
         * `SecureKeyValueStore` / `SecureReadResult` are the encrypted-storage primitive for ALL of MindBoost —
         * memory, logic, decisiones, emotions and speed all persist through them — and plan §4.1 explicitly
         * lists "storage seguro" among the transversal authorities focus2 MAY reuse. What is wrong is not the
         * dependency, it is the ADDRESS: the type physically lives inside the v1 `focus` feature package, so a
         * package-level boundary cannot tell a transversal authority apart from v1 contamination.
         *
         * That mislocation is `MB-FOCUS-0003`, already OPEN and bounded by ADR since S1. Relocating it to
         * `core/storage` touches every MindBoost feature and belongs to its own slice with its own gate, so S3
         * records the exception instead of performing a refactor it was not authorised to perform.
         *
         * [the_storage_carve_out_has_not_grown] pins the exception's exact extent, so it cannot quietly widen
         * into "focus2 may reach v1 after all".
         */
        const val MISLOCATED_STORAGE_PREFIX = "com/mindos/app/features/mindboost/focus/data/local/"

        val FORBIDDEN_PROFILE_MARKERS = listOf(
            "features/mindboost/profile/",
            "ModuleContribution",
            "MindBoostProfileAggregator",
        )

        const val MIN_EXPECTED_CLASSES = 500
        const val CLASS_MAGIC = -0x35014542 // 0xCAFEBABE

        const val TAG_UTF8 = 1
        const val TAG_INTEGER = 3
        const val TAG_FLOAT = 4
        const val TAG_LONG = 5
        const val TAG_DOUBLE = 6
        const val TAG_CLASS = 7
        const val TAG_STRING = 8
        const val TAG_FIELDREF = 9
        const val TAG_METHODREF = 10
        const val TAG_INTERFACE_METHODREF = 11
        const val TAG_NAME_AND_TYPE = 12
        const val TAG_METHOD_HANDLE = 15
        const val TAG_METHOD_TYPE = 16
        const val TAG_DYNAMIC = 17
        const val TAG_INVOKE_DYNAMIC = 18
        const val TAG_MODULE = 19
        const val TAG_PACKAGE = 20
    }
}
