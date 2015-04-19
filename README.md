# scalajs-games

Library for graphics/audio/inputs for Scala.js

## Demo

### Commands

* Press **Escape** to exit
* Press **F** to toggle fullscreen
* Press **L** to toggle pointer lock
* Maintain **W** to accelerate or **S** to brake
* Left mouse button to shoot and mouse movement to navigate

### Launching

#### General

The address the clients will attempt to reach is located in ```demo/shared/src/main/scala/games/demo/Data.scala``` (```ws://localhost:8080/``` by default, which should be fine if you are using the included server locally).

#### Server + Scala.js client

Run ```sbt```. Once in SBT, enter ```serverDemoJS/reStart```. This will start the server and make the Scala.js client available.
To use the Scala.js client, open your browser (preferably Chrome or Firefox) to the specified address (normally [http://localhost:8080/](http://localhost:8080/)) (press Ctrl + C to stop the server and exit SBT).

#### JVM client (requires a running server to connect to)

Run ```sbt "demoJVM/run"```.
