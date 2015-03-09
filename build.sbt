import android.Keys._
import android.Dependencies.aar

lazy val commonSettings = Seq(
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.11.5",
        persistLauncher in Compile := true,
        persistLauncher in Test := true,
        resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        scalacOptions ++= Seq(
          "-deprecation"
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
lazy val demoAndroid = project
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
    )

/* Spray server for the demoJS project */

lazy val serverDemoJS = project
    .in(file("serverDemo"))
    .settings(
        spray.revolver.RevolverPlugin.Revolver.settings: _*
    )
    .settings(
        commonSettings: _*
    )
    .settings(
        libraryDependencies ++= {
            val akkaV = "2.3.6"
            val sprayV = "1.3.2"
            Seq(
                "io.spray" %% "spray-can" % sprayV,
                "io.spray" %% "spray-routing" % sprayV,
                "io.spray" %% "spray-testkit" % sprayV % "test",
                "com.typesafe.akka" %% "akka-actor" % akkaV,
                "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
                "org.specs2" %% "specs2-core" % "2.3.11" % "test"
            )
        },
        (resources in Compile) += (fastOptJS in (demoJS, Compile)).value.data,
        (resources in Compile) += (fullOptJS in (demoJS, Compile)).value.data
    )

/* Server project */

lazy val demoServer = project
    .in(file("demo/server"))
    .settings(
        commonSettings: _*
    )
    .settings(
        libraryDependencies ++= Seq(
            "com.github.olivierblanvillain" %% "transport-netty" % "0.1-SNAPSHOT"
        )
    )
