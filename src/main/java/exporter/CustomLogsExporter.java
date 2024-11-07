package exporter;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class CustomLogsExporter implements LogRecordExporter {

    private static final String DESTINATION_URL = System.getenv("LOGS_DESTINATION_URL") != null
            ? System.getenv("LOGS_DESTINATION_URL")
            : System.getProperty("logs.destination.url", "http://localhost:24225");
    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        System.out.println("CustomLogsExporter: Sending logs to " + DESTINATION_URL);
    }
    
    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        CompletableResultCode result = new CompletableResultCode();
        try {
            ArrayNode resourceLogsArray = objectMapper.createArrayNode();
            ObjectNode resourceLog = objectMapper.createObjectNode();
            ArrayNode scopeLogsArray = objectMapper.createArrayNode();
            ObjectNode scopeLog = objectMapper.createObjectNode();
            ArrayNode logRecordsArray = objectMapper.createArrayNode();


            for (LogRecordData log : logs) {
                ObjectNode logJson = objectMapper.createObjectNode();
                logJson.put("timeUnixNano", log.getTimestampEpochNanos());
                logJson.put("severityNumber", log.getSeverity().getSeverityNumber());
                logJson.put("level", log.getSeverityText());
                logJson.putObject("body").put("stringValue", log.getBody().asString());
                logJson.put("traceId", log.getSpanContext().getTraceId());
                logJson.put("spanId", log.getSpanContext().getSpanId());

                ArrayNode attributesArray = objectMapper.createArrayNode();
                log.getAttributes().forEach((key, value) -> {
                    ObjectNode attributeJson = objectMapper.createObjectNode();
                    attributeJson.put("key", key.getKey());
                    ObjectNode valueJson = objectMapper.createObjectNode();
                    valueJson.put("stringValue", value.toString());
                    attributeJson.set("value", valueJson);
                    attributesArray.add(attributeJson);
                });
                logJson.set("attributes", attributesArray);

                logRecordsArray.add(logJson);
            }
            


            scopeLog.set("logRecords", logRecordsArray);
            scopeLogsArray.add(scopeLog);
            resourceLog.set("scopeLogs", scopeLogsArray);
            resourceLogsArray.add(resourceLog);

            ObjectNode finalJson = objectMapper.createObjectNode();
            finalJson.set("resourceLogs", resourceLogsArray);

            String json = objectMapper.writeValueAsString(finalJson);

            URL url = new URI(DESTINATION_URL).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes());
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                result.succeed();
            } else {
                System.err.println("CustomLogsExporter: Failed to send logs to Fluent Bit. Response code: " 
                    + responseCode);
                result.fail();
            }

            connection.disconnect();
        } catch (Exception e) {
            System.err.println("CustomLogsExporter: Exception occurred while exporting logs");
            e.printStackTrace();
            result.fail();
        }
        return result;
    }
    
    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
