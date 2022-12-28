#include <Servo.h>

Servo servo;
int minPulseWidth = 500;
int maxPulseWidth = 2500;
int numSecondsToWater = 3;
int numSecondsToReturn = 6;

void setup() {
  pinMode(0, OUTPUT); // D3
  

  // turn on water, delay to allow time for water to flow
  digitalWrite(0, HIGH);
  // delay(500);

  servo.attach(2, minPulseWidth, maxPulseWidth); // D4
}

void loop() {
  int pulseWidthDiff = maxPulseWidth - minPulseWidth;

  int degreeToIncrement = 2;
  int delayMilliPerDegreeWater = numSecondsToWater * 1000 / 360.0;

  // SLOW START ROTATION START
  for (float angle = 0; angle < 12; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * degreeToIncrement * 4);
  }

  for (float angle = 12; angle < 24; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * degreeToIncrement * 3);
  }

  for (float angle = 24; angle < 36; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * degreeToIncrement * 2);
  }
  // SLOW START ROTATION END

  // angle starts from end of slow rotation
  for (float angle = 36; angle < 360; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * degreeToIncrement);
  }
  
  servo.writeMicroseconds(maxPulseWidth);
  
  // turn water off
  digitalWrite(0, LOW);
  
  delay(1000);

  int delayMilliPerDegreeReturn = numSecondsToReturn * 1000 / 360.0;
  for (float angle = 360; angle > 0; angle -= 1) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeReturn);
  }

  servo.writeMicroseconds(minPulseWidth);

  // pause until power cuts out
  delay(1000 * 60 * 60);
}
