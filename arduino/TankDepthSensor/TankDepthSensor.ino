int trig = 11; // Attach Trig of ultrasonic sensor to pin 11
int echo = 10; // Attach Echo of ultrasonic sensor to pin 10

void setup() {
  //Initiate Serial communication.
  Serial.begin(9600);
}

void loop() { 
  Serial.print(distanceCentimetersAvg());
  Serial.print("\n");

  delay(5000); // TODO: 2 minute delay
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
  
  // The same pin is used to read the signal from the PING: a HIGH
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
