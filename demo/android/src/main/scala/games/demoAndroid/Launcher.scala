package games.demoAndroid

import android.os.Bundle
import android.app.Activity

import games.demo.Stub

class Launcher extends Activity {
  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    println("Android " + Stub.text)
  }
}
