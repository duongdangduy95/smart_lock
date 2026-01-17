#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <WebServer.h>
#include <Preferences.h>
#include <PubSubClient.h>
#include <time.h>
#include <ESP32Servo.h>
#include <ArduinoJson.h>
#include <Keypad.h>
#include <MFRC522.h>
#include <SPI.h>
#include <LiquidCrystal_I2C.h>

/* ================= SERVO ================= */
Servo doorServo;

#define SERVO_PIN 17        
#define SERVO_LOCK_POS  10
#define SERVO_OPEN_POS  90

/* ================= WIFI + MQTT ================= */
#define RESET_WIFI_PIN 0
const char* apSSID = "ESP32-Config";
const char* apPassword = "12345678";

const char* mqttServer = "1c96de2709a740a68e5b6d81365811b2.s1.eu.hivemq.cloud";
const int mqttPort = 8883;
const char* mqttUser = "duyduong";
const char* mqttPassword = "Duy10012004/";

const char* rootCACertificate = \ 
"-----BEGIN CERTIFICATE-----\n" \
"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\n" \
"-----END CERTIFICATE-----\n";


/* ================= GLOBAL ================= */
Preferences preferences;
WebServer server(80);
WiFiClientSecure secureClient;
PubSubClient mqttClient(secureClient);

String ssid, wifiPass;
bool wifiConnected = false;
unsigned long lastReconnectAttempt = 0;
bool mqttReady() {
  return wifiConnected && mqttClient.connected();
}

/* ================= LCD ================= */
LiquidCrystal_I2C lcd(0x27, 16, 2);

/* ================= RELAY ================= */
#define RELAY_PIN 16
void openDoor() {
  digitalWrite(RELAY_PIN, HIGH);
  doorServo.write(SERVO_OPEN_POS);
  delay(3000);
  doorServo.write(SERVO_LOCK_POS);
  digitalWrite(RELAY_PIN, LOW);
}

/* ================= MQTT ================= */
void sendMQTTLog(String message) {
  if (mqttReady()) {
    mqttClient.publish("lock/log", message.c_str(), true);
  } else {
    saveOfflineLog(message);
  }
}
void flushOfflineLogs() {
  preferences.begin("mqtt_queue", false);

  int count = preferences.getInt("count", 0);
  if (count == 0) {
    preferences.end();
    return;
  }

  Serial.printf("Flushing %d offline MQTT logs\n", count);

  for (int i = 0; i < count; i++) {
    String key = "msg_" + String(i);
    String payload = preferences.getString(key.c_str(), "");

    if (payload.length()) {
      mqttClient.publish("lock/log", payload.c_str(), true);
      delay(200); // tránh flood broker
    }

    preferences.remove(key.c_str());
  }

  preferences.putInt("count", 0);
  preferences.end();
}


void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String msg;
  for (int i = 0; i < length; i++) msg += (char)payload[i];

  if (String(topic) != "lock/cmd") return;

  DynamicJsonDocument doc(512);
  DeserializationError err = deserializeJson(doc, msg);
  if (err) {
    Serial.println("MQTT JSON parse error");
    return;
  }

  String command = doc["command"] | "";

  /* ===== UNLOCK ===== */
  if (command == "UNLOCK") {
    String method = doc["method"] | "APP";
    openDoor();
    sendMQTTLog("{\"event\":\"UNLOCK\",\"method\":\"" + method + "\"}");
    lcd.clear(); lcd.print("Door Open (APP)");
    delay(1500);
    lcd.clear(); lcd.print("Enter Password:");
  }

  /* ===== DELETE RFID ===== */
  else if (command == "DELETE_RFID") {
    String uidToDelete = doc["uid"] | "";

    if (uidToDelete.length() == 0) {
      Serial.println("UID empty!");
      return;
    }

    preferences.begin("rfid", false);
    bool existed = preferences.remove(uidToDelete.c_str());
    preferences.end();

    if (existed) {
      Serial.println("RFID deleted: " + uidToDelete);
      lcd.clear(); lcd.print("RFID Deleted");
      sendMQTTLog("{\"event\":\"DELETE_RFID\",\"uid\":\"" + uidToDelete + "\"}");
    } else {
      Serial.println("RFID not found: " + uidToDelete);
      lcd.clear(); lcd.print("RFID Not Found");
    }

    delay(1500);
    lcd.clear(); lcd.print("Enter Password:");
  }
}

void saveOfflineLog(String payload) {
  preferences.begin("mqtt_queue", false);

  int count = preferences.getInt("count", 0);
  if (count >= 50) { // giới hạn để bảo vệ flash
    preferences.end();
    return;
  }

  String key = "msg_" + String(count);
  preferences.putString(key.c_str(), payload);
  preferences.putInt("count", count + 1);

  preferences.end();

  Serial.println("Saved offline MQTT log");
}

void reconnectMQTT() {
  if (millis() - lastReconnectAttempt < 5000) return;
  lastReconnectAttempt = millis();

  if (mqttClient.connect("ESP32_LOCK", mqttUser, mqttPassword)) {
  mqttClient.publish("lock/status","ONLINE", true);
  mqttClient.setCallback(mqttCallback);
  mqttClient.subscribe("lock/cmd",1);

  flushOfflineLogs();
}

}

/* ================= RFID ================= */
#define SS_PIN  5
#define RST_PIN 27
MFRC522 rfid(SS_PIN, RST_PIN);

bool isRFIDAllowed(String uid) {
  preferences.begin("rfid", true);
  bool ok = preferences.getBool(uid.c_str(), false);
  preferences.end();
  return ok;
}

void saveRFID(String uid) {
  preferences.begin("rfid", false);
  preferences.putBool(uid.c_str(), true);
  preferences.end();
  sendMQTTLog("{\"event\":\"ADD_RFID\",\"uid\":\"" + uid + "\"}");
  StaticJsonDocument<200> doc;
  doc["event"] = "RFID_SCAN";
  doc["uid"] = uid;
  String out;
  serializeJson(doc, out);
  mqttClient.publish("lock/log", out.c_str());
}

/* ================= KEYPAD ================= */
char keys[4][4] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}
};
byte rowPins[4] = {15,14,13,12};
byte colPins[4] = {26,25,33,32};
Keypad keypad = Keypad(makeKeymap(keys), rowPins, colPins, 4, 4);

/* ================= SYSTEM ================= */
String systemPassword = "1234";
String adminCode = "9999";
String inputBuffer = "";

enum Mode {ENTER_PASSWORD, ENTER_ADMIN, MENU, CHANGE_PASSWORD, ADD_RFID};
Mode currentMode = ENTER_PASSWORD;

/* ================= SETUP ================= */
void setup() {
  Serial.begin(115200);
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);

  lcd.init(); lcd.backlight(); lcd.clear();
  lcd.print("Enter Password:");

  doorServo.attach(SERVO_PIN);
  doorServo.write(SERVO_LOCK_POS);

  SPI.begin(); rfid.PCD_Init();

  preferences.begin("config", true);
  ssid = preferences.getString("ssid","");
  wifiPass = preferences.getString("password","");
  preferences.end();

  if(ssid.length()) {
    WiFi.begin(ssid.c_str(), wifiPass.c_str());
    unsigned long t=millis();
    while(WiFi.status()!=WL_CONNECTED && millis()-t<10000) delay(500);
    if(WiFi.status()==WL_CONNECTED) wifiConnected=true;
  }

  if(wifiConnected) {
    secureClient.setCACert(rootCACertificate);
    mqttClient.setServer(mqttServer,mqttPort);
    configTime(0,0,"pool.ntp.org");
  }
}

/* ================= LOOP ================= */
void loop() {
  if(wifiConnected) {
    reconnectMQTT();
    mqttClient.loop();
  }

  // RFID luôn hoạt động
  if(rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
    String uid="";
    for(byte i=0;i<rfid.uid.size;i++) uid += String(rfid.uid.uidByte[i], HEX);

    if(currentMode==ADD_RFID) {
      saveRFID(uid);
      lcd.clear(); lcd.print("RFID Added");
      delay(1500);
      currentMode=ENTER_PASSWORD;
      lcd.clear(); lcd.print("Enter Password:");
    }
    else if(isRFIDAllowed(uid)) {
      sendMQTTLog("{\"event\":\"UNLOCK\",\"method\":\"RFID\",\"uid\":\""+uid+"\"}");
      lcd.clear(); lcd.print("Door Open");
      openDoor();
      lcd.clear(); lcd.print("Enter Password:");
    } else {
      sendMQTTLog("{\"event\":\"DENIED\",\"method\":\"RFID\",\"uid\":\""+uid+"\"}");
      lcd.clear(); lcd.print("Access Denied");
      delay(1000);
      lcd.clear(); lcd.print("Enter Password:");
    }
    rfid.PICC_HaltA();
  }

  // Keypad
  char key = keypad.getKey();
  if(!key) return;

  if(key=='*') {
    inputBuffer=""; lcd.setCursor(0,1); lcd.print("                "); lcd.setCursor(0,1); 
    return;
  }

  if(key=='#') {
    if(currentMode==ENTER_PASSWORD) {
      if(inputBuffer==systemPassword) {
        sendMQTTLog("{\"event\":\"UNLOCK\",\"method\":\"PASSWORD\"}");
        lcd.clear(); lcd.print("Door Open");
        openDoor();
        lcd.clear(); lcd.print("Enter Admin:");
        currentMode=ENTER_ADMIN;
      } else {
        sendMQTTLog("{\"event\":\"DENIED\",\"method\":\"PASSWORD\"}");
        lcd.clear(); lcd.print("Wrong Password");
        delay(1200);
        lcd.clear(); lcd.print("Enter Password:");
      }
      inputBuffer="";
      return;
    }

    if(currentMode==ENTER_ADMIN) {
      if(inputBuffer==adminCode) {
        currentMode=MENU;
        lcd.clear(); lcd.print("1.Pass 2.RFID");
      } else {
        lcd.clear(); lcd.print("Wrong Admin");
        delay(1200);
        currentMode=ENTER_PASSWORD;
        lcd.clear(); lcd.print("Enter Password:");
      }
      inputBuffer="";
      return;
    }

    if(currentMode==CHANGE_PASSWORD) {
      systemPassword=inputBuffer;
      lcd.clear(); lcd.print("Pass Changed");
      delay(1200);
      currentMode=ENTER_PASSWORD;
      lcd.clear(); lcd.print("Enter Password:");
      inputBuffer="";
      return;
    }
  }

  // Menu lựa chọn
  if(currentMode==MENU) {
    if(key=='1') { currentMode=CHANGE_PASSWORD; lcd.clear(); lcd.print("New Password:"); inputBuffer=""; }
    else if(key=='2') { currentMode=ADD_RFID; lcd.clear(); lcd.print("Scan RFID..."); }
    return;
  }

  // Nhập password / admin / change pass
  if(currentMode==ENTER_PASSWORD || currentMode==ENTER_ADMIN || currentMode==CHANGE_PASSWORD) {
    inputBuffer+=key;
    lcd.setCursor(0,1); lcd.print(inputBuffer);
  }
}
