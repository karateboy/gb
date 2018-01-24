name := """gb"""

version := "1.0.26"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  specs2 % Test,
  "org.scalikejdbc" %% "scalikejdbc"                  % "2.5.2",
  "org.scalikejdbc" %% "scalikejdbc-config"           % "2.5.2",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.5.1",
  "com.github.nscala-time" %% "nscala-time" % "2.16.0",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.2.0",
  "com.google.maps" % "google-maps-services" % "0.2.6",
  "commons-io" % "commons-io" % "2.5",
  "org.apache.poi" % "poi" % "3.17",
  "org.apache.poi" % "poi-ooxml" % "3.17",
  "org.apache.poi" % "poi-scratchpad" % "3.17",
  "org.apache.poi" % "poi-ooxml-schemas" % "3.17",
  "com.itextpdf" % "itextpdf" % "5.5.12",
  "com.itextpdf.tool" % "xmlworker" % "5.5.12"
)

mappings in Universal ++=
(baseDirectory.value / "report_template" * "*" get) map
    (x => x -> ("report_template/" + x.getName))
    
 mappings in Universal ++=
(baseDirectory.value / "import" * "*" get) map
    (x => x -> ("import/" + x.getName))
    

//libraryDependencies += "com.google.guava" % "guava" % "19.0"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//routesGenerator := InjectedRoutesGenerator

scalacOptions ++= Seq("-feature")

fork in run := false