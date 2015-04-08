//import android.Keys._
//import android.Dependencies.aar

lazy val commonSettings = Seq(
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.11.6",
        persistLauncher in Compile := true,
        persistLauncher in Test := true,
        resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        resolvers += "Spray" at "http://repo.spray.io",
        scalacOptions ++= Seq(
          "-deprecation"
        ),
        libraryDependencies ++= Seq(
          "com.lihaoyi" %%% "upickle" % "0.2.8"
        )
    )

lazy val demo = crossProject
    .crossType(CrossType.Full)
    .in(file("demo"))
    
    /* Common settings */
    
    .settings(
        commonSettings: _*
    )
    .settings(
        testFrameworks += new TestFramework("utest.runner.Framework"),
        libraryDependencies ++= Seq(
            "com.github.olivierblanvillain" %%% "transport-core" % "0.1-SNAPSHOT",
            "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
        )
    )
    
    /* JavaScript settings */
    
    .jsSettings(
        name := "demoJS",
        skip in packageJSDependencies := false,
        libraryDependencies ++= Seq(
            "org.scala-js" %%% "scalajs-dom" % "0.8.0",
            "com.github.olivierblanvillain" %%% "transport-javascript" % "0.1-SNAPSHOT"
        )
    )
    
    /* Standard JVM settings */
    
    .jvmSettings(
        LWJGLPlugin.lwjglSettings: _*
    )
    .jvmSettings(
        name := "demoJVM",
        connectInput in run := true,
        unmanagedResourceDirectories in Compile += baseDirectory.value / ".." / "shared" / "src" / "main" / "resources",
        libraryDependencies ++= Seq(
            "com.github.olivierblanvillain" %%% "transport-tyrus" % "0.1-SNAPSHOT",
            "org.jcraft" % "jorbis" % "0.0.17"
        )
    )
    
lazy val demoJVM = demo.jvm
lazy val demoJS = demo.js
/*lazy val demoAndroid = project
    .in(file("demo/android"))
    .settings(
        commonSettings: _*
    )
    .settings(
        android.Plugin.androidBuild: _*
    )
    .settings(
        name := "demoAndroid",
        platformTarget in Android := "android-14",
        proguardScala in Android := true,
        proguardOptions in Android ++= Seq(
            "-ignorewarnings",
            "-keep class org.glassfish.tyrus.**", // Somehow, tyrus doesn't seem to like proguard
            "-keep class scala.Dynamic" // TODO should be removed
        ),
        unmanagedSourceDirectories in Compile += baseDirectory.value / ".." / "shared" / "src" / "main" / "scala",
        unmanagedSourceDirectories in Test += baseDirectory.value / ".." / "shared" / "src" / "test" / "scala",
        unmanagedResourceDirectories in Compile += baseDirectory.value / ".." / "shared" / "src" / "main" / "resources",
        unmanagedResourceDirectories in Test += baseDirectory.value / ".." / "shared" / "src" / "test" / "resources",
        libraryDependencies ++= Seq(
            "com.github.olivierblanvillain" %% "transport-core" % "0.1-SNAPSHOT",
            "com.github.olivierblanvillain" %% "transport-tyrus" % "0.1-SNAPSHOT",
            aar("com.google.android.gms" % "play-services" % "4.0.30"),
            aar("com.android.support" % "support-v4" % "r7")
        )
    )*/

/* Spray server for the demoJS project */

lazy val serverDemoJS = project
    .in(file("demo/server"))
    .settings(
        spray.revolver.RevolverPlugin.Revolver.settings: _*
    )
    .settings(
        commonSettings: _*
    )
    .settings(
        libraryDependencies ++= {
            Seq(
                "com.wandoulabs.akka" %% "spray-websocket" % "0.1.4"
            )
        },
        (resources in Compile) += (fastOptJS in (demoJS, Compile)).value.data
    )
