name := """gb"""

version := "1.0.32"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

libraryDependencies += jdbc
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "org.scalikejdbc" %% "scalikejdbc"                  % "2.5.2"
libraryDependencies += "org.scalikejdbc" %% "scalikejdbc-config"           % "2.5.2"
libraryDependencies += "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.5.1"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.2.0"
libraryDependencies += "com.google.maps" % "google-maps-services" % "0.2.6"
libraryDependencies += "commons-io" % "commons-io" % "2.5"
libraryDependencies += "org.apache.poi" % "poi" % "3.17"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "3.17"
libraryDependencies += "org.apache.poi" % "poi-scratchpad" % "3.17"
libraryDependencies += "org.apache.poi" % "poi-ooxml-schemas" % "3.17"
libraryDependencies += "com.itextpdf" % "itextpdf" % "5.5.12"
libraryDependencies += "com.itextpdf.tool" % "xmlworker" % "5.5.12"


mappings in Universal ++=
(baseDirectory.value / "report_template" * "*" get) map
    (x => x -> ("report_template/" + x.getName))
    
 mappings in Universal ++=
(baseDirectory.value / "import" * "*" get) map
    (x => x -> ("import/" + x.getName))
    

//libraryDependencies += "com.google.guava" % "guava" % "19.0"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

//routesGenerator := InjectedRoutesGenerator
routesGenerator := StaticRoutesGenerator

scalacOptions ++= Seq("-feature")

fork in run := false