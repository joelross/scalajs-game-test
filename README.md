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

Run ```sbt```. Once in SBT, enter ```demoJS/fullOptJS``` then ```serverDemoJS/re-start```, open your browser to the specified address, normally [http://localhost:8080/](http://localhost:8080/) (press Ctrl + C to stop the server and exit SBT)

#### Android (not maintained anymore, on standby)

Run ```sbt "demoAndroid/android:run"``` after having installed the Android SDK and NDK with the appropriate packages and connected your device
