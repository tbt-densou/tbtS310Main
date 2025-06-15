#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEClient.h>
#include <BluetoothSerial.h>
#include <LiquidCrystal_I2C.h>

#include <Wire.h>
#include <ICM20948_WE.h>
#include "esp_gap_bt_api.h"

#define ICM20948_ADDR 0x68
#define BUTTON_PIN 18  // スイッチ1姿勢各初期化用
#define HeightAddress 0x70
#define RangeCommand 0x51
#define LcdAddress 0x27
#define SERVICE_UUID      "12345678-1234-1234-1234-123456789abc"  // サービスUUID
#define CHARACTERISTIC_UUID "abcdefab-cdef-abcd-efab-cdef12345678" // キャラクタリスティックUUID
#define PERIPHERAL_MAC "AC:15:18:E9:72:92"

#define LED_PIN 13//スマホ接続確認用(未使用)

BluetoothSerial SerialBT;
BLEClient *pClient = nullptr;
BLERemoteCharacteristic *pRemoteCharacteristic = nullptr;
bool connected = false;
bool reconnecting = false;
int reconnectAttempts = 0;

// グローバル変数として以前の値を保持
float previous_s_value = -999.0; // ありえない初期値
int previous_e_value = -999;
int previous_r_value = -999;

#define MAX_RECONNECT_ATTEMPTS 5  // 最大再接続試行回数

//LCD
LiquidCrystal_I2C lcd(0x27, 16, 2);
int first = 1;

// スレーブから通知を受け取るコールバック関数
void notifyCallback(BLERemoteCharacteristic* pBLERemoteCharacteristic, uint8_t* pData, size_t length, bool isNotify) {
  static unsigned long lastSendTime = 0;
  unsigned long currentMillis = millis();
  if(first){
    delay(500);
    lcd.backlight();
    delay(500);
    first = 0;
  }
  String receivedData = String((char*)pData);
  Serial.println("Received: " + receivedData);

  int e_index = receivedData.indexOf('e');
  int r_index = receivedData.indexOf('r');
  int s_index = receivedData.indexOf('s');

  if (e_index != -1 && r_index != -1 && s_index != -1) {
    int e_value = receivedData.substring(0, e_index).toInt();
    int r_value = receivedData.substring(e_index + 1, r_index).toInt();
    float s_value = receivedData.substring(r_index + 1, s_index).toFloat();

    // 値が変更された場合のみLCDを更新
    if (s_value != previous_s_value || e_value != previous_e_value || r_value != previous_r_value) {
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("E:");
      lcd.print(e_value < 0 ? "-" : "+");
      lcd.print(abs(e_value), 1);
      lcd.print(" P:");
      lcd.print(r_value < 0 ? "-" : "+");
      lcd.print(abs(r_value), 1);
      lcd.print(" deg");
      lcd.setCursor(0, 1);
      lcd.print("Speed:");
      lcd.print(s_value, 2);
      lcd.print(" m/s");

      previous_s_value = s_value;
      previous_e_value = e_value;
      previous_r_value = r_value;
    }

    if (SerialBT.connected()) {
      if (currentMillis - lastSendTime >= 100) {
        String dataToSend = "A:" + String(s_value) + ",B:" + String(e_value) + ",C:" + String(r_value) + "\n";
        Serial.println(dataToSend);
        //A:機速,B:E角度,C:R角度,H:高度,I:rpm,O:roll,P:pitch,Q:yaw,V:sw1,W:sw2,X:sw3
        SerialBT.println(dataToSend);
        lastSendTime = currentMillis;
      }
        digitalWrite(LED_PIN, HIGH);
      } else {
        digitalWrite(LED_PIN, LOW);
        delay(10);
      }
  }
    

}

void connectToPeripheral() {
    Serial.println("Attempting to connect to peripheral...");

    if (pClient == nullptr) {
        pClient = BLEDevice::createClient();
    }

    if (pClient->connect(BLEAddress(PERIPHERAL_MAC))) {
        Serial.println("Connected to peripheral");
        connected = true;
        reconnecting = false;
        reconnectAttempts = 0;

        // サービスとキャラクタリスティックを取得して通知をリスン
        BLERemoteService *pService = pClient->getService(SERVICE_UUID);
        if (pService) {
            pRemoteCharacteristic = pService->getCharacteristic(CHARACTERISTIC_UUID);
            if (pRemoteCharacteristic) {
                pRemoteCharacteristic->registerForNotify(notifyCallback);
                Serial.println("Listening for notifications...");
            } else {
                Serial.println("Failed to find characteristic for notifications.");
            }
        } else {
            Serial.println("Failed to find service.");
        }
    } else {
        Serial.println("Failed to connect. Retrying...");
        connected = false;
        reconnecting = true;
    }
}

//姿勢角
ICM20948_WE myIMU = ICM20948_WE(ICM20948_ADDR);

float roll = 0.0, pitch = 0.0, yaw = 0.0;
unsigned long lastUpdateTime = 0;
const float alpha = 0.85; // Complementary filter rate（少し小さくして応答を速くする）
bool imuReady = true;  // 初期状態では正常と仮定

//初期位置
float initialRoll = 0.0, initialPitch = 0.0;

// --- 磁気センサキャリブレーション ---
float magMinX =  1000, magMaxX = -1000;
float magMinY =  1000, magMaxY = -1000;
bool calibrateMag = false;  // キャリブレーションフラグ

// --- 平均用バッファ ---
const int avgWindow = 5;
float accXBuf[avgWindow] = {0}, accYBuf[avgWindow] = {0}, accZBuf[avgWindow] = {0};
int avgIndex = 0;

bool waitForIMU(uint8_t address, uint16_t timeoutMs) {
  uint32_t start = millis();
  while (millis() - start < timeoutMs) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0) {
      return true; // 応答あり
    }
    delay(100);
  }
  return false; // タイムアウト
}

void initializeIMU() {
  myIMU.setGyrRange(ICM20948_GYRO_RANGE_250);
  myIMU.setAccRange(ICM20948_ACC_RANGE_4G);
  myIMU.setGyrDLPF(ICM20948_DLPF_6);
  myIMU.setAccDLPF(ICM20948_DLPF_6);
  myIMU.initMagnetometer(); // 磁気センサ初期化
}


void resetIMU() {
  // センサ初期化処理
  myIMU.autoOffsets(); // センサのオフセットを再計算
  Serial.println("IMU Resetting...");
  delay(1000);
  
  // 初期角度を再設定
  myIMU.readSensor();
  xyzFloat accRaw;
  myIMU.getAccRawValues(&accRaw);
  accRaw.x = -accRaw.x;
  accRaw.y = -accRaw.y;
  accRaw.z = -accRaw.z;

  roll = atan2(accRaw.y, accRaw.z) * 180.0 / PI;
  pitch = atan2(-accRaw.x, sqrt(accRaw.y * accRaw.y + accRaw.z * accRaw.z)) * 180.0 / PI;
  yaw = 0.0;

  // 初期補正角度を再保存
  initialRoll = roll;
  initialPitch = pitch;
}


void setup() {
  Wire.begin(21, 22);
  Wire.setClock(100000);
  delay(1000);
  //LCD
  lcd.init();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("LCD leady");
  delay(1000);
  lcd.noBacklight();
  delay(1000);
  Serial.begin(115200);
  SerialBT.begin("BTC Device 01");

  BLEDevice::init("ESP32 Master");
  connectToPeripheral();

  //姿勢角
  pinMode(BUTTON_PIN, INPUT_PULLUP);  // ボタンピンを入力プルアップに設定
  delay(500);

  lastUpdateTime = millis();

  myIMU.autoOffsets();
  myIMU.setGyrRange(ICM20948_GYRO_RANGE_250);
  myIMU.setAccRange(ICM20948_ACC_RANGE_4G);
  myIMU.setGyrDLPF(ICM20948_DLPF_6);
  myIMU.setAccDLPF(ICM20948_DLPF_6);
  myIMU.initMagnetometer(); // 磁気センサ初期化

  // 初期角度設定
  myIMU.readSensor();
  xyzFloat accRaw;
  myIMU.getAccRawValues(&accRaw);
  accRaw.x = -accRaw.x;
  accRaw.y = -accRaw.y;
  accRaw.z = -accRaw.z;

  roll = atan2(accRaw.y, accRaw.z) * 180.0 / PI;
  pitch = atan2(-accRaw.x, sqrt(accRaw.y * accRaw.y + accRaw.z * accRaw.z)) * 180.0 / PI;
  yaw = 0.0;

  // --- 初期補正角度を保存 ---
  initialRoll = roll;
  initialPitch = pitch;
}

void loop() {
  static unsigned long lastSendTime = 0;
  unsigned long currentMillis = millis();

  static float rollV = 0;
  static float pitchV = 0;
  static float yawV = 0;

  // 接続状態をチェック
  if (!connected) {
    first = 1;
      if (reconnecting) {
          if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
              Serial.println("Reconnecting...");
              connectToPeripheral();
              reconnectAttempts++;
              delay(1000);  // 再接続の間に1秒待機
          } else {
              Serial.println("Max reconnect attempts reached, waiting before retry...");
              delay(5000);  // 最大試行回数に達したら5秒待機
              reconnectAttempts = 0;  // リセット
          }
      }
  }

  // スレーブが再接続後に通知を受け取る準備ができたことを確認
  if (connected && pRemoteCharacteristic != nullptr) {
      // 接続が切れた場合のみ再接続を試みる
      if (!pClient->isConnected()) {
          Serial.println("Connection lost. Attempting to reconnect...");
          connected = false;
          reconnecting = true;
          first = 1;
      }
  }
  
  //高度
  Wire.beginTransmission(HeightAddress);
  Wire.write(RangeCommand);
  Wire.endTransmission();
  delay(100); // 測定待機
  Wire.requestFrom(HeightAddress, 2);
  double range = Wire.read() << 8 | Wire.read();
  Serial.print("Height: ");
  Serial.print(range);
  Serial.print("cm ");
  double height = range / 100;

  //姿勢角
  // ボタンが押されたらIMUをリセット
  if (digitalRead(BUTTON_PIN) == LOW) {  // ボタンが押された（LOW）場合
    resetIMU();
    while (digitalRead(BUTTON_PIN) == LOW); // ボタンが離されるまで待機
  }
  
  // IMUが未接続・切断されたときの再接続処理
  if (!imuReady) {
    Wire.beginTransmission(ICM20948_ADDR);
    if (Wire.endTransmission() == 0) {
      Serial.println("ICM20948 reconnected. Reinitializing...");
      if (myIMU.init()) {
        imuReady = true;
        initializeIMU();
        Serial.println("Reinitialization successful.");
      } else {
        Serial.println("Reinitialization failed. Will retry.");
      }
    } else {
      delay(100);
      return;  // 接続が復帰するまで測定は一時停止
    }
  }

  myIMU.readSensor();

  xyzFloat accRaw, gyrRaw, magRaw;
  myIMU.getAccRawValues(&accRaw);
  myIMU.getGyrValues(&gyrRaw);
  myIMU.getMagValues(&magRaw);

  unsigned long currentTime = millis();
  float dt = (currentTime - lastUpdateTime) / 1000.0;
  lastUpdateTime = currentTime;

  // 逆さま補正
  accRaw.x = -accRaw.x;
  accRaw.y = -accRaw.y;
  accRaw.z = -accRaw.z;
  gyrRaw.x = -gyrRaw.x;
  gyrRaw.y = -gyrRaw.y;
  gyrRaw.z = -gyrRaw.z;

  // --- 磁気センサキャリブレーション（回転しながら実行） ---
  if (calibrateMag) {
    magMinX = min(magMinX, magRaw.x);
    magMaxX = max(magMaxX, magRaw.x);
    magMinY = min(magMinY, magRaw.y);
    magMaxY = max(magMaxY, magRaw.y);
  }

  // ハードアイアン補正
  float magOffsetX = (magMaxX + magMinX) / 2.0;
  float magOffsetY = (magMaxY + magMinY) / 2.0;
  float correctedMagX = magRaw.x - magOffsetX;
  float correctedMagY = magRaw.y - magOffsetY;

  // --- 地磁気でYaw計算 ---
  float pitchRad = pitch * PI / 180.0;
  float rollRad = roll * PI / 180.0;

  // チルト補正付き磁気方位角の計算
  float xh = magRaw.x * cos(pitchRad) + magRaw.z * sin(pitchRad);
  float yh = magRaw.x * sin(rollRad) * sin(pitchRad) + magRaw.y * cos(rollRad) - magRaw.z * sin(rollRad) * cos(pitchRad);

  float heading = atan2(yh, xh) * 180.0 / PI;
  if (heading < 0) heading += 360;  // 方角が負の場合は360度を加算

  // --- 地磁気に基づくyawの更新 ---
  yaw = 0.90 * yaw + 0.10 * heading;

  // --- ロール・ピッチ計算 ---
  float accRoll = atan2(accRaw.y, accRaw.z) * 180.0 / PI;
  float accPitch = atan2(-accRaw.x, sqrt(accRaw.y * accRaw.y + accRaw.z * accRaw.z)) * 180.0 / PI;

  roll  = alpha * roll  + (1.0 - alpha) * accRoll;
  pitch = alpha * pitch + (1.0 - alpha) * accPitch;

  // --- 補正後の値を出力 ---
  float correctedRoll = roll - initialRoll;
  float correctedPitch = pitch - initialPitch;

  // --- 出力 ---
  Serial.print("Roll: ");
  Serial.print(correctedRoll);  // 補正後のロール角
  Serial.print("deg ");
  rollV = correctedRoll;

  Serial.print("Pitch: ");
  Serial.print(correctedPitch); // 補正後のピッチ角
  Serial.print("deg ");
  pitchV = correctedPitch;

  Serial.print("Yaw: ");
  Serial.print(yaw); // 地磁気に基づくヨー角
  Serial.println("deg");
  yawV = yaw;

  //A:機速,B:E角度,C:R角度,H:高度,I:rpm,O:roll,P:pitch,Q:yaw,V:sw1,W:sw2,X:sw3
  if (SerialBT.connected()) {
    if (currentMillis - lastSendTime >= 100) { // 100ms ごとにデータ送信
        String dataToSend = "H:" + String(height) + ",O:" + String(rollV) + ",P:" + String(pitchV) + ",Q:" + String(yawV) + "\n";
    SerialBT.print(dataToSend);
    Serial.println(dataToSend);
    lastSendTime = currentMillis;
    }
    digitalWrite(LED_PIN, HIGH);
  } else {
    digitalWrite(LED_PIN, LOW);
    delay(10);
  }

}
