name := "patchwork"

version := "1.1"

scalaVersion := "2.11.7"

val sparkVersion = System.getProperty("spark.version", "2.1.0")

libraryDependencies ++= Seq(
  "org.apache.spark"  %% "spark-core"              % sparkVersion,
  "org.apache.spark"  %% "spark-mllib"              % sparkVersion,
  "org.apache.spark"  %% "spark-sql"               % sparkVersion,
  "org.apache.spark"  %% "spark-mllib"             % sparkVersion,
  "org.apache.commons" % "commons-math3" % "3.5",
  "com.github.scopt" %% "scopt" % "3.2.0")

scalariformSettings

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"

resolvers += Resolver.sonatypeRepo("public")

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))