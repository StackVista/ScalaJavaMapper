import sbt.Keys._
import scalariform.formatter.preferences._

val scalaSettings = Seq(
  scalaVersion := "2.11.7"
)

val projectSettings = Seq(
  name := "scala-java-mapper",
  organization := "com.stackstate",
  version := "0.0.2-SNAPSHOT"
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

