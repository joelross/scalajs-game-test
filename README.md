# Scalajs-games

Library for graphics/audio/inputs for Scala.js

## Demo

### Commands

The mouse-keyboard controls are the well-known WASD:
* Maintain **W** to go forward, **S** to go backward, **A** to move left, **D** to move right (or the arrows)
* Left mouse button to shoot and mouse movement to change the orientation
* Press **Escape** to exit
* Press **F** to toggle fullscreen
* Press **L** to toggle pointer lock
* Press **Tab** to alternate between Qwerty and Azerty key-mapping.
* Press **numpad +** to increase the audio volume
* Press **numpad -** to decrease the audio volume

The touchscreen controls are:
* Left part of the image to move
* Right part of the image to change the orientation
* Tap the image to shoot
* Tap the top-left corner of the image to toggle fullscreen
* Tap the top-right corner of the image to invert the part to move and the part for orientation (may be more comfortable if you are left handed)

Players are dispatched in room of up to 8 players.

### Launching

#### General

The address the clients will attempt to reach is located in the file ```demo/shared/src/main/resources/games/demo/config```. The responsible line is ```server=ws://localhost:8080/``` by default, which should be fine if you are starting both the server and the client locally on the same machine, but if you are planning to connect to the server from other machines, you should replace ```localhost``` by something more reachable (your IP or your domain name if you have one).

#### Server + Scala.js client

* Run ```sbt```. Once in SBT, enter ```serverDemoJS/reStart```. This will start the server and make the Scala.js client available through it (press Ctrl + C to stop the server and exit SBT).
* To use the Scala.js client, open your browser (preferably Chrome or Firefox) to the specified address ([http://localhost:8080/](http://localhost:8080/) by default).

#### JVM client (requires a running server to connect to)

Run ```sbt "demoJVM/run"```.

## License

Scalajs-games code itself is under the BSD license

The dependencies for the JVM code are:
* [LWJGL](https://github.com/LWJGL/lwjgl)
* [JOrbis](http://www.jcraft.com/jorbis/)

The dependencies for the Scala.js code are:
* [Aurora.js](https://github.com/audiocogs/aurora.js) (optional)
