import sbt.Keys._
import scalariform.formatter.preferences._

val scalaSettings = Seq(
  scalaVersion := "2.11.7"
)

val scalariform = scalariformSettings :+
  (ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(AlignParameters, false)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 90)
  )

lazy val macro = project.in(file("macro"))
  .settings(scalaSettings:_*)
  .settings(libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value)
  .settings(scalariform)

lazy val root = project.in(file(".")).dependsOn(macro)
  .settings(scalaSettings:_*)
  .settings(scalariform)
  .settings(compileOrder := CompileOrder.Mixed)

