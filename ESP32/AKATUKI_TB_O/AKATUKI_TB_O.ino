#include <Wire.h>
#include <ICM20948_WE.h>
#include <driver/adc.h>
#include <EEPROM.h>
#include "BLEDevice.h"
#include <BLEUtils.h>
#include <BLEServer.h>

#define SDP810_ADDR 0x25
#define SDA_PIN 21  // SDA ピン
#define SCL_PIN 22  // SCL ピン
#define e_pin 25
#define r_pin 26
#define switch1 32
#define switch2 33
#define SERVICE_UUID        "一つ目のUUID" // サービスUUID
#define CHARACTERISTIC_UUID "二つ目のUUID" // キャラクタリスティックUUID

//舵角
volatile bool switch1Flag = false;
volatile bool switch2Flag = false;

void IRAM_ATTR handleSwitch1() {
    switch1Flag = true;
}

void IRAM_ATTR handleSwitch2() {
    switch2Flag = true;
}
int e_basis;
int r_basis;

//機速
const float AIR_DENSITY = 1.225;  // 空気密度 (kg/m³)

//bluetooth
BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;

// サーバーコールバッククラス
class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    Serial.println("Device connected");
  }

  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    Serial.println("Device disconnected");
    BLEDevice::startAdvertising(); // 切断時に再度アドバタイズを開始
  }
};

void setup() {
    Serial.begin(115200);

    //bluetooth
    BLEDevice::init("ESP32_Peripheral");
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());
    
    BLEService *pService = pServer->createService(SERVICE_UUID);
    
    pCharacteristic = pService->createCharacteristic(
                        CHARACTERISTIC_UUID,
                        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);

    pCharacteristic->setValue("Initial Data");
    pService->start();

    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->start();

    //機速
    Wire.begin(SDA_PIN, SCL_PIN);

    //舵角
    pinMode(switch1, INPUT_PULLUP);
    pinMode(switch2, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(switch1), handleSwitch1, FALLING);
    attachInterrupt(digitalPinToInterrupt(switch2), handleSwitch2, FALLING);

    EEPROM.begin(16);
    EEPROM.get(0, e_basis);
    EEPROM.get(4, r_basis);
}

void loop() {
  if (deviceConnected) {
  //機速
    Wire.beginTransmission(SDP810_ADDR);
    Wire.write(0x36);
    Wire.write(0x15);
    Wire.endTransmission();

    //舵角
    int e_sum = 0;
    int r_sum = 0;
    int e_maxdata = 0;
    int e_mindata = 4096;
    int r_maxdata = 0;
    int r_mindata = 4096;

    for (int i = 0; i < 10; i++) { // 4回測定して平均を取る
    if(e_maxdata < analogRead(e_pin)) e_maxdata = analogRead(e_pin);
    if(e_mindata > analogRead(e_pin)) e_mindata = analogRead(e_pin);
    if(r_maxdata < analogRead(r_pin)) r_maxdata = analogRead(r_pin);
    if(r_mindata > analogRead(r_pin)) r_mindata = analogRead(r_pin);
        e_sum += analogRead(e_pin);
        r_sum += analogRead(r_pin);
        delay(10); // 100ms周期を10回測定する形に
    }
    e_sum -= e_maxdata + e_mindata;
    r_sum -= r_maxdata + r_mindata;

    int e_data = e_sum / 8;//maxとminを除外した8回
    int r_data = r_sum / 8;//maxとminを除外した8回

    int e_value = map(e_data, e_basis - 1082, e_basis + 1070, -90, 90);
    int r_value = map(r_data, r_basis - 1082, r_basis + 1070, -90, 90);

    //機速
    Wire.requestFrom(SDP810_ADDR, 9);

    int16_t rawPressure = (Wire.read() << 8) | Wire.read();
    Wire.read();
    float pressurePa = rawPressure / 60.0;
    if (pressurePa < 0) {
        pressurePa = 0;
    }
    float speed = sqrt(2 * pressurePa / AIR_DENSITY);

    // Bluetooth
    String sensorValueStr = String(e_value) + "e" + String(r_value) + "r" + String(speed) + "s";
    pCharacteristic->setValue(sensorValueStr.c_str());
    pCharacteristic->notify(); // 値が更新されたことを通知
    Serial.print("e:");
    Serial.print(e_value);
    Serial.print("r:");
    Serial.print(r_value);
    Serial.print("s:");
    Serial.println(speed);

    if (switch1Flag) {
        switch1Flag = false;
        e_basis = e_data;
        EEPROM.put(0, e_basis);
        EEPROM.put(4, r_basis);
        EEPROM.commit();
    }

    if (switch2Flag) {
        switch2Flag = false;
        r_basis = r_data;
        EEPROM.put(0, e_basis);
        EEPROM.put(4, r_basis);
        EEPROM.commit();
    }
  }
}
