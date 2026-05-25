import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
  runTransaction,
  setDoc,
  updateDoc,
} from "firebase/firestore";

const projectId = "neuv3-username-rules";
const now = 1_700_000_000_000;

const testEnv = await initializeTestEnvironment({
  projectId,
  firestore: {
    rules: readFileSync("firestore.rules", "utf8"),
    host: "127.0.0.1",
    port: Number(process.env.FIRESTORE_EMULATOR_PORT ?? 8080),
  },
});

try {
  await testEnv.clearFirestore();

  async function seedPrivacy(uid, profileVisibility = "Public") {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "privacy", uid), {
        userId: uid,
        profileVisibility,
        followersVisibility: "Everyone",
        followingVisibility: "Everyone",
        messagePermission: "Mutuals",
        allowMentionsFrom: "Followers",
        discoverable: true,
        showActivityStatus: false,
        updatedAt: now,
      });
    });
  }

  function post(postId, userId, timestamp = now) {
    return {
      id: postId,
      userId,
      authorUsername: userId,
      authorDisplayName: `User ${userId}`,
      postType: "TEXT",
      text: `Post by ${userId}`,
      attachments: [],
      createdAt: timestamp,
      updatedAt: timestamp,
    };
  }

  async function seedPost(postId, userId, timestamp = now) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const data = post(postId, userId, timestamp);
      await setDoc(doc(context.firestore(), "posts", postId), data);
      await setDoc(doc(context.firestore(), "user_posts", userId, "posts", postId), data);
    });
  }

  async function seedFollow(followerId, followeeId) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "follows", `${followerId}_${followeeId}`), {
        followerId,
        followeeId,
        createdAt: now,
      });
    });
  }

  async function seedBlock(blockerId, blockedId) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "blocks", `${blockerId}_${blockedId}`), {
        blockerId,
        blockedId,
        createdAt: now,
      });
    });
  }

  async function seedDMChat(chatId, participants, timestamp = now) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "dm_chats", chatId), dmChat(chatId, participants, timestamp));
    });
  }

  async function seedMaterializedFeedEntry(viewerId, data) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "feeds", viewerId, "posts", data.id), {
        viewerId,
        postId: data.id,
        post: data,
        createdAt: data.createdAt,
      });
    });
  }

  function profile(uid, username, timestamp = now, usernameSearchTokens = []) {
    return {
      schemaVersion: 1,
      userId: uid,
      displayName: `User ${uid}`,
      username,
      userphrase: null,
      mainProfilePhotoUrl: null,
      searchable: true,
      directoryVisible: true,
      usernameSearchTokens,
      observersCount: 0,
      identityPosition: "Top",
      primaryColor: 0,
      secondaryColor: 1,
      categoryIconsConfig: ["Art"],
      layoutVersion: 1,
      layoutPreset: "LargeLeft",
      modules: [
        {
          id: "main",
          size: "Large",
          slot: "LargeLeft",
          contentType: "Empty",
          text: null,
          media: null,
        },
        {
          id: "small_1",
          size: "Small",
          slot: "SmallTopRight",
          contentType: "Empty",
          text: null,
          media: null,
        },
        {
          id: "small_2",
          size: "Small",
          slot: "SmallBottomRight",
          contentType: "Empty",
          text: null,
          media: null,
        },
      ],
      createdAt: timestamp,
      updatedAt: timestamp,
    };
  }

  function claim(uid, username, timestamp = now) {
    return {
      uid,
      username,
      createdAt: timestamp,
      updatedAt: timestamp,
    };
  }

  function dmChat(chatId, participants, timestamp = now) {
    return {
      participants,
      participantKey: chatId,
      lastMessage: null,
      lastMessageType: null,
      lastTimestamp: timestamp,
      updatedAt: timestamp,
      lastReadAtByUser: {},
    };
  }

  function dmMessage(chatId, messageId, senderId, timestamp = now) {
    return {
      chatId,
      senderId,
      type: "Text",
      status: "Sent",
      text: `Message ${messageId}`,
      createdAt: timestamp,
    };
  }

  async function claimAndCreateProfile(db, uid, username) {
    await runTransaction(db, async (tx) => {
      tx.set(doc(db, "usernames", username), claim(uid, username));
      tx.set(doc(db, "profile_visuals", uid), profile(uid, username));
    });
  }

  async function seedClaimAndProfile(uid, username, usernameSearchTokens = []) {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "usernames", username), claim(uid, username));
      await setDoc(
        doc(context.firestore(), "profile_visuals", uid),
        profile(uid, username, now, usernameSearchTokens),
      );
    });
  }

  async function changeUsername(db, uid, previousUsername, nextUsername) {
    await runTransaction(db, async (tx) => {
      tx.set(doc(db, "usernames", nextUsername), claim(uid, nextUsername, now + 1));
      tx.update(doc(db, "profile_visuals", uid), {
        username: nextUsername,
        updatedAt: now + 1,
      });
      tx.delete(doc(db, "usernames", previousUsername));
    });
  }

  await seedPrivacy("alice");
  await seedPrivacy("bob");

  const alice = testEnv.authenticatedContext("alice").firestore();
  const bob = testEnv.authenticatedContext("bob").firestore();

  await assertSucceeds(claimAndCreateProfile(alice, "alice", "alpha"));

  await assertFails(
    setDoc(doc(bob, "profile_visuals", "bob"), profile("bob", "alpha")),
  );

  await assertSucceeds(claimAndCreateProfile(bob, "bob", "bravo"));

  await assertFails(
    updateDoc(doc(bob, "profile_visuals", "bob"), {
      username: "alpha",
      updatedAt: now + 1,
    }),
  );

  await assertSucceeds(changeUsername(alice, "alice", "alpha", "beta"));

  await assertFails(
    setDoc(doc(bob, "usernames", "admin"), claim("bob", "admin")),
  );

  await assertFails(
    setDoc(doc(bob, "profile_visuals", "bob2"), profile("bob2", "charlie")),
  );

  await assertFails(deleteDoc(doc(bob, "usernames", "beta")));

  await seedPrivacy("carol");
  const carol = testEnv.authenticatedContext("carol").firestore();
  await assertFails(
    runTransaction(carol, async (tx) => {
      tx.set(doc(carol, "usernames", "charlie"), claim("carol", "charlie"));
      tx.set(
        doc(carol, "profile_visuals", "carol"),
        profile("carol", "charlie", now, ["cha", "charlie"]),
      );
    }),
  );

  await seedClaimAndProfile("alice", "beta", ["be", "bet", "beta"]);
  await assertSucceeds(
    updateDoc(doc(alice, "profile_visuals", "alice"), {
      userphrase: "updated without touching search tokens",
      primaryColor: 42,
      updatedAt: now + 2,
    }),
  );
  await assertFails(
    updateDoc(doc(alice, "profile_visuals", "alice"), {
      usernameSearchTokens: deleteField(),
      updatedAt: now + 3,
    }),
  );
  await assertFails(
    updateDoc(doc(alice, "profile_visuals", "alice"), {
      usernameSearchTokens: ["be", "bet", "beta", "fake-ranking-token"],
      updatedAt: now + 3,
    }),
  );

  await testEnv.clearFirestore();

  await seedPrivacy("alice", "Public");
  await seedPrivacy("bob", "Public");
  await seedPrivacy("carol", "Private");
  await seedPost("post_alice_public", "alice");
  await seedPost("post_bob_public", "bob");
  await seedPost("post_carol_private", "carol");

  await assertFails(
    getDocs(query(collection(bob, "posts"), limit(100))),
  );

  await assertSucceeds(
    getDoc(doc(alice, "posts", "post_alice_public")),
  );

  await assertSucceeds(
    getDoc(doc(bob, "posts", "post_alice_public")),
  );

  await assertFails(
    getDoc(doc(bob, "posts", "post_carol_private")),
  );

  await seedFollow("bob", "carol");
  await assertSucceeds(
    getDoc(doc(bob, "posts", "post_carol_private")),
  );

  await assertSucceeds(
    getDocs(query(collection(bob, "user_posts", "alice", "posts"), limit(25))),
  );

  await seedBlock("alice", "bob");
  await assertFails(
    getDoc(doc(bob, "posts", "post_alice_public")),
  );

  await seedMaterializedFeedEntry("bob", post("post_bob_public", "bob"));
  await assertSucceeds(
    getDocs(query(collection(bob, "feeds", "bob", "posts"), limit(25))),
  );
  await assertFails(
    getDocs(query(collection(alice, "feeds", "bob", "posts"), limit(25))),
  );

  await testEnv.clearFirestore();

  await seedPrivacy("alice", "Public");
  await seedPrivacy("bob", "Public");
  await seedPost("post_alice_delete", "alice");
  await seedPost("post_bob_delete", "bob");

  await assertFails(
    updateDoc(doc(alice, "posts", "post_bob_delete"), {
      userId: "alice",
      updatedAt: now + 1,
    }),
  );
  await assertFails(
    deleteDoc(doc(bob, "posts", "post_alice_delete")),
  );
  await assertFails(
    deleteDoc(doc(testEnv.unauthenticatedContext().firestore(), "posts", "post_alice_delete")),
  );
  await assertSucceeds(
    deleteDoc(doc(alice, "posts", "post_alice_delete")),
  );

  await assertFails(
    deleteDoc(doc(bob, "user_posts", "alice", "posts", "post_alice_delete")),
  );
  await assertFails(
    deleteDoc(doc(testEnv.unauthenticatedContext().firestore(), "user_posts", "alice", "posts", "post_alice_delete")),
  );
  await assertSucceeds(
    deleteDoc(doc(alice, "user_posts", "alice", "posts", "post_alice_delete")),
  );

  await testEnv.clearFirestore();

  const chatId = "dm1_5_alice_3_bob";
  const messageId = "msg_001";
  const aliceChatRef = doc(alice, "dm_chats", chatId);
  const bobChatRef = doc(bob, "dm_chats", chatId);
  const bobMessageRef = doc(bob, "dm_chats", chatId, "messages", messageId);

  await assertSucceeds(setDoc(aliceChatRef, dmChat(chatId, ["alice", "bob"])));
  await assertSucceeds(
    setDoc(doc(alice, "dm_chats", chatId, "messages", "msg_000"), dmMessage(chatId, "msg_000", "alice")),
  );

  await assertFails(
    setDoc(bobMessageRef, dmMessage(chatId, messageId, "alice")),
  );

  await assertFails(
    updateDoc(bobChatRef, {
      participants: ["alice", "bob", "carol"],
      updatedAt: now + 1,
    }),
  );

  await testEnv.clearFirestore();
  await seedBlock("alice", "bob");

  await assertFails(setDoc(bobChatRef, dmChat(chatId, ["alice", "bob"])));
  await assertFails(setDoc(aliceChatRef, dmChat(chatId, ["alice", "bob"])));

  await seedDMChat(chatId, ["alice", "bob"]);

  await assertFails(
    setDoc(bobMessageRef, dmMessage(chatId, messageId, "bob")),
  );

  await assertFails(
    updateDoc(bobChatRef, {
      lastMessage: "blocked bump",
      lastMessageType: "Text",
      lastTimestamp: now + 1,
      updatedAt: now + 1,
    }),
  );

  await assertFails(
    setDoc(doc(carol, "dm_chats", chatId, "messages", "msg_002"), dmMessage(chatId, "msg_002", "carol")),
  );

  console.log("Firestore username, posts privacy, and DM block rules tests passed");
} finally {
  await testEnv.cleanup();
}
