name := "let-plugin"
organization := "io.github.jeremyrsmith"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.11", "2.12.4")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

// thanks, kind-projector!
scalacOptions in Test ++= {
  val jar = (packageBin in Compile).value
  Seq(s"-Xplugin:${jar.getAbsolutePath}", s"-Jdummy=${jar.lastModified}") // ensures recompile
}