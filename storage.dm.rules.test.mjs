import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { getBytes, ref, uploadBytes } from "firebase/storage";

const projectId = "mindos-storage-metadata-rules";
const bytes = new Uint8Array([1, 2, 3]);

const testEnv = await initializeTestEnvironment({
  projectId,
  storage: {
    rules: readFileSync("storage.rules", "utf8"),
    host: "127.0.0.1",
    port: Number(process.env.FIREBASE_STORAGE_EMULATOR_PORT ?? 9199),
  },
});

function jpegMetadata(customMetadata = {}) {
  return {
    contentType: "image/jpeg",
    customMetadata,
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

async function readObject(storage, path) {
  return getBytes(ref(storage, path), 1024);
}

try {
  await testEnv.clearStorage();

  const publicProfilePath = "profile_visuals/alice/main/1700000000000_public.jpg";
  await assertSucceeds(
    uploadBytes(
      ref(storageFor("alice"), publicProfilePath),
      bytes,
      jpegMetadata({ ownerUid: "alice", visibility: "public" }),
    ),
  );
  await assertSucceeds(readObject(testEnv.unauthenticatedContext().storage(), publicProfilePath));

  const privateProfilePath = "profile_visuals/alice/main/1700000000001_private.jpg";
  await assertSucceeds(
    uploadBytes(
      ref(storageFor("alice"), privateProfilePath),
      bytes,
      jpegMetadata({ ownerUid: "alice", visibility: "private" }),
    ),
  );
  await assertSucceeds(readObject(storageFor("alice"), privateProfilePath));
  await assertFails(readObject(storageFor("bob"), privateProfilePath));

  const missingMetadataPath = "profile_visuals/alice/main/1700000000002_legacy.jpg";
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await uploadBytes(ref(context.storage(), missingMetadataPath), bytes, { contentType: "image/jpeg" });
  });
  await assertFails(readObject(storageFor("alice"), missingMetadataPath));

  await assertFails(
    uploadBytes(
      ref(storageFor("alice"), "profile_visuals/bob/main/1700000000003_spoof.jpg"),
      bytes,
      jpegMetadata({ ownerUid: "alice", visibility: "public" }),
    ),
  );
  await assertFails(
    uploadBytes(
      ref(storageFor("alice"), "profile_visuals/bob/main/1700000000004_spoof.jpg"),
      bytes,
      jpegMetadata({ ownerUid: "bob", visibility: "public" }),
    ),
  );

  const publicPostPath = "post_attachments/alice/post_123/attachment.jpg";
  await assertSucceeds(
    uploadBytes(
      ref(storageFor("alice"), publicPostPath),
      bytes,
      jpegMetadata({ ownerUid: "alice", postId: "post_123", visibility: "public" }),
    ),
  );
  await assertSucceeds(readObject(testEnv.unauthenticatedContext().storage(), publicPostPath));

  const privatePostPath = "post_attachments/alice/post_123/private.jpg";
  await assertSucceeds(
    uploadBytes(
      ref(storageFor("alice"), privatePostPath),
      bytes,
      jpegMetadata({ ownerUid: "alice", postId: "post_123", visibility: "private" }),
    ),
  );
  await assertSucceeds(readObject(storageFor("alice"), privatePostPath));
  await assertFails(readObject(storageFor("bob"), privatePostPath));

  const chatId = "dm1_5_alice_3_bob";
  const mediaPath = `dm_media/${chatId}/msg_001.jpg`;
  await assertSucceeds(uploadBytes(ref(storageFor("alice"), mediaPath), bytes, dmMetadata(chatId, "image/jpeg")));
  await assertSucceeds(readObject(storageFor("alice"), mediaPath));
  await assertSucceeds(readObject(storageFor("bob"), mediaPath));
  await assertFails(readObject(storageFor("carol"), mediaPath));

  await assertSucceeds(
    uploadBytes(ref(storageFor("alice"), `dm_media/${chatId}/msg_003.png`), bytes, dmMetadata(chatId, "image/png")),
  );
  await assertSucceeds(
    uploadBytes(ref(storageFor("alice"), `dm_media/${chatId}/msg_004.webp`), bytes, dmMetadata(chatId, "image/webp")),
  );

  await assertFails(
    uploadBytes(ref(storageFor("alice"), mediaPath), bytes, dmMetadata(chatId, "image/jpeg")),
  );
  await assertFails(
    uploadBytes(ref(storageFor("alice"), `dm_media/${chatId}/msg_005.bin`), bytes, dmMetadata(chatId, "application/octet-stream")),
  );
  await assertFails(
    uploadBytes(ref(storageFor("alice"), `dm_media/${chatId}/msg_006.jpg`), bytes, dmMetadata(chatId, "application/octet-stream")),
  );
  await assertFails(
    uploadBytes(ref(storageFor("alice"), `dm_media/${chatId}/msg_007.jpg`), bytes, dmMetadata(chatId, "image/png")),
  );

  for (const [extension, contentType] of [
    ["apk", "application/vnd.android.package-archive"],
    ["exe", "application/x-msdownload"],
    ["zip", "application/zip"],
    ["js", "application/javascript"],
    ["html", "text/html"],
    ["json", "application/json"],
    ["txt", "text/plain"],
  ]) {
    await assertFails(
      uploadBytes(
        ref(storageFor("alice"), `dm_media/${chatId}/blocked_${extension}.${extension}`),
        bytes,
        dmMetadata(chatId, contentType),
      ),
    );
  }

  await assertFails(
    uploadBytes(
      ref(storageFor("carol"), `dm_media/${chatId}/msg_002.jpg`),
      bytes,
      dmMetadata(chatId, "image/jpeg", "carol"),
    ),
  );

  console.log("Storage metadata hardening rules tests passed");
} finally {
  await testEnv.cleanup();
}
