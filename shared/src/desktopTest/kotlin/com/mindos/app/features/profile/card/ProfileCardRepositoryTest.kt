package com.mindos.app.features.profile.card

import com.mindos.app.features.profile.card.data.InMemoryProfileCardRepository
import com.mindos.app.features.profile.card.domain.BackgroundPaint
import com.mindos.app.features.profile.card.domain.FlagOrTeam
import com.mindos.app.features.profile.card.domain.GradientKind
import com.mindos.app.features.profile.card.domain.ProfileCardAppearance
import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.ProfileCardMedia
import com.mindos.app.features.profile.card.domain.ProfileCardMediaKind
import com.mindos.app.features.profile.card.domain.ProfileRank
import com.mindos.app.features.profile.card.domain.RankDivision
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ProfileCardRepositoryTest {

    private fun appearance() = ProfileCardAppearance(
        accentArgb = 0xFFE8C77AL,
        screenBackground = BackgroundPaint.Gradient(listOf(0xFF1B2746L, 0xFF05070DL), GradientKind.RADIAL_TOP),
        cardBackground = BackgroundPaint.Solid(0xFF0E1426L),
        pegatinaIndex = 0,
        insigniaOn = true,
        flagOrTeam = FlagOrTeam.FLAG,
        level = 47,
        rank = ProfileRank.DIAMANTE,
        division = RankDivision.III,
    )

    @Test
    fun save_writesBothPrivateDraftAndPublicSnapshot() = runTest {
        val repo = InMemoryProfileCardRepository()
        val doc = ProfileCardDoc(appearance = appearance())
        repo.save("u1", doc)
        assertEquals(doc, repo.load("u1"))
        assertEquals(doc, repo.loadPublic("u1"))
        assertNull(repo.load("other"))
        assertNull(repo.loadPublic("other"))
    }

    @Test
    fun save_withBackgroundMedia_roundTrips() = runTest {
        val repo = InMemoryProfileCardRepository()
        val media = ProfileCardMedia(
            id = "m1",
            kind = ProfileCardMediaKind.IMAGE,
            storagePath = "profile_cards_public/u1/media/m1.jpg",
            downloadUrl = "https://test.local/m1",
        )
        val doc = ProfileCardDoc(appearance = appearance(), backgroundMedia = media)
        repo.save("u1", doc)
        assertEquals(media, repo.load("u1")?.backgroundMedia)
        assertEquals(media, repo.loadPublic("u1")?.backgroundMedia)
    }

    @Test
    fun save_propagatesConfiguredFailure() = runTest {
        val repo = InMemoryProfileCardRepository()
        repo.failSaveWith = IllegalStateException("firestore down")
        assertFailsWith<IllegalStateException> { repo.save("u1", ProfileCardDoc(appearance = appearance())) }
        // After a failure, nothing was persisted.
        assertNull(repo.load("u1"))
    }
}
