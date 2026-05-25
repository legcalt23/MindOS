package com.example.neuv3.features.social.domain.publishing

import com.example.neuv3.features.social.domain.valueobject.UserId

sealed interface PublishingCommand {
    val actorId: UserId
    val idempotencyKey: IdempotencyKey
    val trace: TraceContext
}

data class CreatePostCommand(
    override val actorId: UserId,
    val body: String,
    val visibility: PostVisibility = PostVisibility.Public,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    override val idempotencyKey: IdempotencyKey,
    override val trace: TraceContext,
) : PublishingCommand

data class EditPostCommand(
    override val actorId: UserId,
    val postId: PostId,
    val body: String,
    val mediaAttachments: List<PostMediaAttachment> = emptyList(),
    override val idempotencyKey: IdempotencyKey,
    override val trace: TraceContext,
) : PublishingCommand

data class DeletePostCommand(
    override val actorId: UserId,
    val postId: PostId,
    override val idempotencyKey: IdempotencyKey,
    override val trace: TraceContext,
) : PublishingCommand
