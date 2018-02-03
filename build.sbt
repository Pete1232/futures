val root = (project in file("."))
  .settings(
    name := "scala-notes",
    scalaVersion := "2.12.4"
  )
  .settings(
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4"
  )
