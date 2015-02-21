# scalajs-games

Library for graphics/audio/inputs for Scala.js

## How to launch the demo

Change the address of the server in the file ```demo/shared/src/main/scala/games/demo/Data.scala```
(It may be a good idea to avoid starting the server and client in the same directory)

### Server

Run ```sbt "demoServer/run"``` (press enter to quit)

### Clients

#### Standard JVM

Run ```sbt "demoJVM/run"```

#### Scala.js

Run ```sbt "demoJS/fastOptJS"``` and open the file ```demoJS-launcher/index-fastopt.html``` with a modern browser

#### Android

Run ```sbt "demoAndroid/android:run"``` after having installed the SDK with the appropriate packages and connected your device
