package com.example.mindos.features.social.domain

import com.example.mindos.features.social.data.local.InMemorySocialGraphComponent
import com.example.mindos.features.social.domain.error.BlockedRelationship
import com.example.mindos.features.social.domain.error.SelfBlockNotAllowed
import com.example.mindos.features.social.domain.error.SelfFollowNotAllowed
import com.example.mindos.features.social.domain.model.FollowRequestStatus
import com.example.mindos.features.social.domain.model.PrivacySettings
import com.example.mindos.features.social.domain.model.ProfileVisibility
import com.example.mindos.features.social.domain.usecase.BlockUserResult
import com.example.mindos.features.social.domain.usecase.FollowRequestResolutionResult
import com.example.mindos.features.social.domain.usecase.FollowUserResult
import com.example.mindos.features.social.domain.usecase.RemoveFollowerResult
import com.example.mindos.features.social.domain.valueobject.DisplayName
import com.example.mindos.features.social.domain.valueobject.Handle
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.getOrThrow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SocialGraphRulesTest {
    private val alice = UserId("alice")
    private val bob = UserId("bob")

    @Test
    fun publicFollowCreatesActiveRelation() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob)

        val result = graph.useCases.followUser(alice, bob).getOrThrow()

        assertIs<FollowUserResult.Following>(result)
        assertTrue(graph.follows.exists(alice, bob))
        val counters = graph.useCases.getProfileCounters(bob).getOrThrow()
        assertEquals(1, counters.followersCount)
    }

    @Test
    fun privateFollowCreatesPendingRequest() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob, privateProfile = true)

        val result = graph.useCases.followUser(alice, bob).getOrThrow()

        val requested = assertIs<FollowUserResult.Requested>(result)
        assertEquals(FollowRequestStatus.Pending, requested.request.status)
        assertFalse(graph.follows.exists(alice, bob))
    }

    @Test
    fun acceptingRequestCreatesActiveFollow() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob, privateProfile = true)
        val request = assertIs<FollowUserResult.Requested>(
            graph.useCases.followUser(alice, bob).getOrThrow(),
        ).request

        val result = graph.useCases.acceptFollowRequest(bob, request.id).getOrThrow()

        assertIs<FollowRequestResolutionResult.Accepted>(result)
        assertTrue(graph.follows.exists(alice, bob))
        assertEquals(FollowRequestStatus.Accepted, graph.requests.get(request.id)?.status)
    }

    @Test
    fun denyingRequestDoesNotCreateFollow() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob, privateProfile = true)
        val request = assertIs<FollowUserResult.Requested>(
            graph.useCases.followUser(alice, bob).getOrThrow(),
        ).request

        val result = graph.useCases.denyFollowRequest(bob, request.id).getOrThrow()

        assertIs<FollowRequestResolutionResult.Denied>(result)
        assertFalse(graph.follows.exists(alice, bob))
        assertEquals(FollowRequestStatus.Denied, graph.requests.get(request.id)?.status)
    }

    @Test
    fun cancelingRequestResolvesItWithoutFollow() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob, privateProfile = true)
        val request = assertIs<FollowUserResult.Requested>(
            graph.useCases.followUser(alice, bob).getOrThrow(),
        ).request

        val result = graph.useCases.cancelFollowRequest(alice, request.id).getOrThrow()

        assertIs<FollowRequestResolutionResult.Canceled>(result)
        assertFalse(graph.follows.exists(alice, bob))
        assertEquals(FollowRequestStatus.Canceled, graph.requests.get(request.id)?.status)
    }

    @Test
    fun blockingRemovesFollowsAndInvalidatesPendingRequests() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob, privateProfile = true)
        graph.useCases.followUser(bob, alice).getOrThrow()
        val request = assertIs<FollowUserResult.Requested>(
            graph.useCases.followUser(alice, bob).getOrThrow(),
        ).request

        val result = graph.useCases.blockUser(alice, bob).getOrThrow()

        val blocked = assertIs<BlockUserResult.Blocked>(result)
        assertEquals(1, blocked.removedFollowCount)
        assertEquals(1, blocked.invalidatedRequestCount)
        assertFalse(graph.follows.exists(alice, bob))
        assertFalse(graph.follows.exists(bob, alice))
        assertEquals(FollowRequestStatus.InvalidatedByBlock, graph.requests.get(request.id)?.status)
    }

    @Test
    fun blockingPreventsFutureFollow() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob)
        graph.useCases.blockUser(alice, bob).getOrThrow()

        val result = graph.useCases.followUser(bob, alice)

        val failure = assertIs<DomainResult.Failure>(result)
        assertIs<BlockedRelationship>(failure.error)
    }

    @Test
    fun selfFollowIsRejected() = runTest {
        val graph = fixture()
        graph.seed(alice)

        val result = graph.useCases.followUser(alice, alice)

        val failure = assertIs<DomainResult.Failure>(result)
        assertEquals(SelfFollowNotAllowed, failure.error)
    }

    @Test
    fun selfBlockIsRejected() = runTest {
        val graph = fixture()
        graph.seed(alice)

        val result = graph.useCases.blockUser(alice, alice)

        val failure = assertIs<DomainResult.Failure>(result)
        assertEquals(SelfBlockNotAllowed, failure.error)
    }

    @Test
    fun duplicateFollowIsIdempotent() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob)

        graph.useCases.followUser(alice, bob).getOrThrow()
        val second = graph.useCases.followUser(alice, bob).getOrThrow()

        assertIs<FollowUserResult.AlreadyFollowing>(second)
        assertEquals(1, graph.useCases.getProfileCounters(alice).getOrThrow().followingCount)
    }

    @Test
    fun duplicatePendingRequestIsIdempotent() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob, privateProfile = true)

        graph.useCases.followUser(alice, bob).getOrThrow()
        val second = graph.useCases.followUser(alice, bob).getOrThrow()

        assertIs<FollowUserResult.RequestAlreadyPending>(second)
        assertEquals(1, graph.useCases.listOutgoingFollowRequests(alice).getOrThrow().items.size)
    }

    @Test
    fun relationshipStateReflectsRequestsFollowsAndMutuality() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob, privateProfile = true)
        val request = assertIs<FollowUserResult.Requested>(
            graph.useCases.followUser(alice, bob).getOrThrow(),
        ).request
        graph.useCases.followUser(bob, alice).getOrThrow()

        val pendingState = graph.useCases.getRelationshipState(alice, bob).getOrThrow()
        assertTrue(pendingState.outgoingRequestPending)
        assertTrue(pendingState.followsYou)
        assertFalse(pendingState.canViewFullProfile)

        graph.useCases.acceptFollowRequest(bob, request.id).getOrThrow()
        val acceptedState = graph.useCases.getRelationshipState(alice, bob).getOrThrow()
        assertTrue(acceptedState.isFollowing)
        assertTrue(acceptedState.isMutual)
        assertTrue(acceptedState.canViewFullProfile)
    }

    @Test
    fun changingPrivacyDoesNotBreakExistingRelations() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob)
        graph.useCases.followUser(alice, bob).getOrThrow()

        graph.useCases.updatePrivacySettings(
            bob,
            PrivacySettings(userId = bob, profileVisibility = ProfileVisibility.Private),
        ).getOrThrow()

        val state = graph.useCases.getRelationshipState(alice, bob).getOrThrow()
        assertTrue(state.isFollowing)
        assertTrue(state.canViewFullProfile)
    }

    @Test
    fun removeFollowerDeletesIncomingFollow() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob)
        graph.useCases.followUser(alice, bob).getOrThrow()

        val result = graph.useCases.removeFollower(bob, alice).getOrThrow()

        assertIs<RemoveFollowerResult.Removed>(result)
        assertFalse(graph.follows.exists(alice, bob))
    }

    @Test
    fun unblockDoesNotRestoreFollowsOrRequests() = runTest {
        val graph = fixture()
        graph.seed(alice)
        graph.seed(bob)
        graph.useCases.followUser(alice, bob).getOrThrow()
        graph.useCases.blockUser(alice, bob).getOrThrow()
        graph.useCases.unblockUser(alice, bob).getOrThrow()

        val state = graph.useCases.getRelationshipState(alice, bob).getOrThrow()
        assertFalse(state.isFollowing)
        assertTrue(state.canFollow)
    }

    private fun fixture(): InMemorySocialGraphComponent =
        InMemorySocialGraphComponent.create()

    private suspend fun InMemorySocialGraphComponent.seed(
        id: UserId,
        privateProfile: Boolean = false,
    ) {
        seedUser(
            userId = id,
            handle = Handle.from(id.value).getOrThrow(),
            displayName = DisplayName.from(id.value).getOrThrow(),
            privacySettings = PrivacySettings(
                userId = id,
                profileVisibility = if (privateProfile) ProfileVisibility.Private else ProfileVisibility.Public,
            ),
        )
    }
}

