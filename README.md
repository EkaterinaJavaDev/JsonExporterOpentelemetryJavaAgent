java -javaagent:./opentelemetry-javaagent.jar \
     -Dotel.javaagent.extensions=./CustomTracesExporter-0.0.1.jar \
     -Dotel.service.name=my-java-service \
     -jar ./target/edutreck_backend-0.0.1-SNAPSHOT.jar
