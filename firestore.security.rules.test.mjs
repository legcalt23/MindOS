import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  orderBy,
  query,
  setDoc,
  updateDoc,
  deleteDoc,
  where,
} from "firebase/firestore";

const projectId = "mindos-security-audit";
const now = 1_700_000_000_000;
let passed = 0;
let failed = 0;

function ok(label) {
  passed++;
  console.log(`  ✅ ${label}`);
}

async function test(label, fn) {
  try {
    await fn();
    ok(label);
  } catch (e) {
    failed++;
    console.error(`  ❌ ${label}: ${e.message}`);
  }
}

const testEnv = await initializeTestEnvironment({
  projectId,
  firestore: {
    rules: readFileSync("firestore.rules", "utf8"),
    host: "127.0.0.1",
    port: Number(process.env.FIRESTORE_EMULATOR_PORT ?? 8080),
  },
});

async function seed(path, data) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), ...path.split("/")), data);
  });
}

async function seedPrivacy(uid, vis = "Public") {
  await seed(`privacy/${uid}`, {
    userId: uid,
    profileVisibility: vis,
    followersVisibility: "Everyone",
    followingVisibility: "Everyone",
    messagePermission: "Mutuals",
    allowMentionsFrom: "Followers",
    discoverable: true,
    showActivityStatus: false,
    updatedAt: now,
  });
}

function profileData(uid, username) {
  return {
    schemaVersion: 1, userId: uid, displayName: `User ${uid}`,
    username, userphrase: null, mainProfilePhotoUrl: null,
    searchable: true, directoryVisible: true, usernameSearchTokens: [],
    observersCount: 0, identityPosition: "Top",
    primaryColor: 0, secondaryColor: 1, categoryIconsConfig: ["Art"],
    layoutVersion: 1, layoutPreset: "LargeLeft",
    modules: [
      { id: "main", size: "Large", slot: "LargeLeft", contentType: "Empty" },
      { id: "small_1", size: "Small", slot: "SmallTopRight", contentType: "Empty" },
      { id: "small_2", size: "Small", slot: "SmallBottomRight", contentType: "Empty" },
    ],
    createdAt: now, updatedAt: now,
  };
}

function userAccount(uid) {
  return { id: uid, handle: uid, status: "Active", createdAt: now, updatedAt: now };
}

function counterData(uid) {
  return {
    userId: uid, followersCount: 0, followingCount: 0,
    incomingPendingRequestsCount: 0, outgoingPendingRequestsCount: 0,
    blockedUsersCount: 0, derivedAt: now,
  };
}

function dmChatData(chatId, participants) {
  return {
    participants, participantKey: chatId,
    lastMessage: null, lastMessageType: null,
    lastTimestamp: now, updatedAt: now, lastReadAtByUser: {},
  };
}

const unauth = testEnv.unauthenticatedContext().firestore();

try {
  // ================================================================
  console.log("\n=== SECTION 1: Unauthenticated access denied ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice");
  await seed("privacy/alice", {
    userId: "alice", profileVisibility: "Public",
    followersVisibility: "Everyone", followingVisibility: "Everyone",
    messagePermission: "Mutuals", allowMentionsFrom: "Followers",
    discoverable: true, showActivityStatus: false, updatedAt: now,
  });
  await seed("profile_visuals/alice", profileData("alice", "alice"));
  await seed("users/alice", userAccount("alice"));
  await seed("usernames/alice", { uid: "alice", username: "alice", createdAt: now, updatedAt: now });

  await test("T1: Unauth cannot read private privacy doc", async () => {
    await assertFails(getDoc(doc(unauth, "privacy", "alice")));
  });

  await test("T2: Unauth cannot read user account", async () => {
    await assertFails(getDoc(doc(unauth, "users", "alice")));
  });

  await test("T3: Unauth cannot read profile_visuals", async () => {
    await assertFails(getDoc(doc(unauth, "profile_visuals", "alice")));
  });

  await test("T4: Unauth cannot write to privacy", async () => {
    await assertFails(setDoc(doc(unauth, "privacy", "hacker"), {
      userId: "hacker", profileVisibility: "Public",
      followersVisibility: "Everyone", followingVisibility: "Everyone",
      messagePermission: "Mutuals", allowMentionsFrom: "Followers",
      discoverable: true, showActivityStatus: false, updatedAt: now,
    }));
  });

  await test("T5: Unauth cannot write to profile_visuals", async () => {
    await assertFails(setDoc(doc(unauth, "profile_visuals", "hacker"), profileData("hacker", "hacker")));
  });

  await test("T6: Unauth cannot read usernames", async () => {
    await assertFails(getDoc(doc(unauth, "usernames", "alice")));
  });

  // ================================================================
  console.log("\n=== SECTION 2: Cross-user access denied ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice", "Private");
  await seedPrivacy("bob", "Public");
  await seed("users/alice", userAccount("alice"));
  await seed("users/bob", userAccount("bob"));
  await seed("privacy/alice", {
    userId: "alice", profileVisibility: "Private",
    followersVisibility: "Everyone", followingVisibility: "Everyone",
    messagePermission: "Mutuals", allowMentionsFrom: "Followers",
    discoverable: true, showActivityStatus: false, updatedAt: now,
  });
  await seed("profile_visuals/alice", profileData("alice", "alice_handle"));
  await seed("usernames/alice_handle", { uid: "alice", username: "alice_handle", createdAt: now, updatedAt: now });
  await seed("counters/alice", counterData("alice"));

  const alice = testEnv.authenticatedContext("alice").firestore();
  const bob = testEnv.authenticatedContext("bob").firestore();

  await test("T7: Bob cannot read Alice's privacy doc", async () => {
    await assertFails(getDoc(doc(bob, "privacy", "alice")));
  });

  await test("T8: Bob cannot read Alice's user account", async () => {
    await assertFails(getDoc(doc(bob, "users", "alice")));
  });

  await test("T9: Bob cannot read private Alice's profile_visual", async () => {
    await assertFails(getDoc(doc(bob, "profile_visuals", "alice")));
  });

  await test("T10: Bob cannot write Alice's privacy doc", async () => {
    await assertFails(updateDoc(doc(bob, "privacy", "alice"), { updatedAt: now + 1 }));
  });

  await test("T11: Bob cannot write Alice's profile_visual", async () => {
    await assertFails(updateDoc(doc(bob, "profile_visuals", "alice"), { updatedAt: now + 1 }));
  });

  // ================================================================
  console.log("\n=== SECTION 3: Immutable field protection ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice", "Public");
  await seed("usernames/alpha", { uid: "alice", username: "alpha", createdAt: now, updatedAt: now });
  await seed("profile_visuals/alice", profileData("alice", "alpha"));

  await test("T12: Cannot update ownerId (userId)", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      userId: "bob", updatedAt: now + 1,
    }));
  });

  await test("T13: Cannot update createdAt", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      createdAt: now - 1000, updatedAt: now + 1,
    }));
  });

  await test("T14: Cannot update schemaVersion", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      schemaVersion: 2, updatedAt: now + 1,
    }));
  });

  await test("T15: Cannot update layoutVersion", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      layoutVersion: 2, updatedAt: now + 1,
    }));
  });

  await test("T16: Cannot update observersCount directly", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      observersCount: 999, updatedAt: now + 1,
    }));
  });

  await test("T17: Cannot update usernameSearchTokens", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      usernameSearchTokens: ["injected"], updatedAt: now + 1,
    }));
  });

  // ================================================================
  console.log("\n=== SECTION 4: Extra fields rejected (mass assignment) ===\n");
  // ================================================================

  await test("T18: Cannot add extra fields to profile_visual", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      isAdmin: true, updatedAt: now + 1,
    }));
  });

  await test("T19: Cannot add role field to profile_visual", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      role: "admin", updatedAt: now + 1,
    }));
  });

  await test("T20: Cannot add isPremium field", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      isPremium: true, updatedAt: now + 1,
    }));
  });

  await test("T21: Cannot add billingStatus field", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      billingStatus: "active", updatedAt: now + 1,
    }));
  });

  await test("T22: Cannot add permissions field", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      permissions: ["admin"], updatedAt: now + 1,
    }));
  });

  await test("T23: Cannot add securityFlags field", async () => {
    await assertFails(updateDoc(doc(alice, "profile_visuals", "alice"), {
      securityFlags: { bypass: true }, updatedAt: now + 1,
    }));
  });

  // ================================================================
  console.log("\n=== SECTION 5: Server-only collections deny all client writes ===\n");
  // ================================================================

  await test("T24: Cannot write to users collection", async () => {
    await assertFails(setDoc(doc(alice, "users", "alice"), userAccount("alice")));
  });

  await test("T25: Cannot write to follows collection", async () => {
    await assertFails(setDoc(doc(alice, "follows", "alice_bob"), {
      followerId: "alice", followeeId: "bob", createdAt: now,
    }));
  });

  await test("T26: Cannot write to blocks collection", async () => {
    await assertFails(setDoc(doc(alice, "blocks", "alice_bob"), {
      blockerId: "alice", blockedId: "bob", createdAt: now,
    }));
  });

  await test("T27: Cannot write to counters collection", async () => {
    await assertFails(setDoc(doc(alice, "counters", "alice"), counterData("alice")));
  });

  await test("T28: Cannot write to followRequests collection", async () => {
    await assertFails(setDoc(doc(alice, "followRequests", "req1"), {
      id: "req1", requesterId: "alice", targetId: "bob",
      status: "Pending", createdAt: now,
    }));
  });

  await test("T29: Cannot create profile_visits directly (callable-only)", async () => {
    await assertFails(setDoc(doc(alice, "profile_visits", "alice_bob"), {
      viewerUserId: "alice",
      profileOwnerId: "bob",
      hasVisitedProfile: true,
      firstVisitedAt: now,
      lastVisitedAt: now,
    }));
  });

  await test("T30: Cannot update profile_visits directly (callable-only)", async () => {
    await seed("profile_visits/alice_bob", {
      viewerUserId: "alice",
      profileOwnerId: "bob",
      hasVisitedProfile: true,
      firstVisitedAt: now,
      lastVisitedAt: now,
    });
    await assertFails(updateDoc(doc(alice, "profile_visits", "alice_bob"), {
      lastVisitedAt: now + 1,
    }));
  });

  await test("T31: Cannot write to auditEvents", async () => {
    await assertFails(setDoc(doc(alice, "auditEvents", "evt1"), { action: "test" }));
  });

  await test("T32: Cannot read auditEvents", async () => {
    await assertFails(getDoc(doc(alice, "auditEvents", "evt1")));
  });

  // ================================================================
  console.log("\n=== SECTION 6: Wallet collections fully denied ===\n");
  // ================================================================

  await test("T31: Cannot write to wallets", async () => {
    await assertFails(setDoc(doc(alice, "wallets", "alice"), { balance: 1000 }));
  });

  await test("T32: Cannot write to balance_snapshots", async () => {
    await assertFails(setDoc(doc(alice, "balance_snapshots", "alice"), { available: 1000 }));
  });

  await test("T33: Cannot write to ledger_transactions", async () => {
    await assertFails(setDoc(doc(alice, "ledger_transactions", "tx1"), { amount: 100 }));
  });

  await test("T34: Cannot read ledger_transactions", async () => {
    await assertFails(getDoc(doc(alice, "ledger_transactions", "tx1")));
  });

  await test("T35: Cannot write to ledger_entries", async () => {
    await assertFails(setDoc(doc(alice, "ledger_entries", "entry1"), { amount: 50 }));
  });

  await test("T36: Cannot read ledger_entries", async () => {
    await assertFails(getDoc(doc(alice, "ledger_entries", "entry1")));
  });

  await test("T37: Cannot write to idempotency_keys", async () => {
    await assertFails(setDoc(doc(alice, "idempotency_keys", "key1"), { status: "completed" }));
  });

  await test("T38: Cannot write to purchases", async () => {
    await assertFails(setDoc(doc(alice, "purchases", "p1"), { amount: 100 }));
  });

  await test("T39: Cannot write to fraud_signals", async () => {
    await assertFails(setDoc(doc(alice, "fraud_signals", "sig1"), { score: 99 }));
  });

  await test("T40: Cannot read fraud_signals", async () => {
    await assertFails(getDoc(doc(alice, "fraud_signals", "sig1")));
  });

  await test("T41: Cannot write to wallet_audit_logs", async () => {
    await assertFails(setDoc(doc(alice, "wallet_audit_logs", "log1"), { action: "test" }));
  });

  await test("T42: Cannot read wallet_audit_logs", async () => {
    await assertFails(getDoc(doc(alice, "wallet_audit_logs", "log1")));
  });

  await test("T43: Cannot write to admin_actions", async () => {
    await assertFails(setDoc(doc(alice, "admin_actions", "a1"), { action: "adjust" }));
  });

  await test("T44: Cannot read admin_actions", async () => {
    await assertFails(getDoc(doc(alice, "admin_actions", "a1")));
  });

  // ================================================================
  console.log("\n=== SECTION 7: Wallet owner-read allowed ===\n");
  // ================================================================

  await seed("wallets/alice", { status: "active", balance: 100 });
  await seed("balance_snapshots/alice", { available: 100 });

  await test("T45: Owner can read own wallet", async () => {
    await assertSucceeds(getDoc(doc(alice, "wallets", "alice")));
  });

  await test("T46: Owner can read own balance_snapshot", async () => {
    await assertSucceeds(getDoc(doc(alice, "balance_snapshots", "alice")));
  });

  await test("T47: Bob cannot read Alice's wallet", async () => {
    await assertFails(getDoc(doc(bob, "wallets", "alice")));
  });

  await test("T48: Bob cannot read Alice's balance_snapshot", async () => {
    await assertFails(getDoc(doc(bob, "balance_snapshots", "alice")));
  });

  // ================================================================
  console.log("\n=== SECTION 8: Bounded list queries enforced ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice", "Public");
  await seedPrivacy("bob", "Public");
  await seed("profile_visuals/alice", profileData("alice", "alice"));

  const chatId = "dm1_5_alice_3_bob";
  await seed(`dm_chats/${chatId}`, dmChatData(chatId, ["alice", "bob"]));
  await seed(`dm_chats/${chatId}/messages/msg001`, {
    chatId, senderId: "alice", type: "Text", status: "Sent",
    text: "hello", createdAt: now,
  });

  await test("T49: DM chats list with limit succeeds", async () => {
    // Must match the dm_chats `allow list` rule (uid() in resource.data.participants):
    // the query is constrained to the caller's chats, exactly like the production
    // client query (FirebaseDMRepository.getUserChats). A bare list without this
    // filter is correctly DENIED by the rule.
    await assertSucceeds(getDocs(query(
      collection(alice, "dm_chats"),
      where("participants", "array-contains", "alice"),
      limit(50),
    )));
  });

  await test("T50: DM messages list with limit succeeds", async () => {
    await assertSucceeds(getDocs(query(
      collection(alice, "dm_chats", chatId, "messages"), limit(100)
    )));
  });

  // Feed bounded list
  const postData = {
    id: "post1", userId: "alice", authorUsername: "alice",
    authorDisplayName: "User alice", postType: "TEXT", text: "hello",
    attachments: [], createdAt: now, updatedAt: now,
  };
  await seed("feeds/alice/posts/post1", {
    viewerId: "alice", postId: "post1", post: postData, createdAt: now,
  });

  await test("T51: Feed list with limit succeeds", async () => {
    await assertSucceeds(getDocs(query(
      collection(alice, "feeds", "alice", "posts"), limit(100)
    )));
  });

  // User posts bounded list
  await seed("user_posts/alice/posts/post1", postData);

  await test("T52: User posts list with limit succeeds", async () => {
    await assertSucceeds(getDocs(query(
      collection(alice, "user_posts", "alice", "posts"), limit(100)
    )));
  });

  // ================================================================
  console.log("\n=== SECTION 9: Unknown collections denied (catch-all) ===\n");
  // ================================================================

  await test("T53: Cannot read from unknown collection", async () => {
    await assertFails(getDoc(doc(alice, "unknown_collection", "doc1")));
  });

  await test("T54: Cannot write to unknown collection", async () => {
    await assertFails(setDoc(doc(alice, "unknown_collection", "doc1"), { data: "test" }));
  });

  await test("T55: Unauth cannot read unknown collection", async () => {
    await assertFails(getDoc(doc(unauth, "secret_stuff", "doc1")));
  });

  await test("T56: Cannot read from unknown subcollection", async () => {
    await assertFails(getDoc(doc(alice, "users", "alice", "secrets", "s1")));
  });

  // ================================================================
  console.log("\n=== SECTION 10: Privacy document validation ===\n");
  // ================================================================

  await testEnv.clearFirestore();

  await test("T57: Alice can create her privacy doc", async () => {
    await assertSucceeds(setDoc(doc(alice, "privacy", "alice"), {
      userId: "alice", profileVisibility: "Public",
      followersVisibility: "Everyone", followingVisibility: "Everyone",
      messagePermission: "Mutuals", allowMentionsFrom: "Followers",
      discoverable: true, showActivityStatus: false, updatedAt: now,
    }));
  });

  await test("T58: Alice cannot create Bob's privacy doc", async () => {
    await assertFails(setDoc(doc(alice, "privacy", "bob"), {
      userId: "bob", profileVisibility: "Public",
      followersVisibility: "Everyone", followingVisibility: "Everyone",
      messagePermission: "Mutuals", allowMentionsFrom: "Followers",
      discoverable: true, showActivityStatus: false, updatedAt: now,
    }));
  });

  await test("T59: Privacy doc with invalid visibility rejected", async () => {
    await assertFails(setDoc(doc(bob, "privacy", "bob"), {
      userId: "bob", profileVisibility: "INVALID",
      followersVisibility: "Everyone", followingVisibility: "Everyone",
      messagePermission: "Mutuals", allowMentionsFrom: "Followers",
      discoverable: true, showActivityStatus: false, updatedAt: now,
    }));
  });

  await test("T60: Privacy doc missing required fields rejected", async () => {
    await assertFails(setDoc(doc(bob, "privacy", "bob"), {
      userId: "bob", profileVisibility: "Public",
    }));
  });

  // ================================================================
  console.log("\n=== SECTION 11: Delete operations ===\n");
  // ================================================================

  await test("T61: Cannot delete privacy doc", async () => {
    await assertFails(deleteDoc(doc(alice, "privacy", "alice")));
  });

  await test("T62: Cannot delete profile_visuals", async () => {
    await seed("profile_visuals/alice", profileData("alice", "alpha"));
    await seed("usernames/alpha", { uid: "alice", username: "alpha", createdAt: now, updatedAt: now });
    await assertFails(deleteDoc(doc(alice, "profile_visuals", "alice")));
  });

  await test("T63: Cannot delete notifications", async () => {
    await seed("notifications/alice_bob", {
      notificationId: "alice_bob", recipientUserId: "bob",
      actorUserId: "alice", actorUsername: "alpha",
      actorDisplayName: "User alice", type: "Observed",
      timestamp: now, targetUserId: "bob", isRead: false,
    });
    await assertFails(deleteDoc(doc(bob, "notifications", "alice_bob")));
  });

  // ================================================================
  console.log("\n=== SECTION 12: DM participant isolation ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  const dmId = "dm1_5_alice_3_bob";
  await seed(`dm_chats/${dmId}`, dmChatData(dmId, ["alice", "bob"]));
  await seed(`dm_chats/${dmId}/messages/m1`, {
    chatId: dmId, senderId: "alice", type: "Text", status: "Sent",
    text: "private message", createdAt: now,
  });

  await seedPrivacy("carol", "Public");
  const carol = testEnv.authenticatedContext("carol").firestore();

  await test("T64: Carol cannot read DM chat between Alice and Bob", async () => {
    await assertFails(getDoc(doc(carol, "dm_chats", dmId)));
  });

  await test("T65: Carol cannot read DM messages", async () => {
    await assertFails(getDoc(doc(carol, "dm_chats", dmId, "messages", "m1")));
  });

  await test("T66: Carol cannot write DM messages", async () => {
    await assertFails(setDoc(doc(carol, "dm_chats", dmId, "messages", "m2"), {
      chatId: dmId, senderId: "carol", type: "Text", status: "Sent",
      text: "injected", createdAt: now,
    }));
  });

  await test("T67: DM chat cannot modify participants", async () => {
    await assertFails(updateDoc(doc(alice, "dm_chats", dmId), {
      participants: ["alice", "carol"],
      updatedAt: now + 1,
    }));
  });

  // ================================================================
  console.log("\n=== SECTION 13: Notification update restrictions ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice", "Public");
  await seed("profile_visuals/alice", profileData("alice", "alpha"));
  await seed("usernames/alpha", { uid: "alice", username: "alpha", createdAt: now, updatedAt: now });
  await seed("notifications/alice_bob", {
    notificationId: "alice_bob", recipientUserId: "bob",
    actorUserId: "alice", actorUsername: "alpha",
    actorDisplayName: "User alice", type: "Observed",
    timestamp: now, targetUserId: "bob", isRead: false,
  });

  await test("T68: Recipient can mark notification as read", async () => {
    await assertSucceeds(updateDoc(doc(bob, "notifications", "alice_bob"), {
      isRead: true,
    }));
  });

  await test("T69: Cannot modify notification type", async () => {
    await assertFails(updateDoc(doc(bob, "notifications", "alice_bob"), {
      type: "Hacked",
    }));
  });

  await test("T70: Cannot modify notification actorUserId", async () => {
    await assertFails(updateDoc(doc(bob, "notifications", "alice_bob"), {
      actorUserId: "hacker",
    }));
  });

  // ================================================================
  console.log("\n=== SECTION 14: Unbounded list query denial (fix verification) ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice", "Public");
  await seedPrivacy("bob", "Public");
  await seed("profile_visuals/alice", profileData("alice", "alice"));

  const dmChatId = "dm1_5_alice_3_bob";
  await seed(`dm_chats/${dmChatId}`, dmChatData(dmChatId, ["alice", "bob"]));
  await seed(`dm_chats/${dmChatId}/messages/m1`, {
    chatId: dmChatId, senderId: "alice", type: "Text",
    status: "Sent", text: "hi", createdAt: now,
  });

  const postBody = {
    id: "p1", userId: "alice", authorUsername: "alice",
    authorDisplayName: "User alice", postType: "TEXT", text: "hi",
    attachments: [], createdAt: now, updatedAt: now,
  };
  await seed("user_posts/alice/posts/p1", postBody);
  await seed("feeds/alice/posts/p1", {
    viewerId: "alice", postId: "p1", post: postBody, createdAt: now,
  });

  await test("T71: dm_chats list WITHOUT limit must FAIL (H1 verification)", async () => {
    await assertFails(getDocs(collection(alice, "dm_chats")));
  });

  await test("T72: dm_chats list with limit > 50 must FAIL (H1 verification)", async () => {
    await assertFails(getDocs(query(collection(alice, "dm_chats"), limit(51))));
  });

  await test("T73: dm_messages list WITHOUT limit must FAIL (M1 verification)", async () => {
    await assertFails(getDocs(collection(alice, "dm_chats", dmChatId, "messages")));
  });

  await test("T74: dm_messages list with limit > 100 must FAIL (M1 verification)", async () => {
    await assertFails(getDocs(query(
      collection(alice, "dm_chats", dmChatId, "messages"), limit(101)
    )));
  });

  await test("T75: feeds list WITHOUT limit must FAIL (H3 verification)", async () => {
    await assertFails(getDocs(collection(alice, "feeds", "alice", "posts")));
  });

  await test("T76: feeds list with limit > 100 must FAIL (H3 verification)", async () => {
    await assertFails(getDocs(query(
      collection(alice, "feeds", "alice", "posts"), limit(101)
    )));
  });

  await test("T77: user_posts list WITHOUT limit must FAIL (H2 verification)", async () => {
    await assertFails(getDocs(collection(alice, "user_posts", "alice", "posts")));
  });

  await test("T78: user_posts list with limit > 100 must FAIL (H2 verification)", async () => {
    await assertFails(getDocs(query(
      collection(alice, "user_posts", "alice", "posts"), limit(101)
    )));
  });

  // ================================================================
  console.log("\n=== SECTION 15: Cross-user denial tests for previously fixed rules ===\n");
  // ================================================================

  await test("T79: Non-participant cannot list DM chats containing other users", async () => {
    await assertFails(getDocs(query(collection(carol, "dm_chats"), limit(10))));
  });

  await test("T80: Non-owner cannot list user_posts of another user (private)", async () => {
    await seedPrivacy("dave", "Private");
    const dave = testEnv.authenticatedContext("dave").firestore();
    await seed("user_posts/dave/posts/p_dave", {
      id: "p_dave", userId: "dave", authorUsername: "dave",
      authorDisplayName: "User dave", postType: "TEXT", text: "private",
      attachments: [], createdAt: now, updatedAt: now,
    });
    // alice (not following dave, not blocked) tries to list dave's posts
    await assertFails(getDocs(query(
      collection(alice, "user_posts", "dave", "posts"), limit(50)
    )));
  });

  await test("T81: Non-owner cannot list another user's materialized feed", async () => {
    await assertFails(getDocs(query(
      collection(bob, "feeds", "alice", "posts"), limit(10)
    )));
  });

  // ================================================================
  console.log("\n=== SECTION 16: Observation read is participant-only (H25) ===\n");
  // ================================================================

  await seed("observations/alice_bob", {
    id: "alice_bob", observerUserId: "alice", observedUserId: "bob", createdAt: now,
  });

  await test("T82: Observer can read their own observation", async () => {
    await assertSucceeds(getDoc(doc(alice, "observations", "alice_bob")));
  });

  await test("T83: Observed user can read the observation about them", async () => {
    await assertSucceeds(getDoc(doc(bob, "observations", "alice_bob")));
  });

  await test("T84: Non-participant cannot read another pair's observation (H25)", async () => {
    await assertFails(getDoc(doc(carol, "observations", "alice_bob")));
  });

  // ================================================================
  console.log("\n=== SECTION 17: Block parity for profile discovery (P0-008) ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice", "Public");
  await seedPrivacy("bob", "Public");
  await seed("profile_visuals/bob", profileData("bob", "blocked_bob"));
  await seed("blocks/alice_bob", {
    blockerId: "alice", blockedId: "bob", createdAt: now,
  });

  await test("T85: Blocked profile direct get is denied", async () => {
    await assertFails(getDoc(doc(alice, "profile_visuals", "bob")));
  });

  await test("T86: Blocked profile cannot be rediscovered by username search", async () => {
    await assertFails(getDocs(query(
      collection(alice, "profile_visuals"),
      where("searchable", "==", true),
      where("directoryVisible", "==", true),
      where("username", ">=", "blocked"),
      where("username", "<=", "blocked\uf8ff"),
      orderBy("username"),
      limit(12),
    )));
  });

  // ================================================================
  console.log("\n=== SECTION 18: DM read receipt ownership (P0-009) ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  const receiptChatId = "dm1_5_alice_3_bob";
  await seed(`dm_chats/${receiptChatId}`, dmChatData(receiptChatId, ["alice", "bob"]));

  await test("T87: Participant can update only their own read receipt", async () => {
    await assertSucceeds(updateDoc(doc(alice, "dm_chats", receiptChatId), {
      lastReadAtByUser: { alice: now + 1 },
      updatedAt: now + 1,
    }));
  });

  await test("T88: Participant cannot forge the peer read receipt", async () => {
    await assertFails(updateDoc(doc(alice, "dm_chats", receiptChatId), {
      lastReadAtByUser: { alice: now + 1, bob: now + 2 },
      updatedAt: now + 2,
    }));
  });

  await test("T89: Read receipt map rejects non-participant keys", async () => {
    await assertFails(updateDoc(doc(alice, "dm_chats", receiptChatId), {
      lastReadAtByUser: { alice: now + 1, mallory: now + 2 },
      updatedAt: now + 2,
    }));
  });

  await test("T90: Peer can independently update their own read receipt", async () => {
    await assertSucceeds(updateDoc(doc(bob, "dm_chats", receiptChatId), {
      lastReadAtByUser: { alice: now + 1, bob: now + 2 },
      updatedAt: now + 2,
    }));
  });

  await test("T91: Chat creator cannot pre-populate the peer receipt", async () => {
    const forgedChatId = "dm1_5_alice_5_carol";
    await assertFails(setDoc(doc(alice, "dm_chats", forgedChatId), {
      ...dmChatData(forgedChatId, ["alice", "carol"]),
      lastReadAtByUser: { carol: now },
    }));
  });

  // ================================================================
  console.log("\n=== SECTION 19: Post writes are callable-only (P0-010/P0-011) ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  await seedPrivacy("alice", "Public");
  const directPost = {
    id: "direct_post", userId: "alice", authorUsername: "alice",
    authorDisplayName: "User alice", postType: "TEXT", text: "bypass",
    attachments: [], createdAt: now, updatedAt: now,
  };

  await test("T92: Modified client cannot create a global post without a profile", async () => {
    await assertFails(setDoc(doc(alice, "posts", "direct_post"), directPost));
  });

  await seed("profile_visuals/alice", profileData("alice", "alice"));

  await test("T93: Modified client cannot bypass callable post creation with a canonical profile", async () => {
    await assertFails(setDoc(doc(alice, "posts", "direct_post"), directPost));
  });

  await test("T94: Modified client cannot write the user_posts mirror", async () => {
    await assertFails(setDoc(doc(alice, "user_posts", "alice", "posts", "direct_post"), directPost));
  });

  // ================================================================
  console.log("\n=== SECTION: profile_cards private (owner-only) + public (authed read) ===\n");
  // ================================================================

  await testEnv.clearFirestore();
  const cardOwner = testEnv.authenticatedContext("cardowner").firestore();
  const cardIntruder = testEnv.authenticatedContext("intruder").firestore();
  const privateCard = { schemaVersion: 1, payload: '{"accentArgb":1}', updatedAt: now };
  const publicCard = { schemaVersion: 2, ownerUid: "cardowner", payload: '{"accentArgb":1}', updatedAt: now };

  // --- private draft (owner-only read + write) ---
  await test("T95: Owner can create their private card draft", async () => {
    await assertSucceeds(setDoc(doc(cardOwner, "profile_cards_private", "cardowner"), privateCard));
  });

  await test("T96: Owner can update their private card draft", async () => {
    await assertSucceeds(
      updateDoc(doc(cardOwner, "profile_cards_private", "cardowner"), { payload: '{"accentArgb":2}', updatedAt: now + 1 }),
    );
  });

  await test("T97: Owner can read their private card draft", async () => {
    await assertSucceeds(getDoc(doc(cardOwner, "profile_cards_private", "cardowner")));
  });

  await test("T98: Non-owner CANNOT read another user's private draft", async () => {
    await assertFails(getDoc(doc(cardIntruder, "profile_cards_private", "cardowner")));
  });

  await test("T99: Non-owner CANNOT write another user's private draft", async () => {
    await assertFails(setDoc(doc(cardIntruder, "profile_cards_private", "cardowner"), privateCard));
  });

  await test("T100: Unauth cannot read a private draft", async () => {
    await assertFails(getDoc(doc(unauth, "profile_cards_private", "cardowner")));
  });

  await test("T101: Oversized private payload is rejected", async () => {
    await assertFails(
      setDoc(doc(cardOwner, "profile_cards_private", "cardowner"), { schemaVersion: 1, payload: "x".repeat(16001), updatedAt: now }),
    );
  });

  await test("T102: Extra keys in private draft are rejected", async () => {
    await assertFails(
      setDoc(doc(cardOwner, "profile_cards_private", "cardowner"), { schemaVersion: 1, payload: "{}", updatedAt: now, evil: true }),
    );
  });

  // --- public snapshot (authenticated read, owner-only write) ---
  await test("T103: Owner CANNOT publish their public snapshot directly (server-only write)", async () => {
    await assertFails(setDoc(doc(cardOwner, "profile_cards_public", "cardowner"), publicCard));
  });

  await test("T104: Owner CANNOT update their public snapshot directly (server-only write)", async () => {
    await assertFails(
      updateDoc(doc(cardOwner, "profile_cards_public", "cardowner"), { payload: '{"accentArgb":9}', updatedAt: now + 1 }),
    );
  });

  await test("T105: Any authenticated user CAN read another user's public snapshot", async () => {
    await assertSucceeds(getDoc(doc(cardIntruder, "profile_cards_public", "cardowner")));
  });

  await test("T106: Unauth CANNOT read a public snapshot", async () => {
    await assertFails(getDoc(doc(unauth, "profile_cards_public", "cardowner")));
  });

  await test("T107: Non-owner CANNOT write another user's public snapshot", async () => {
    await assertFails(
      setDoc(doc(cardIntruder, "profile_cards_public", "cardowner"), { schemaVersion: 2, ownerUid: "intruder", payload: "{}", updatedAt: now }),
    );
  });

  await test("T108: Public snapshot with mismatched ownerUid is rejected", async () => {
    await assertFails(
      setDoc(doc(cardOwner, "profile_cards_public", "cardowner"), { schemaVersion: 2, ownerUid: "someoneelse", payload: "{}", updatedAt: now }),
    );
  });

  await test("T109: Oversized public payload is rejected", async () => {
    await assertFails(
      setDoc(doc(cardOwner, "profile_cards_public", "cardowner"), { schemaVersion: 2, ownerUid: "cardowner", payload: "x".repeat(16001), updatedAt: now }),
    );
  });

  // --- Marketplace Rules (Ola 1) ---
  const marketplaceUser = testEnv.authenticatedContext("marketplace_user");
  const marketplaceIntruder = testEnv.authenticatedContext("marketplace_intruder");

  await test("M01: Authed user can read marketplace_catalog", async () => {
    await assertSucceeds(getDoc(doc(marketplaceUser.firestore(), "marketplace_catalog", "prod_1")));
  });

  await test("M02: Unauthed user CANNOT read marketplace_catalog", async () => {
    await assertFails(getDoc(doc(unauth, "marketplace_catalog", "prod_1")));
  });

  await test("M03: Client CANNOT write marketplace_catalog", async () => {
    await assertFails(
      setDoc(doc(marketplaceUser.firestore(), "marketplace_catalog", "prod_1"), { status: "ACTIVE" })
    );
  });

  await test("M04: Authed user can read marketplace_assets", async () => {
    await assertSucceeds(getDoc(doc(marketplaceUser.firestore(), "marketplace_assets", "asset_1")));
  });

  await test("M05: Client CANNOT write marketplace_assets", async () => {
    await assertFails(
      setDoc(doc(marketplaceUser.firestore(), "marketplace_assets", "asset_1"), { status: "ACTIVE" })
    );
  });

  await test("M06: Owner can read their entitlements", async () => {
    await assertSucceeds(getDoc(doc(marketplaceUser.firestore(), "user_entitlements", "marketplace_user", "items", "ent_1")));
  });

  await test("M07: Non-owner CANNOT read user entitlements", async () => {
    await assertFails(getDoc(doc(marketplaceIntruder.firestore(), "user_entitlements", "marketplace_user", "items", "ent_1")));
  });

  await test("M08: Client CANNOT write user entitlements", async () => {
    await assertFails(
      setDoc(doc(marketplaceUser.firestore(), "user_entitlements", "marketplace_user", "items", "ent_1"), { status: "ACTIVE" })
    );
  });

  await test("M09: Owner can read their inventory", async () => {
    await assertSucceeds(getDoc(doc(marketplaceUser.firestore(), "user_inventory", "marketplace_user", "items", "item_1")));
  });

  await test("M10: Non-owner CANNOT read user inventory", async () => {
    await assertFails(getDoc(doc(marketplaceIntruder.firestore(), "user_inventory", "marketplace_user", "items", "item_1")));
  });

  await test("M11: Client CANNOT write user inventory", async () => {
    await assertFails(
      setDoc(doc(marketplaceUser.firestore(), "user_inventory", "marketplace_user", "items", "item_1"), { quantity: 1 })
    );
  });

  // --- Marketplace list query-limit caps (cost-amplification guard) ---
  // Authorization (who) and query limit (how much) are orthogonal controls:
  // every `allow list` must cap request.query.limit so an authed client cannot
  // read a whole collection in one query. catalog/assets cap = 50, owner-scoped
  // entitlements/inventory cap = 100. Mirrors T49-T52 / T71-T78 for other lists.

  await test("M12: marketplace_catalog list with limit <= 50 succeeds", async () => {
    await assertSucceeds(getDocs(query(
      collection(marketplaceUser.firestore(), "marketplace_catalog"), limit(50)
    )));
  });

  await test("M13: marketplace_catalog list WITHOUT limit must FAIL", async () => {
    await assertFails(getDocs(collection(marketplaceUser.firestore(), "marketplace_catalog")));
  });

  await test("M14: marketplace_catalog list with limit > 50 must FAIL", async () => {
    await assertFails(getDocs(query(
      collection(marketplaceUser.firestore(), "marketplace_catalog"), limit(51)
    )));
  });

  await test("M15: marketplace_assets list with limit <= 50 succeeds", async () => {
    await assertSucceeds(getDocs(query(
      collection(marketplaceUser.firestore(), "marketplace_assets"), limit(50)
    )));
  });

  await test("M16: marketplace_assets list with limit > 50 must FAIL", async () => {
    await assertFails(getDocs(query(
      collection(marketplaceUser.firestore(), "marketplace_assets"), limit(51)
    )));
  });

  await test("M17: Owner entitlements list with limit <= 100 succeeds", async () => {
    await assertSucceeds(getDocs(query(
      collection(marketplaceUser.firestore(), "user_entitlements", "marketplace_user", "items"), limit(100)
    )));
  });

  await test("M18: entitlements list WITHOUT limit must FAIL", async () => {
    await assertFails(getDocs(
      collection(marketplaceUser.firestore(), "user_entitlements", "marketplace_user", "items")
    ));
  });

  await test("M19: entitlements list with limit > 100 must FAIL", async () => {
    await assertFails(getDocs(query(
      collection(marketplaceUser.firestore(), "user_entitlements", "marketplace_user", "items"), limit(101)
    )));
  });

  await test("M20: Non-owner CANNOT list another user's entitlements (even with limit)", async () => {
    await assertFails(getDocs(query(
      collection(marketplaceIntruder.firestore(), "user_entitlements", "marketplace_user", "items"), limit(50)
    )));
  });

  await test("M21: Owner inventory list with limit <= 100 succeeds", async () => {
    await assertSucceeds(getDocs(query(
      collection(marketplaceUser.firestore(), "user_inventory", "marketplace_user", "items"), limit(100)
    )));
  });

  await test("M22: inventory list WITHOUT limit must FAIL", async () => {
    await assertFails(getDocs(
      collection(marketplaceUser.firestore(), "user_inventory", "marketplace_user", "items")
    ));
  });

  await test("M23: Non-owner CANNOT list another user's inventory (even with limit)", async () => {
    await assertFails(getDocs(query(
      collection(marketplaceIntruder.firestore(), "user_inventory", "marketplace_user", "items"), limit(50)
    )));
  });

  // ================================================================
  console.log("\n\n===== RESULTS =====\n");
  console.log(`  Passed: ${passed}`);
  console.log(`  Failed: ${failed}`);
  console.log(`  Total:  ${passed + failed}\n`);

  if (failed > 0) {
    process.exitCode = 1;
  }

} finally {
  await testEnv.cleanup();
}
