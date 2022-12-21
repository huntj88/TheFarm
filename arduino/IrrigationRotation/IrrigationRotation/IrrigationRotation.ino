#include <Servo.h>

Servo servo;
int minPulseWidth = 500;
int maxPulseWidth = 2500;
int numSecondsToWater = 5;
int numSecondsToReturn = 15;

void setup() {
  servo.attach(2, minPulseWidth, maxPulseWidth); //D4
}

void loop() {
  int pulseWidthDiff = maxPulseWidth - minPulseWidth;

  int degreeToIncrement = 2;
  int delayMilliPerDegreeWater = numSecondsToWater * 1000 / 360.0 / degreeToIncrement;

  for (float angle = 0; angle < 360; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater);
  }

  servo.writeMicroseconds(maxPulseWidth);
  delay(2000);


  int delayMilliPerDegreeReturn = numSecondsToReturn * 1000 / 360.0;
  for (float angle = 360; angle > 0; angle -= 1) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeReturn);
  }

  servo.writeMicroseconds(minPulseWidth);

  // pause until power cuts out
  delay(1000 * 60 * 60);
}
