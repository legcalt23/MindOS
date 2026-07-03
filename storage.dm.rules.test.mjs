import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { deleteObject, getBytes, ref, uploadBytes } from "firebase/storage";

const projectId = "mindos-storage-metadata-rules";
const bytes = new Uint8Array([1, 2, 3]);
const oversizedThumbnail = new Uint8Array(1_048_577);
const EXPECTED_DYNAMIC_ASSERTIONS = 44;

function emulatorRulesWithAppCheckSubstitute() {
  const productionRules = readFileSync("storage.rules", "utf8");
  const productionGuard = "return request.app != null;";
  const occurrences = productionRules.split(productionGuard).length - 1;
  assert.equal(
    occurrences,
    1,
    "Production rules must contain exactly one canonical App Check guard before substitution.",
  );
  return productionRules.replace(
    productionGuard,
    "return true; // TEST-ONLY: Storage emulator does not expose request.app",
  );
}

const testEnv = await initializeTestEnvironment({
  projectId,
  storage: {
    rules: emulatorRulesWithAppCheckSubstitute(),
    host: "127.0.0.1",
    port: Number(process.env.FIREBASE_STORAGE_EMULATOR_PORT ?? 9199),
  },
});

let assertionCount = 0;

async function succeeds(operation) {
  assertionCount += 1;
  await assertSucceeds(operation);
}

async function fails(operation) {
  assertionCount += 1;
  await assertFails(operation);
}

function privateMetadata(customMetadata = {}) {
  return {
    contentType: "image/jpeg",
    customMetadata: { visibility: "private", ...customMetadata },
  };
}

function dmMetadata(chatId, contentType, ownerUid = "alice") {
  return {
    contentType,
    customMetadata: {
      ownerUid,
      visibility: "private",
      chatId,
      participantA: "alice",
      participantB: "bob",
    },
  };
}

function storageFor(uid) {
  return testEnv.authenticatedContext(uid).storage();
}

function objectRef(uid, path) {
  return ref(storageFor(uid), path);
}

async function readObject(storage, path) {
  return getBytes(ref(storage, path), 1024);
}

try {
  await testEnv.clearStorage();

  const profilePath = "profile_visuals/alice/main/1700000000000_profile.jpg";
  await succeeds(uploadBytes(objectRef("alice", profilePath), bytes, privateMetadata({ ownerUid: "alice" })));
  await succeeds(readObject(storageFor("alice"), profilePath));
  await fails(readObject(testEnv.unauthenticatedContext().storage(), profilePath));
  await fails(readObject(storageFor("bob"), profilePath));

  const missingMetadataPath = "profile_visuals/alice/main/1700000000001_legacy.jpg";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await uploadBytes(ref(context.storage(), missingMetadataPath), bytes, { contentType: "image/jpeg" });
  });
  await fails(readObject(storageFor("alice"), missingMetadataPath));
  await fails(
    uploadBytes(
      objectRef("alice", "profile_visuals/bob/main/1700000000002_spoof.jpg"),
      bytes,
      privateMetadata({ ownerUid: "alice" }),
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "profile_visuals/bob/main/1700000000003_spoof.jpg"),
      bytes,
      privateMetadata({ ownerUid: "bob" }),
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "profile_visuals/alice/main/1700000000004_public.jpg"),
      bytes,
      { contentType: "image/jpeg", customMetadata: { ownerUid: "alice", visibility: "public" } },
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "profile_visuals/alice/unsupported/1700000000005_profile.jpg"),
      bytes,
      privateMetadata({ ownerUid: "alice" }),
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "profile_visuals/alice/main/1700000000006_thumb.jpg"),
      oversizedThumbnail,
      privateMetadata({ ownerUid: "alice" }),
    ),
  );

  const postPath = "post_attachments/alice/post_123/attachment.jpg";
  const postMetadata = privateMetadata({ ownerUid: "alice", postId: "post_123" });
  await succeeds(uploadBytes(objectRef("alice", postPath), bytes, postMetadata));
  await fails(
    uploadBytes(
      objectRef("bob", "post_attachments/alice/post_123/attacker.jpg"),
      bytes,
      privateMetadata({ ownerUid: "bob", postId: "post_123" }),
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "post_attachments/alice/post_123/missing_post.jpg"),
      bytes,
      privateMetadata({ ownerUid: "alice" }),
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "post_attachments/alice/post_123/mismatch.jpg"),
      bytes,
      { ...postMetadata, contentType: "image/png" },
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "post_attachments/alice/post_123/payload.exe"),
      bytes,
      { ...postMetadata, contentType: "application/x-msdownload" },
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "post_attachments/alice/post_123/public.jpg"),
      bytes,
      {
        contentType: "image/jpeg",
        customMetadata: { ownerUid: "alice", postId: "post_123", visibility: "public" },
      },
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", "post_attachments/alice/post_123/empty.jpg"),
      new Uint8Array(),
      postMetadata,
    ),
  );
  await fails(uploadBytes(objectRef("alice", postPath), bytes, postMetadata));
  await fails(deleteObject(objectRef("bob", postPath)));
  await succeeds(deleteObject(objectRef("alice", postPath)));

  const chatId = "dm1_5_alice_3_bob";
  const mediaPath = `dm_media/${chatId}/msg_001.jpg`;
  await succeeds(uploadBytes(objectRef("alice", mediaPath), bytes, dmMetadata(chatId, "image/jpeg")));
  await succeeds(readObject(storageFor("alice"), mediaPath));
  await succeeds(readObject(storageFor("bob"), mediaPath));
  await fails(readObject(storageFor("carol"), mediaPath));
  await fails(uploadBytes(objectRef("alice", mediaPath), bytes, dmMetadata(chatId, "image/jpeg")));
  await fails(
    uploadBytes(
      objectRef("alice", `dm_media/${chatId}/mismatch.jpg`),
      bytes,
      dmMetadata(chatId, "image/png"),
    ),
  );
  await fails(
    uploadBytes(
      objectRef("alice", `dm_media/${chatId}/payload.exe`),
      bytes,
      dmMetadata(chatId, "application/x-msdownload"),
    ),
  );

  // ---- profile card media: PRIVATE owner-only / PUBLIC authenticated read ----
  const cardMeta = (contentType = "image/jpeg") => ({ contentType });
  const cardPrivatePath = "profile_cards_private/alice/media/draftcard.jpg";
  const cardPublicPath = "profile_cards_public/alice/media/publiccard.jpg";

  // private: owner read/write only
  await succeeds(uploadBytes(objectRef("alice", cardPrivatePath), bytes, cardMeta()));
  await succeeds(readObject(storageFor("alice"), cardPrivatePath));
  await fails(readObject(storageFor("bob"), cardPrivatePath));
  await fails(readObject(testEnv.unauthenticatedContext().storage(), cardPrivatePath));

  // public: owner writes, any authenticated user reads
  await succeeds(uploadBytes(objectRef("alice", cardPublicPath), bytes, cardMeta()));
  await succeeds(readObject(storageFor("alice"), cardPublicPath));
  await succeeds(readObject(storageFor("bob"), cardPublicPath));
  await fails(readObject(testEnv.unauthenticatedContext().storage(), cardPublicPath));

  // spoof / validation
  await fails(uploadBytes(objectRef("bob", "profile_cards_private/alice/media/spoof.jpg"), bytes, cardMeta()));
  await fails(uploadBytes(objectRef("bob", "profile_cards_public/alice/media/spoof.jpg"), bytes, cardMeta()));
  await fails(uploadBytes(objectRef("alice", "profile_cards_public/alice/media/badtype.jpg"), bytes, cardMeta("application/pdf")));
  await fails(uploadBytes(objectRef("alice", "profile_cards_public/alice/media/ab.jpg"), bytes, cardMeta()));
  await fails(deleteObject(objectRef("bob", cardPublicPath)));
  await succeeds(deleteObject(objectRef("alice", cardPublicPath)));

  // ---- Marketplace assets: auth read / unauth no read / client no write ----
  const marketplaceAssetPath = "marketplace/assets/asset_1/original.webp";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await uploadBytes(ref(context.storage(), marketplaceAssetPath), bytes, { contentType: "image/webp" });
  });
  await succeeds(readObject(storageFor("alice"), marketplaceAssetPath));
  await fails(readObject(testEnv.unauthenticatedContext().storage(), marketplaceAssetPath));
  await fails(uploadBytes(objectRef("alice", marketplaceAssetPath), bytes, { contentType: "image/webp" }));

  assert.equal(assertionCount, EXPECTED_DYNAMIC_ASSERTIONS);
  console.log(`Storage rules dynamic tests passed (${assertionCount} assertions).`);
} finally {
  await testEnv.cleanup();
}
