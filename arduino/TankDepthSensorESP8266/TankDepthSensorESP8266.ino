#include "secrets.h"
#include <ESP8266WiFi.h>
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"

//#define MQTT_SERVER      "192.168.1.196"
#define MQTT_SERVER      "192.168.1.83"
#define MQTT_SERVERPORT  1883                   // use 8883 for SSL

int trig = 4; // Attach Trig of ultrasonic sensor to pin D2
int echo = 5; // Attach Echo of ultrasonic sensor to pin D1

WiFiClient client;
Adafruit_MQTT_Client mqtt(&client, MQTT_SERVER, MQTT_SERVERPORT); // TODO: username and password
Adafruit_MQTT_Publish publishTopic = Adafruit_MQTT_Publish(&mqtt, "humidifierTankDepthSensor");

void setup() {
  Serial.begin(9600);

  delay(1000);

  // Connect to WiFi access point.
  Serial.println(); Serial.println();
  Serial.print("Connecting to ");
  Serial.println(SECRET_SSID);

  WiFi.begin(SECRET_SSID, SECRET_PASS);
  while (WiFi.status() != WL_CONNECTED) {
    delay(2000);
    Serial.print(".");
  }
  Serial.println();

  Serial.println("WiFi connected");
  Serial.println("IP address: "); Serial.println(WiFi.localIP());
  WiFi.setAutoReconnect(true);
  WiFi.persistent(true);

}

void loop() { 
  MQTTConnect();
  float distanceCm = distanceCentimetersAvg();
  Serial.println(distanceCm);
  publishTopic.publish(distanceCm);
  delay(30000); // TODO: 2 minute delay
}

float distanceCentimetersAvg() {
  float sum = 0.0;
  for (int i = 0; i < 10; i++) {
    sum += distanceCentimeters();
    delay(100); 
  }
  return sum / 10.0;
}

float distanceCentimeters() {
  // The PING is triggered by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  pinMode(trig, OUTPUT);
  digitalWrite(trig, LOW);
  delayMicroseconds(2);
  digitalWrite(trig, HIGH);
  delayMicroseconds(5);
  digitalWrite(trig, LOW);
  
  // echo pin is used to read the signal from the PING: a HIGH
  // pulse whose duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(echo, INPUT);
  long duration = pulseIn(echo, HIGH);
  return microsecondsToCentimeters(duration);
}

float microsecondsToCentimeters(long microseconds) {
  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29.0 / 2.0;
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
