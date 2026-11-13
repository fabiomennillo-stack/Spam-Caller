const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfile(uid) {
  const past = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "Mario Rossi",
    email: "mario@example.com",
    photoUrl: "https://example.com/avatar.jpg",
    autoBlockEnabled: true,
    spamThreshold: 70,
    blockHiddenNumbers: false,
    lastPhoneBackupLabel: "Nessun backup",
    createdAt: past,
    updatedAt: past,
  };
}

test("Unauthenticated user cannot read or write users collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
});

test("Authenticated user can create, read, and update their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));
  await assertSucceeds(ref.get());
  await assertSucceeds(
    ref.update({
      spamThreshold: 85,
      autoBlockEnabled: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated user cannot read or write another user's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      ...validProfile(BOB_UID),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      spamThreshold: 10,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Shadow update with ghost field or invalid spamThreshold is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertFails(
    ref.set({
      ...validProfile(ALICE_UID),
      isAdmin: true,
    })
  );
  await assertFails(
    ref.set({
      ...validProfile(ALICE_UID),
      spamThreshold: 5,
    })
  );
});

test("User cannot change immutable userId or createdAt on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(ref.set(validProfile(ALICE_UID)));

  await assertFails(
    ref.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
