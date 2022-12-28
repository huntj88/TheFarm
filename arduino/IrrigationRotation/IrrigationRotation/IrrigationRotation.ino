#include <Servo.h>

Servo servo;
int minPulseWidth = 500;
int maxPulseWidth = 2500;
int pulseWidthDiff = maxPulseWidth - minPulseWidth;
int numSecondsToWater = 3;
int delayMilliPerDegreeWater = numSecondsToWater * 1000 / 360.0;

int numSecondsToReturn = 6;
int waterPin = 0; // D3

void setup() {
  // setup and turn on water
  pinMode(waterPin, OUTPUT);
  servo.attach(2, minPulseWidth, maxPulseWidth); // D4
}

void loop() {
  waterAndRotateCounterClockwise();

  delay(1000);

  resetRotation();
  
  // pause until power cuts out
  delay(1000 * 60 * 60);
}

void waterAndRotateCounterClockwise() {
  digitalWrite(waterPin, HIGH);
  
  int degreeToIncrement = 2;

  // SLOW START ROTATION START
  for (float angle = 0; angle < 8; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * degreeToIncrement * 4);
  }

  for (float angle = 8; angle < 16; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * degreeToIncrement * 3);
  }

  for (float angle = 16; angle < 24; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * degreeToIncrement * 2);
  }
  // SLOW START ROTATION END

  // angle starts from end of slow rotation
  for (float angle = 24; angle < 360; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    turnOffWaterIfEnding(angle);
    delay(delayMilliPerDegreeWater * degreeToIncrement);
  }
  
  servo.writeMicroseconds(maxPulseWidth);
}

void turnOffWaterIfEnding(int angle) {
  // turn water pressure off 1 second before end of irrigation period, remaining pressure in pipe is enough to finish
    int turnOffWaterDegrees = 360 - (1000 / delayMilliPerDegreeWater);
    if (angle >= turnOffWaterDegrees) {
      digitalWrite(0, LOW);
    }
}

void resetRotation() {
    int delayMilliPerDegreeReturn = numSecondsToReturn * 1000 / 360.0;
  for (float angle = 360; angle > 0; angle -= 1) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeReturn);
  }

  servo.writeMicroseconds(minPulseWidth);
}
