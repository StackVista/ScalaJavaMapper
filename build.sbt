import sbt.Credentials
import sbt.Keys._
import scalariform.formatter.preferences._

val scalaSettings = Seq(
  scalaVersion := "2.11.7"
  //, scalacOptions += "-Ymacro-debug-lite"
)

val projectSettings = Seq(
  name := "scala-java-mapper",
  organization := "com.stackstate",
  version := "0.0.2-SNAPSHOT",
  publishTo := Some("Artifactory Realm" at "http://192.168.2.58:8081/artifactory/libs-snapshot-local"),
  credentials += Credentials(Path.userHome / ".sbt" / "artifactory.credentials")
)

val dependencies = {
  libraryDependencies ++= Seq(
    "org.scala-lang"            % "scala-compiler"           % scalaVersion.value,
    "org.scalatest"            %%  "scalatest"               % "2.2.4"      % "test"
  )
}

val scalariform = scalariformSettings :+
  (ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(AlignParameters, false)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 90)
  )

lazy val root = project.in(file("."))
  .settings(projectSettings:_*)
  .settings(scalaSettings:_*)
  .settings(scalariform)
  .settings(dependencies)
  .settings(compileOrder := CompileOrder.Mixed)

