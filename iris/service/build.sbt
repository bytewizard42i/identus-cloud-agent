import Dependencies._
import sbt.Keys.testFrameworks
import sbtghpackages.GitHubPackagesPlugin.autoImport._

// Custom keys
val apiBaseDirectory = settingKey[File]("The base directory for Iris API specifications")
ThisBuild / apiBaseDirectory := baseDirectory.value / "../api"

def commonProject(project: Project): Project =
  project.settings(
    version := "0.1.0-SNAPSHOT",
    organization := "io.iohk.atala",
    scalaVersion := "3.1.3",
    githubTokenSource := TokenSource.Environment("ATALA_GITHUB_TOKEN"),
    resolvers += Resolver
      .githubPackages("input-output-hk", "atala-prism-sdk"),
    // Needed for Kotlin coroutines that support new memory management mode
    resolvers +=
      "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
  )

// Project definitions
lazy val root = commonProject(project)
  .in(file("."))
  .aggregate(core, sql, server)

lazy val core = commonProject(project)
  .in(file("core"))
  .settings(
    name := "iris-core",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    libraryDependencies ++= coreDependencies,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(apiBaseDirectory.value / "grpc")
  )

lazy val sql = commonProject(project)
  .in(file("sql"))
  .settings(
    name := "iris-sql",
    libraryDependencies ++= sqlDependencies
  )
  .dependsOn(core)

lazy val server = commonProject(project)
  .in(file("server"))
  .settings(
    name := "iris-server",
    libraryDependencies ++= serverDependencies,
  )
  .dependsOn(core, sql)