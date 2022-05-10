#include "secrets.h"
#include <ESP8266WiFi.h>
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"

#define MQTT_SERVER      "192.168.1.191"
#define MQTT_SERVERPORT  1883                   // use 8883 for SSL

// Create an ESP8266 WiFiClient class to connect to the MQTT server.
WiFiClient client;

// Setup the MQTT client class by passing in the WiFi client and MQTT server and login details.
Adafruit_MQTT_Client mqtt(&client, MQTT_SERVER, MQTT_SERVERPORT);
Adafruit_MQTT_Publish publishTopic = Adafruit_MQTT_Publish(&mqtt, "humidifierTankDepthSensor");

int trig = 4; // Attach Trig of ultrasonic sensor to pin D2
int echo = 5; // Attach Echo of ultrasonic sensor to pin D1

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

}

void loop() { 
  MQTTConnect();
  float distanceCm = distanceCentimetersAvg();
  Serial.println(distanceCm);
  publishTopic.publish(distanceCm);
  delay(15000); // TODO: 2 minute delay
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

// Function to connect and reconnect as necessary to the MQTT server.
// Should be called in the loop function and it will take care if connecting.
void MQTTConnect() {
  int8_t ret;

  // Stop if already connected.
  if (mqtt.connected()) {
    return;
  }

  Serial.print("Connecting to MQTT... ");

  while ((ret = mqtt.connect()) != 0) { // connect will return 0 for connected
       Serial.println(mqtt.connectErrorString(ret));
       Serial.println("Retrying MQTT connection in 15 seconds...");
       mqtt.disconnect();
       delay(15000);  // wait 15 seconds
  }
  Serial.println("MQTT Connected!");
}
