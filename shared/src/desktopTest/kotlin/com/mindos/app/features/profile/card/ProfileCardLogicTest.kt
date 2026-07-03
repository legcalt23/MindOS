package com.mindos.app.features.profile.card

import com.mindos.app.features.profile.card.domain.ProfileCardState
import com.mindos.app.features.profile.card.domain.ProfilePanel
import com.mindos.app.features.profile.card.domain.ProfileRank
import com.mindos.app.features.profile.card.domain.RankDivision
import com.mindos.app.features.profile.card.domain.RankTier
import com.mindos.app.features.profile.card.domain.ReputationVote
import com.mindos.app.features.profile.card.presentation.ColorMath
import com.mindos.app.features.profile.card.presentation.ProfileCardStateHolder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileCardLogicTest {

    private fun holder() = ProfileCardStateHolder(ProfileCardState(username = "VXNESA"))

    @Test
    fun rankLabel_usesDivisionForTiers_andWorldClassForLegend() {
        assertEquals("Diamante III", RankTier(ProfileRank.DIAMANTE, RankDivision.III).label)
        assertEquals("Rubí I", RankTier(ProfileRank.RUBI, RankDivision.I).label)
        assertEquals("Leyenda · World Class", RankTier(ProfileRank.LEYENDA, RankDivision.II).label)
    }

    @Test
    fun allTenTiers_areOrderedBronzeToLegend() {
        val order = ProfileRank.entries.map { it.displayName }
        assertEquals(
            listOf(
                "Bronce", "Plata", "Oro", "Diamante", "Zafiro",
                "Rubí", "Maestro", "Gran Maestro", "Prodigio", "Leyenda",
            ),
            order,
        )
        assertTrue(ProfileRank.LEYENDA.isWorldClass)
    }

    @Test
    fun colorMath_hsvArgbRoundTripsWithinRounding() {
        listOf(0xFFE8C77AL, 0xFF4F7CFFL, 0xFF3FBF8FL, 0xFFE0556BL).forEach { argb ->
            val hsv = ColorMath.argbToHsv(argb)
            val back = ColorMath.hsvToArgb(hsv.hue, hsv.saturation, hsv.value)
            fun channel(v: Long, shift: Int) = (v shr shift) and 0xFF
            listOf(16, 8, 0).forEach { shift ->
                assertTrue(
                    abs(channel(argb, shift) - channel(back, shift)) <= 1,
                    "channel $shift mismatch for ${ColorMath.toHex(argb)} -> ${ColorMath.toHex(back)}",
                )
            }
        }
    }

    @Test
    fun like_then_likeAgain_togglesBackToNeutral() {
        val h = holder()
        val base = h.state.likeCount
        h.setLike()
        assertEquals(ReputationVote.LIKE, h.state.reputation)
        assertEquals(base + 1, h.state.likeCount)
        h.setLike()
        assertEquals(ReputationVote.NONE, h.state.reputation)
        assertEquals(base, h.state.likeCount)
    }

    @Test
    fun switchingFromLikeToDislike_movesTheVote() {
        val h = holder()
        val likeBase = h.state.likeCount
        val dislikeBase = h.state.dislikeCount
        h.setLike()
        h.setDislike()
        assertEquals(ReputationVote.DISLIKE, h.state.reputation)
        assertEquals(likeBase, h.state.likeCount)
        assertEquals(dislikeBase + 1, h.state.dislikeCount)
    }

    @Test
    fun canvas_addPlaceholder_appendsAndSelects() {
        val h = holder()
        val before = h.state.canvasItems.size
        h.addPlaceholder()
        assertEquals(before + 1, h.state.canvasItems.size)
        assertEquals(h.state.canvasItems.last().id, h.state.selectedCanvasItemId)
    }

    @Test
    fun selectPanel_updatesActivePanel() {
        val h = holder()
        h.selectPanel(ProfilePanel.STATS)
        assertEquals(ProfilePanel.STATS, h.state.activePanel)
    }
}
