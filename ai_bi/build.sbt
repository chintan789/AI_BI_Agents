name := "ai_bi"
version := "0.1"
scalaVersion := "2.13.12"

lazy val pekkoVersion = "1.0.2"
lazy val pekkoKafkaVersion = "1.0.0"
lazy val pekkoHttpVersion = "1.0.0"

resolvers ++= Seq(
  "Apache Pekko" at "https://repo.maven.apache.org/maven2",
  "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases/",
  Resolver.mavenCentral
)

libraryDependencies ++= Seq(
  // ✅ Core Pekko
  "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,

  // ✅ Pekko Kafka (Alpakka)
  "org.apache.pekko" %% "pekko-connectors-kafka" % pekkoKafkaVersion,

  // ✅ Pekko HTTP (correct artifact)
  "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,

  // ✅ JSON parsing
  "io.spray" %% "spray-json" % "1.3.6"
)
