@file:Suppress("MagicNumber")

package com.mindos.app.features.personalidentity

import com.mindos.app.features.personalidentity.model.PersonalSignature
import com.mindos.app.features.personalidentity.model.SignaturePoint
import com.mindos.app.features.personalidentity.model.SignatureStroke
import com.mindos.app.features.personalidentity.model.normalizedAndSimplified
import com.mindos.app.features.personalidentity.model.simplified
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignatureGeometryTest {
    @Test
    fun normalizedAndSimplifiedClampsPoints() {
        val signature = PersonalSignature(
            id = "test",
            name = "Test",
            strokes = listOf(
                SignatureStroke(
                    points = listOf(
                        SignaturePoint(-1f, -1f, pressure = 2f),
                        SignaturePoint(0.5f, 0.5f),
                        SignaturePoint(2f, 2f),
                    ),
                ),
            ),
        ).normalizedAndSimplified()

        assertTrue(signature.isUsable)
        for (stroke in signature.strokes) {
            for (point in stroke.points) {
                assertTrue(point.x in 0f..1f)
                assertTrue(point.y in 0f..1f)
                assertTrue(point.pressure in 0f..1f)
            }
        }
    }

    @Test
    fun simplificationPreservesFinalPoint() {
        val stroke = SignatureStroke(
            listOf(
                SignaturePoint(0f, 0f),
                SignaturePoint(0.001f, 0.001f),
                SignaturePoint(1f, 1f),
            ),
        ).simplified()

        assertEquals(SignaturePoint(1f, 1f), stroke.points.last())
    }

    @Test
    fun onePointStrokeIsNotUsable() {
        val signature = PersonalSignature(
            id = "test",
            name = "Test",
            strokes = listOf(SignatureStroke(listOf(SignaturePoint(0.1f, 0.1f)))),
        )

        assertEquals(false, signature.isUsable)
    }
}
