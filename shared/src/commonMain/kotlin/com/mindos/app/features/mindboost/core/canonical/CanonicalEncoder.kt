package com.mindos.app.features.mindboost.core.canonical

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/** A value that cannot be canonicalized unambiguously. Fail-closed: never encode something we cannot pin. */
internal class CanonicalizationException(message: String) : IllegalArgumentException(message)

/**
 * Identifies the byte-level encoding a digest was computed under. Frozen once published.
 *
 * A digest is meaningless without the encoding that produced it: the same fields under a different encoder are a
 * different digest, and comparing across the two would fail rows that never drifted. Persisting the id is what
 * lets V2 exist later without reinterpreting V1 rows under V2 rules.
 */
enum class CanonicalizerVersion(val id: String) {
    /** Tagged, length-prefixed, field-named, fixed-width. Do not modify — publish V2 instead. */
    V1("mindboost-canonical-v1"),
    ;

    companion object {
        fun fromId(id: String): CanonicalizerVersion? = entries.firstOrNull { it.id == id }
    }
}

/** Identifies the digest function. Frozen alongside [CanonicalizerVersion]. */
enum class DigestAlgorithmId(val id: String) {
    SHA_256("sha-256"),
    ;

    companion object {
        fun fromId(id: String): DigestAlgorithmId? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Canonicalizes any `@Serializable` value into an unambiguous byte string, driven by its [SerialDescriptor].
 *
 * ## Why this is descriptor-driven rather than hand-written
 *
 * The previous design built its canonical string by appending hand-chosen fields (`"seed=$x;rng=$y;..."`) and
 * kept a second hand-written list of the same fields in `FormInputs`, a third in the identity's constructor and
 * a fourth in the mapper. Four lists, one meaning. They drifted — and not hypothetically: the sibling
 * `FocusFormFingerprint.payloadOf` used the same hand-copy approach and was found omitting
 * `Vigilance.lure`, `Vigilance.isSignal`, `MultiObjectTracking.targetCount`, `TaskSwitch.correctResponse` and
 * `DualTask.go`, in a function whose own KDoc asserted it named every field that makes the form what it is.
 * A previous fix had made that `when` total over the 13 payload TYPES — which the compiler enforces — while the
 * FIELDS inside each branch stayed hand-copied, which nothing enforces. The defect survived the fix because the
 * fix addressed the level the compiler could see.
 *
 * The lesson is structural, not local: any canonicalization that a human maintains as a list of fields will
 * eventually omit one, silently, and a silent omission means two different forms produce the same digest. So the
 * field list here is not written down at all — it is read from the type's own serializer. Adding a property to a
 * canonicalized type changes its digest automatically. Nothing has to be remembered.
 *
 * The remaining risk is the inverse — that a field is added *deliberately* and the digest changes without anyone
 * noticing the compatibility break. That is what the pinned shape goldens are for (`CanonicalShapeGoldenTest`):
 * they fail on any descriptor change and force a conscious version decision.
 *
 * ## Why the encoding is unambiguous
 *
 * The old encoding joined map entries as `k=v` with `,`, inside fields joined with `;`. That is ambiguous by
 * construction: `{"a": 1.0, "b": 2.0}` and `{"a=1.0,b": 2.0}` both render `a=1.0,b=2.0`. Whether a caller could
 * actually inject such a key is a separate question from whether the encoding distinguishes them — it did not.
 * Here there are no delimiters to collide on: every string is length-prefixed, every number is fixed-width, and
 * every value carries a type tag. Two different values cannot produce the same bytes.
 *
 * Cross-platform determinism: no `Double.toString()` (whose output is not contractually identical across JVM,
 * Native and JS), no locale, no map iteration order (collections are ordered by the caller and validated), and
 * doubles are encoded as raw IEEE-754 bits with `-0.0` normalized to `0.0` and NaN/infinity rejected outright.
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions") // One override per encodable type is the AbstractEncoder contract, and the
// count IS the coverage: every type this encoder does not override falls through to encodeValue and fails
// closed. Splitting them would hide which types are handled.
internal class CanonicalEncoder private constructor(
    private val out: MutableList<Byte>,
) : AbstractEncoder() {

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        tag(TAG_BEGIN)
        // The serialName carries the sealed subtype, so GoNoGo(go=true) and Vigilance(isSignal=true) — or two
        // distinct payloads with structurally identical fields — can never encode to the same bytes.
        string(descriptor.serialName)
        return this
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        tag(TAG_COLLECTION)
        string(descriptor.serialName)
        int(collectionSize)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) { tag(TAG_END) }

    /** Field NAME, not position: reordering a data class's properties must not change the digest. */
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        tag(TAG_FIELD)
        string(descriptor.getElementName(index))
        return true
    }

    /** Defaults are encoded too — a field left at its default still decides the form. */
    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = true

    override fun encodeString(value: String) {
        tag(TAG_STRING)
        string(value)
    }

    override fun encodeInt(value: Int) {
        tag(TAG_INT)
        int(value)
    }

    override fun encodeLong(value: Long) {
        tag(TAG_LONG)
        long(value)
    }

    override fun encodeBoolean(value: Boolean) {
        tag(TAG_BOOLEAN)
        out.add(if (value) 1 else 0)
    }

    override fun encodeDouble(value: Double) {
        if (value.isNaN() || value.isInfinite()) {
            throw CanonicalizationException(
                "A non-finite double cannot be canonicalized: NaN and the infinities have no single bit pattern " +
                    "(NaN has 2^52 of them) and no defensible meaning as a difficulty or timing. Reject at the " +
                    "boundary instead of digesting a value that will not compare equal to itself.",
            )
        }
        // -0.0 == 0.0 is true under IEEE, so equal values must not produce different bytes.
        val normalized = if (value == 0.0) 0.0 else value
        tag(TAG_DOUBLE)
        long(normalized.toRawBits())
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        tag(TAG_ENUM)
        string(enumDescriptor.serialName)
        // By NAME, never by ordinal: reordering enum entries must not silently rewrite history.
        string(enumDescriptor.getElementName(index))
    }

    override fun encodeNull() { tag(TAG_NULL) }

    override fun encodeNotNullMark() { tag(TAG_NOT_NULL) }

    /**
     * Anything not explicitly encoded above lands here and fails closed. If someone adds a `Float`, a `Char` or
     * a contextual type to a canonicalized model, they get a loud error rather than a quietly lossy digest.
     */
    override fun encodeValue(value: Any): Unit = throw CanonicalizationException(
        "No canonical encoding is defined for ${value::class.simpleName}. Add an explicit, fixed-width, " +
            "unambiguous encoding to CanonicalEncoder and publish a new CanonicalizerVersion — do not let it " +
            "fall back to a text representation.",
    )

    private fun tag(value: Byte) { out.add(value) }

    private fun string(value: String) {
        val bytes = value.encodeToByteArray()
        int(bytes.size)
        for (byte in bytes) out.add(byte)
    }

    private fun int(value: Int) {
        for (shift in INT_SHIFTS) out.add((value ushr shift).toByte())
    }

    private fun long(value: Long) {
        for (shift in LONG_SHIFTS) out.add((value ushr shift).toByte())
    }

    companion object {
        /**
         * Canonical bytes of [value] under a NAMED encoder version.
         *
         * The `when` is exhaustive on purpose: publishing V2 will not compile until someone decides, here, what
         * V2 does — and V1's branch must keep producing V1's bytes forever, because rows written under V1 are
         * validated by recomputing their digest. Editing V1's encoding would invalidate every existing row while
         * every test still passed, since the tests would move with it. That is why [CanonicalizerVersion.V1] is
         * frozen and `CanonicalV1GoldenTest` pins its bytes to literals.
         */
        fun <T> encode(version: CanonicalizerVersion, serializer: SerializationStrategy<T>, value: T): ByteArray =
            when (version) {
                CanonicalizerVersion.V1 -> encodeV1(serializer, value)
            }

        /** Lowercase hex SHA-256 of the canonical bytes of [value]. */
        fun <T> digest(version: CanonicalizerVersion, serializer: SerializationStrategy<T>, value: T): String =
            Sha256.hex(encode(version, serializer, value))

        private fun <T> encodeV1(serializer: SerializationStrategy<T>, value: T): ByteArray {
            val out = ArrayList<Byte>(INITIAL_CAPACITY)
            val encoder = CanonicalEncoder(out)
            // The version id is inside the digested bytes, so a V2 digest can never collide with a V1 digest.
            encoder.encodeString(CanonicalizerVersion.V1.id)
            encoder.encodeSerializableValue(serializer, value)
            return out.toByteArray()
        }

        private const val TAG_BEGIN: Byte = 0x01
        private const val TAG_END: Byte = 0x02
        private const val TAG_FIELD: Byte = 0x03
        private const val TAG_COLLECTION: Byte = 0x04
        private const val TAG_STRING: Byte = 0x10
        private const val TAG_INT: Byte = 0x11
        private const val TAG_LONG: Byte = 0x12
        private const val TAG_BOOLEAN: Byte = 0x13
        private const val TAG_DOUBLE: Byte = 0x14
        private const val TAG_NULL: Byte = 0x15
        private const val TAG_NOT_NULL: Byte = 0x16
        private const val TAG_ENUM: Byte = 0x17
        private const val INITIAL_CAPACITY = 256
        private val INT_SHIFTS = intArrayOf(24, 16, 8, 0)
        private val LONG_SHIFTS = intArrayOf(56, 48, 40, 32, 24, 16, 8, 0)
    }
}
