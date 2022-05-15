#include <Ezo_uart.h>
#include <SoftwareSerial.h>
#include "secrets.h"
#include <ESP8266WiFi.h>
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"

//#define MQTT_SERVER      "192.168.1.196"
#define MQTT_SERVER      "192.168.1.83"
#define MQTT_SERVERPORT  1883


SoftwareSerial mySUART(4, 5);  //D2, D1 = SRX, STX
Ezo_uart sensor(mySUART);
WiFiClient client;
Adafruit_MQTT_Client mqtt(&client, MQTT_SERVER, MQTT_SERVERPORT);
Adafruit_MQTT_Publish publishTopic = Adafruit_MQTT_Publish(&mqtt, "ezoHum");

const uint8_t bufferLength = 32;
char responseData[bufferLength];

void setup() {
  Serial.begin(9600);
  
  // Connect to WiFi access point.
  Serial.println(); Serial.println();
  Serial.print("Connecting to ");
  Serial.println(SECRET_SSID);

  WiFi.begin(SECRET_SSID, SECRET_PASS);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();

  Serial.println("WiFi connected");
  Serial.println("IP address: "); Serial.println(WiFi.localIP());

  mySUART.begin(9600);

  Serial.println("continuous mode 15 seconds");
  sensor.send_cmd("C,15", responseData, bufferLength);
  Serial.println(responseData);
  
  Serial.println("enabling humidity");
  sensor.send_cmd("O,HUM,1", responseData, bufferLength);
  Serial.println(responseData);
  
  Serial.println("enabling temperature");
  sensor.send_cmd("O,T,1", responseData, bufferLength);
  Serial.println(responseData);
  
  Serial.println("disabling dew point");
  sensor.send_cmd("O,DEW,0", responseData, bufferLength);
  Serial.println(responseData);
}

void loop() {
  MQTTConnect();
  Serial.println("reading sensor data");
  sensor.receive_cmd(responseData, bufferLength);
  Serial.println(responseData);
  publishTopic.publish(responseData);
  delay(15000);
}

// invoke in loop function
void MQTTConnect() {
  if (mqtt.connected()) {
    return;
  }

  Serial.print("Connecting to MQTT... ");
  // retry infinitely
  int8_t responseCode;
  while ((responseCode = mqtt.connect()) != 0) { // connect will return 0 for connected
       Serial.println(mqtt.connectErrorString(responseCode));
       Serial.println("Retrying MQTT connection in 30 seconds...");
       mqtt.disconnect();
       delay(30000);
  }
  Serial.println("MQTT Connected!");
}
