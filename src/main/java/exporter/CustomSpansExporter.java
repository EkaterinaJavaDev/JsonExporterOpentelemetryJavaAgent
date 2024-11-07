package exporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;

public class CustomSpansExporter implements SpanExporter {

    private static final String DESTINATION_URL = System.getenv("SPANS_DESTINATION_URL") != null
            ? System.getenv("SPANS_DESTINATION_URL")
            : System.getProperty("spans.destination.url", "http://localhost:24224");
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        System.out.println("CustomSpansExporter: Sending spans to " + DESTINATION_URL);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        CompletableResultCode result = new CompletableResultCode();

        try {
            for (SpanData span : spans) {

                String json = objectMapper.writeValueAsString(span);
                
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
                    System.err.println("CustomJsonExporter: Failed to send span " + span.getSpanId() + " to Fluent Bit. Response code: " + responseCode);
                    result.fail();
                }

                connection.disconnect();
            }
        } catch (Exception e) {
            System.err.println("CustomJsonExporter: Exception occurred while exporting spans");
            e.printStackTrace();
            result.fail();
        }
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        System.out.println("CustomJsonExporter: flush method called");
        return CompletableResultCode.ofSuccess(); 
    }

    @Override
    public CompletableResultCode shutdown() {
        System.out.println("CustomJsonExporter: shutdown method called");
        return CompletableResultCode.ofSuccess(); 
    }
}
