# scalajs-games

Library for graphics/audio/inputs for Scala.js

## How to launch the demo

### Server

Run ```sbt "demoServer/run"``` (press enter to quit)

### Clients

#### Standard JVM

Run ```sbt "demoJVM/run"```

#### Scala.js

Run ```sbt "demoJS/fastOptJS"``` and open the file ```demoJS-launcher/index-fastopt.html``` with a modern browser

#### Android

Run ```sbt "demoAndroid/android:run"``` after having installed the SDK with the appropriate packages and connected your device
