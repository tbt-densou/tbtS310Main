const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.helloWorld = functions.https.onCall(async(data, context) => {
  // Firebase Callable Functions では、data は { data: { ... } } 形式になることに注意
  const code = data?.code || data?.data?.code || "World";
  console.log("Received data name:", code);
  const expectedCode = "shion"; //このコードとの整合性をチェックする

  if (code !== expectedCode) {
    console.warn("Invalid registration code attempt:", code, "from user:", context.auth.uid);
    throw new functions.https.HttpsError(
      'permission-denied', // または 'invalid-argument'
      '認証コードが間違っています。',
      { customMessage: '入力された認証コードが一致しません。' }
    );
  }
// 認証コードが正しい場合、成功を返す
  return { success: true, message: "認証コードが確認されました。" };
});