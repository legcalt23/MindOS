package com.mindos.app.features.profile.card.domain

/**
 * The owner-editable customization subset of the profile card — the only part the
 * user controls on their OWN profile, and therefore the only part safe to persist
 * as an owner-owned document. Social-graph state (friend/block/mute), aggregate
 * counters (reputation) and progression (level/rank as earned) are intentionally
 * NOT here: those need backend authority and are not client-writable.
 *
 * Pure Kotlin (no Compose) so it round-trips through serialization and tests.
 */
data class ProfileCardAppearance(
    val accentArgb: Long,
    val screenBackground: BackgroundPaint,
    val cardBackground: BackgroundPaint,
    val pegatinaIndex: Int,
    val insigniaOn: Boolean,
    val flagOrTeam: FlagOrTeam,
    val level: Int,
    val rank: ProfileRank,
    val division: RankDivision,
)

/** Extracts the persistable appearance from the full screen state. */
fun ProfileCardState.appearance(): ProfileCardAppearance = ProfileCardAppearance(
    accentArgb = accentArgb,
    screenBackground = screenBackground,
    cardBackground = cardBackground,
    pegatinaIndex = pegatinaIndex,
    insigniaOn = insigniaOn,
    flagOrTeam = flagOrTeam,
    level = level,
    rank = generalRank.rank,
    division = generalRank.division,
)

/** Returns a copy of the state with [appearance] applied (social/stat fields untouched). */
fun ProfileCardState.withAppearance(appearance: ProfileCardAppearance): ProfileCardState = copy(
    accentArgb = appearance.accentArgb,
    screenBackground = appearance.screenBackground,
    cardBackground = appearance.cardBackground,
    pegatinaIndex = appearance.pegatinaIndex,
    insigniaOn = appearance.insigniaOn,
    flagOrTeam = appearance.flagOrTeam,
    level = appearance.level,
    generalRank = RankTier(appearance.rank, appearance.division),
)
