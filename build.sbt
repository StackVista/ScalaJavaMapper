import sbt.Credentials
import sbt.Keys._
import scalariform.formatter.preferences._
import com.typesafe.sbt.git._
val scalaSettings = Seq(
  scalaVersion := "2.11.7"
  //, scalacOptions += "-Ymacro-debug-lite"
)

val projectSettings = Seq(
  name := "scala-java-mapper",
  organization := "com.stackstate",
  version := {
    import scala.collection.JavaConversions._
    val git = new org.eclipse.jgit.api.Git(new org.eclipse.jgit.storage.file.FileRepositoryBuilder().findGitDir(baseDirectory.value).build)
    git.getRepository.getBranch.toLowerCase + "-" + git.log().call().toList.length + "-" + git.getRepository.resolve("HEAD").abbreviate(7).name()
  },
  publishTo := Some("Artifactory Realm" at "http://54.194.173.64/artifactory/libs"),
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

lazy val root = 
  project.in(file("."))
  .enablePlugins(GitVersioning)
  .settings(projectSettings:_*)
  .settings(scalaSettings:_*)
  .settings(scalariform)
  .settings(dependencies)
  .settings(compileOrder := CompileOrder.Mixed)

