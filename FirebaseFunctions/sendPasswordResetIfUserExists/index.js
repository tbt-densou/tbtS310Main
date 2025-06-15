const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendPasswordResetIfUserExists = functions.https.onCall(async (data, context) => {
  const email = data?.email || data?.data?.email || "";
  console.log("Received data email:", email);

  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "メールアドレスが必要です。");
  }

  try {
    const userRecord = await admin.auth().getUserByEmail(email);
    if (userRecord) {
      // クライアント側で送信するため、リンク生成やメール送信はしない
      return { result: "確認済み" };
    }
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      throw new functions.https.HttpsError("not-found", "登録されていないメールアドレスです。");
    }
    throw new functions.https.HttpsError("internal", error.message);
  }
});

