#include <Servo.h>

#define ENA 7
#define IN1 6
#define IN2 5
#define ENB 2
#define IN3 4
#define IN4 3

Servo lifterLeft;
Servo lifterRight;

String inputString = "";
bool stringComplete = false;

void setup() {
  Serial.begin(9600);
  Serial1.begin(9600);

  pinMode(ENA, OUTPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(ENB, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);

  lifterLeft.attach(9);
  lifterRight.attach(10);

  stopMotors();
}

void loop() {
  while (Serial1.available()) {
    char inChar = (char)Serial1.read();
    Serial.print(inChar);
    if (inChar == '\n') {
      stringComplete = true;
    } else {
      inputString += inChar;
    }
  }

  if (stringComplete) {
    processCommand(inputString);
    inputString = "";
    stringComplete = false;
  }
}

void processCommand(String command) {
  command.trim();
  Serial1.print("Received: ");
  Serial1.println(command);

  if (command.startsWith("F:")) {
    int speed = command.substring(2).toInt();
    moveForward(speed);
  } else if (command.startsWith("B:")) {
    int speed = command.substring(2).toInt();
    moveBackward(speed);
  } else if (command.startsWith("L:")) {
    int speed = command.substring(2).toInt();
    turnLeft(speed);
  } else if (command.startsWith("R:")) {
    int speed = command.substring(2).toInt();
    turnRight(speed);
  } else if (command.startsWith("S")) {
    stopMotors();
  } else if (command.startsWith("LFT:")) {
    int angle = command.substring(4).toInt();
    setLifterAngle(angle);
  }
}

void moveForward(int speed) {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  analogWrite(ENA, speed);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENB, speed);
}

void moveBackward(int speed) {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  analogWrite(ENA, speed);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  analogWrite(ENB, speed);
}

void turnLeft(int speed) {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  analogWrite(ENA, speed);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  analogWrite(ENB, speed);
}

void turnRight(int speed) {
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  analogWrite(ENA, speed);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  analogWrite(ENB, speed);
}

void stopMotors() {
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  analogWrite(ENA, 0);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
  analogWrite(ENB, 0);
}

void setLifterAngle(int angle) {
  lifterLeft.write(angle);
  delay(200);
  lifterRight.write(180 - angle);
}
