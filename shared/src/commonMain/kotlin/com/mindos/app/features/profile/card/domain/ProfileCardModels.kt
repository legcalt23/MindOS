@file:Suppress("MagicNumber") // Seed/sample content and color literals ARE the data for this screen.

package com.mindos.app.features.profile.card.domain

/**
 * Pure-Kotlin domain model for the MindBoost app profile card (design: Perfil.dc.html).
 *
 * This is the *application* profile (social/identity surface) — NOT the cognitive
 * "brain" profile (MindBoostProfile), which is a separate screen reached by swiping
 * left. Nothing here measures or claims cognitive ability; it models identity,
 * customization, social actions and a competitive rank surface.
 *
 * No Compose types live here so the state machine stays unit-testable on the JVM.
 */

/**
 * The ten competitive tiers, lowest → highest. Each tier has three divisions
 * (III → II → I, I being the highest within the tier). [LEYENDA] is the apex,
 * labelled "World Class".
 */
enum class ProfileRank(val displayName: String, val isWorldClass: Boolean = false) {
    BRONCE("Bronce"),
    PLATA("Plata"),
    ORO("Oro"),
    DIAMANTE("Diamante"),
    ZAFIRO("Zafiro"),
    RUBI("Rubí"),
    MAESTRO("Maestro"),
    GRAN_MAESTRO("Gran Maestro"),
    PRODIGIO("Prodigio"),
    LEYENDA("Leyenda", isWorldClass = true),
}

/** Division within a [ProfileRank]. III is entry, I is the top of the tier. */
enum class RankDivision(val label: String, val numeral: Int) {
    III("III", 3),
    II("II", 2),
    I("I", 1),
}

/**
 * A concrete rank placement: a [rank] tier plus a [division]. The apex tier
 * ([ProfileRank.LEYENDA]) ignores the division in its label ("World Class").
 */
data class RankTier(
    val rank: ProfileRank,
    val division: RankDivision,
) {
    /** Human label, e.g. "Diamante III" or "Leyenda · World Class". */
    val label: String
        get() = if (rank.isWorldClass) {
            "${rank.displayName} · World Class"
        } else {
            "${rank.displayName} ${division.label}"
        }

    companion object {
        val DEFAULT = RankTier(ProfileRank.DIAMANTE, RankDivision.III)
    }
}

/** A secondary, domain-specific badge shown beside the general rank. */
data class SecondaryBadge(
    val id: String,
    val glyph: String,
    val title: String,
    val rankLabel: String,
    val baseArgb: Long,
    val edgeArgb: Long,
)

/** Which of the three shared-box content panels is active. */
enum class ProfilePanel { CANVAS, POSTS, STATS }

/** Reputation vote the viewer has cast on this profile. */
enum class ReputationVote { NONE, LIKE, DISLIKE }

/** A flag vs team toggle for the identity slot. */
enum class FlagOrTeam { FLAG, TEAM }

/**
 * A draggable/resizable media item placed on the free canvas (panel 0).
 * [imageRef] is an opaque platform reference (null for the seeded placeholders,
 * which render [placeholderGradient] instead).
 */
data class CanvasItem(
    val id: Long,
    val kind: CanvasItemKind,
    val label: String = "",
    val imageRef: String? = null,
    val placeholderGradientStart: Long = 0xFF241A2E,
    val placeholderGradientEnd: Long = 0xFF1F1628,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val z: Int,
) {
    val isPlaceholder: Boolean get() = kind == CanvasItemKind.PLACEHOLDER
}

enum class CanvasItemKind { PLACEHOLDER, IMAGE, GIF, VIDEO }

/** A social post tile in the 4x4 grid (panel 1). */
data class ProfilePost(
    val index: Int,
    val kind: PostKind,
    val gradientStart: Long,
    val gradientEnd: Long,
)

enum class PostKind { IMAGE, GIF, VIDEO }

/** A single stat chip in the stats panel (panel 2). */
data class StatChip(val value: String, val label: String)

/** A mode-distribution row in the stats panel. */
data class ModeShare(val name: String, val percent: Int)

/** Paint for a customizable background surface (screen or card). */
sealed interface BackgroundPaint {
    data class Solid(val argb: Long) : BackgroundPaint
    data class Gradient(val stops: List<Long>, val kind: GradientKind) : BackgroundPaint
    data class Image(val ref: String) : BackgroundPaint
}

enum class GradientKind { LINEAR_DIAGONAL, RADIAL_TOP }

/**
 * The complete, immutable state of the profile card screen. The state holder
 * copies this on every interaction (copy-on-write).
 */
data class ProfileCardState(
    val username: String,
    val accountNumber: Int = 1,
    val level: Int = 47,
    val isOnline: Boolean = false,
    val generalRank: RankTier = RankTier.DEFAULT,
    val secondaryBadges: List<SecondaryBadge> = DefaultProfileContent.secondaryBadges,
    val accentArgb: Long = DefaultProfileContent.ACCENT,
    val screenBackground: BackgroundPaint = DefaultProfileContent.screenBackground,
    val cardBackground: BackgroundPaint = DefaultProfileContent.cardBackground,
    val pegatinaIndex: Int = 0,
    val insigniaOn: Boolean = true,
    val insigniaCount: Int = 12,
    val flagOrTeam: FlagOrTeam = FlagOrTeam.FLAG,
    val isFriend: Boolean = false,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val reputation: ReputationVote = ReputationVote.NONE,
    val likeCount: Int = 1284,
    val dislikeCount: Int = 37,
    // Prototype default is the canvas (tab 0). The earlier STATS default was a
    // workaround for the now-fixed background-paint bug; restored for fidelity.
    val activePanel: ProfilePanel = ProfilePanel.CANVAS,
    val canvasItems: List<CanvasItem> = DefaultProfileContent.canvasItems,
    val selectedCanvasItemId: Long? = null,
    val posts: List<ProfilePost> = DefaultProfileContent.posts,
    val statChips: List<StatChip> = DefaultProfileContent.statChips,
    val modes: List<ModeShare> = DefaultProfileContent.modes,
    val winRatePercent: Float = 57.8f,
    val performanceTrendPercent: Int = 14,
    val recentColors: List<Long> = DefaultProfileContent.recentColors,
    val savedColors: List<Long> = DefaultProfileContent.savedColors,
) {
    val pegatinaGlyph: String
        get() = DefaultProfileContent.pegatinas[pegatinaIndex % DefaultProfileContent.pegatinas.size]
}

/** Seed content mirroring the design's initial state. */
object DefaultProfileContent {
    const val ACCENT: Long = 0xFFE8C77A

    val pegatinas = listOf("🎯", "🚀", "👑", "🔥")

    val screenBackground: BackgroundPaint = BackgroundPaint.Gradient(
        stops = listOf(0xFF1B2746, 0xFF0B1020, 0xFF05070D),
        kind = GradientKind.RADIAL_TOP,
    )

    val cardBackground: BackgroundPaint = BackgroundPaint.Gradient(
        stops = listOf(0xFF1C2440, 0xFF0E1426),
        kind = GradientKind.LINEAR_DIAGONAL,
    )

    val secondaryBadges = listOf(
        SecondaryBadge("estratega", "♛", "Estratega", "Élite", 0xFF7FD0FF, 0xFF1D5FB0),
        SecondaryBadge("combate", "★", "Combate", "Maestro", 0xFFFFD98A, 0xFFB5742A),
        SecondaryBadge("creativo", "◆", "Creativo", "Leyenda", 0xFFC6A3FF, 0xFF6536B5),
    )

    val canvasItems = listOf(
        CanvasItem(
            id = 1, kind = CanvasItemKind.PLACEHOLDER, label = "foto",
            placeholderGradientStart = 0xFF241A2E, placeholderGradientEnd = 0xFF1F1628,
            x = 18f, y = 20f, width = 150f, height = 104f, z = 1,
        ),
        CanvasItem(
            id = 2, kind = CanvasItemKind.PLACEHOLDER, label = "GIF",
            placeholderGradientStart = 0xFF16261F, placeholderGradientEnd = 0xFF122019,
            x = 118f, y = 128f, width = 120f, height = 120f, z = 2,
        ),
    )

    private val postGradients = listOf(
        0xFF1A1F30 to 0xFF11151F,
        0xFF241A2E to 0xFF1A1320,
        0xFF16261F to 0xFF0F1A14,
        0xFF2A2418 to 0xFF1C1810,
        0xFF1B2746 to 0xFF0F1830,
        0xFF2E1A24 to 0xFF1F1218,
    )

    private val postKinds = listOf(
        PostKind.IMAGE, PostKind.GIF, PostKind.VIDEO, PostKind.IMAGE,
        PostKind.VIDEO, PostKind.IMAGE, PostKind.GIF, PostKind.IMAGE,
        PostKind.IMAGE, PostKind.VIDEO, PostKind.IMAGE, PostKind.GIF,
        PostKind.IMAGE, PostKind.IMAGE, PostKind.VIDEO, PostKind.GIF,
    )

    val posts: List<ProfilePost> = postKinds.mapIndexed { index, kind ->
        val (start, end) = postGradients[index % postGradients.size]
        ProfilePost(index = index, kind = kind, gradientStart = start, gradientEnd = end)
    }

    val statChips = listOf(
        StatChip("1.284", "Partidas"),
        StatChip("742", "Victorias"),
        StatChip("12", "Racha"),
        StatChip("8.460", "Puntos"),
    )

    val modes = listOf(
        ModeShare("Ranked", 57),
        ModeShare("Casual", 31),
        ModeShare("Torneo", 12),
    )

    val recentColors = listOf(0xFFE8C77A, 0xFF4F7CFF, 0xFFE0556B, 0xFF3FBF8F)
    val savedColors = listOf(0xFF0E1E3D, 0xFFC9A24B)
}
