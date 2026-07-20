package com.mindos.app.features.mindboost.focus2.domain.generator

import kotlinx.serialization.Serializable

/**
 * Focus V5 · S4 · tarea (1) + tarea (4) — the two facts the ENGINE needs from an exercise's spec, and no more.
 *
 * **This interface is the whole coupling story of Tracer B, written down as a type.** Before S4, the sealed
 * session descriptor stored a concrete [VectorFilterSpec] — the flanker's spec, with its `flankerCount`,
 * `separation` and `similarity` — even though the engine reads exactly two scalars out of it: the response
 * window (`FocusSessionRuntime` arms every deadline from it) and the total trial count
 * (`FocusSessionRestore.incoherent` checks the snapshot's index against it). Every other field of a
 * `VectorFilterSpec` is invisible to the engine. So the descriptor over-committed: it named Tracer A's
 * concrete type where it only ever needed those two neutral numbers, and a vigilance session could not be
 * sealed without fabricating flanker parameters it does not have.
 *
 * S4 is the slice whose entire point is to surface that over-commitment by building a second mechanic on the
 * same engine (plan §5/S4: "si el motor NECESITA cambios, se registra cada uno como hallazgo de acoplamiento").
 * This interface IS that one change, recorded as `MB-FOCUS-0040`: the descriptor's `spec` widens from the
 * concrete flanker type to this neutral pair, and NOTHING in the engine core moves — `FocusSessionRuntime`,
 * `FocusSessionRestore` and `FocusSessionReducer` already read only `responseWindowMs` and `totalTrials`, both
 * of which live here. It is a widening, not a behaviour change: the ideally-empty engine diff the tracer was
 * built to produce.
 *
 * It is `@Serializable` and `sealed` so the descriptor's field serialises polymorphically across the closed set
 * of exercise specs without a hand-registered module. A row written under one spec that a newer build does not
 * know decodes to a failure, not to a silent default — the same fail-closed contract the rest of this package
 * already keeps.
 */
@Serializable
sealed interface FocusExerciseSpec {

    /**
     * The response deadline, in milliseconds. The runtime multiplies this into nanos and arms every trial's
     * window from it and nothing else, so the sealed spec — never a call site — is the authority on how long a
     * person has to answer.
     */
    val responseWindowMs: Long

    /**
     * The total number of planned trials. The restore path rejects any snapshot whose trial index or resolved
     * count exceeds this, because a session cannot have reached a trial its own plan never contained.
     */
    val totalTrials: Int
}
