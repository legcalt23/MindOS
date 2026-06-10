package com.mindos.app.design

/**
 * Centralized animation duration/easing tokens for MindOS.
 *
 * ALL animation durations should reference these constants.
 */
object AppAnimation {

    /** Quick micro-interaction (state toggle, opacity change). */
    const val DurationFast: Int = 150

    /** Standard transition (expand, slide, fade). */
    const val DurationMedium: Int = 300

    /** Slow/emphasis transition (page enter, complex transform). */
    const val DurationSlow: Int = 500

    /** Extra slow for dramatic transitions (intro, full-screen reveal). */
    const val DurationDramatic: Int = 800

    /** Stagger delay between sequential animations (list items). */
    const val StaggerDelay: Long = 50L
}
