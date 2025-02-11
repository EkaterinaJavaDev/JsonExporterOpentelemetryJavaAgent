# Custom Traces, Metrics, and Logs Exporter Documentation

This is a custom exporter for sending data (traces, metrics, and logs) collected by `opentelemetry-javaagent` in JSON format.

## How to Use the Custom Exporter

1. **📥 Download Latest Release**
   
   [![Download CustomTracesMetricsLogsExporter JAR](https://img.shields.io/badge/Download-CustomTracesMetricsLogsExporter.jar-blue?style=for-the-badge&logo=github)](https://github.com/EkaterinaJavaDev/JsonExporterOpentelemetryJavaAgent/releases/latest/download/CustomTracesMetricsLogsExporter.jar)

   [![Download OpenTelemetry Agent(version 2.9.0)](https://img.shields.io/badge/Download-OpenTelemetry--javaagent.jar-green?style=for-the-badge&logo=github)](https://github.com/EkaterinaJavaDev/JsonExporterOpentelemetryJavaAgent/releases/latest/download/opentelemetry-javaagent.jar)

3. **Specify Paths to the Files**

   Save the files anywhere in your project and specify their paths when running the application.

   Use the following JVM arguments to point to the correct locations:
   ```bash
     -javaagent:<path_to>/opentelemetry-javaagent.jar
     -Dotel.javaagent.extensions=<path_to>/CustomTracesMetricsLogsExporter-0.0.1.jar
     -Dmetric.interval.minutes=<interval_in_minutes>  # Optional: Specify metric collection interval in minutes (default is 30)
      ```

   For example, if you place both files in the root of your project, use:
   ```bash
     -javaagent:./opentelemetry-javaagent.jar
     -Dotel.javaagent.extensions=./CustomTracesMetricsLogsExporter-0.0.1.jar
     -Dmetric.interval.minutes=15
      ```

4. **Set JVM Arguments**

   - When running your application, add the following JVM arguments to specify where to send traces, logs, and metrics:

     ```bash
     -Dspans.destination.url=<traces_url>
     -Dlogs.destination.url=<logs_url>
     -Dmetrics.destination.url=<metrics_url>
     ```

   - By default, the exporter sends data to these URLs:

     - **Traces**: `http://localhost:24224`
     - **Logs**: `http://localhost:24225`
     - **Metrics**: `http://localhost:24226`

5. **Disable Default Exporters**

   - To prevent the agent from sending data to its default ports, disable the default exporters:

     ```bash
     -Dotel.traces.exporter=none
     -Dotel.metrics.exporter=none
     -Dotel.logs.exporter=none
     ```

## Example in a Dockerfile

Here is how you can set it up in a Dockerfile:

```dockerfile
FROM maven AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app1
COPY --from=build /app/target/your_application.jar your_application.jar
COPY ./opentelemetry-javaagent.jar .
COPY ./CustomTracesMetricsLogsExporter.jar .

EXPOSE 8080

ENTRYPOINT ["java", \
            "-javaagent:./opentelemetry-javaagent.jar", \
            "-Dotel.javaagent.extensions=./CustomTracesMetricsLogsExporter-0.0.1.jar", \
            "-Dotel.traces.exporter=none", \
            "-Dotel.metrics.exporter=none", \
            "-Dotel.logs.exporter=none", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dspans.destination.url=https://192.168.0.1:24224", \
            "-Dlogs.destination.url=https://192.168.0.1:24225", \
            "-Dmetrics.destination.url=https://192.168.0.1:24226", \
            "-Dmetric.interval.minutes=20", \
            "-jar", "your_application.jar"]
```

Replace your_application.jar with the name of your application.

Make sure the URLs you provide are correct and the servers are ready to receive the data.
