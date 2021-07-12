organization in ThisBuild := "org.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

lagomCassandraEnabled in ThisBuild := false
lagomUnmanagedServices in ThisBuild := Map("cas_native" -> "tcp://localhost:9042")

//
//lagomKafkaEnabled in ThisBuild := false
//lagomKafkaAddress in ThisBuild := "localhost:9092"
//lagomKafkaPropertiesFile in ThisBuild := Some((baseDirectory in ThisBuild).value / "project" / "kafka-server.properties")

val AkkaVersion = "2.6.14"
val AkkaManagementVersion = "1.1.0"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test

lazy val `hello` = (project in file("."))
  .aggregate(`hello-api`, `hello-impl`, `hello-stream-api`, `hello-stream-impl`)

lazy val `hello-api` = (project in file("hello-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `hello-impl` = (project in file("hello-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      lagomScaladslAkkaDiscovery,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings)
  .dependsOn(`hello-api`)

lazy val `hello-stream-api` = (project in file("hello-stream-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )
  .dependsOn(`hello-api`)

lazy val `hello-stream-impl` = (project in file("hello-stream-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslTestKit,
      lagomScaladslAkkaDiscovery,
      lagomScaladslCluster,
      lagomScaladslPubSub,
      lagomScaladslKafkaBroker,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
      macwire,
      scalaTest
    )
  )
  .dependsOn(`hello-stream-api`, `hello-api`)

addCommandAlias(s"runMicrocervice1", ";lagomServiceLocatorStart;lagomCassandraStart;lagomKafkaStart;runAll")
