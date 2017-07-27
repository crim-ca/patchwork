mkdir -p bin
sbt "-Dspark.version=2.1.0" clean package && mv target/scala-2.11/patchwork_2.11-1.1.jar bin/patchwork_1.1.jar