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

  // SLOW START ROTATION START
  for (float angle = 0; angle < 16; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * 6);
  }

  for (float angle = 16; angle < 32; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * 4);
  }

  for (float angle = 32; angle < 64; angle += degreeToIncrement) {
    servo.writeMicroseconds((angle / 360.0 * pulseWidthDiff) + minPulseWidth);
    delay(delayMilliPerDegreeWater * 2);
  }
  // SLOW START ROTATION END

  // angle starts from end of slow rotation
  for (float angle = 64; angle < 360; angle += degreeToIncrement) {
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
