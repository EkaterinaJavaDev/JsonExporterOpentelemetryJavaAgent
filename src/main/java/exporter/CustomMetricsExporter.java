package exporter;

import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collection;

public class CustomMetricsExporter implements MetricExporter {
    private static final String DESTINATION_URL = System.getenv("METRICS_DESTINATION_URL") != null
            ? System.getenv("METRICS_DESTINATION_URL")
            : System.getProperty("metrics.destination.url", "http://localhost:24226");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AggregationTemporalitySelector temporalitySelector = AggregationTemporalitySelector.deltaPreferred();

    static {
        System.out.println("CustomMetricsExporter: Sending metrics to " + DESTINATION_URL);
    }
    
    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        CompletableResultCode result = new CompletableResultCode();
        try {
            ArrayNode metricsArray = objectMapper.createArrayNode();
            
            for (MetricData metric : metrics) {
                ObjectNode metricJson = objectMapper.createObjectNode();
                metricJson.put("name", metric.getName());
                metricJson.put("description", metric.getDescription());
                metricJson.put("unit", metric.getUnit());
                metricJson.put("type", metric.getType().name());
                
                ArrayNode dataPoints = objectMapper.createArrayNode();
                
                for (PointData point : metric.getData().getPoints()) {
                    ObjectNode pointJson = objectMapper.createObjectNode();
                    pointJson.put("startTimeUnixNano", point.getStartEpochNanos());
                    pointJson.put("timeUnixNano", point.getEpochNanos());
                    
                    if (point instanceof LongPointData) {
                        pointJson.put("value", ((LongPointData) point).getValue());
                    } 
                    else if (point instanceof DoublePointData) {
                        pointJson.put("value", ((DoublePointData) point).getValue());
                    }
                    else if (point instanceof HistogramPointData) {
                        HistogramPointData histogramPoint = (HistogramPointData) point;
                        ObjectNode histogramJson = objectMapper.createObjectNode();
                        histogramJson.put("count", histogramPoint.getCount());
                        histogramJson.put("sum", histogramPoint.getSum());
                        
                        ArrayNode boundariesArray = objectMapper.createArrayNode();
                        histogramPoint.getBoundaries().forEach(boundariesArray::add);
                        histogramJson.set("boundaries", boundariesArray);
                        
                        ArrayNode countsArray = objectMapper.createArrayNode();
                        histogramPoint.getCounts().forEach(countsArray::add);
                        histogramJson.set("counts", countsArray);
                        
                        pointJson.set("histogram", histogramJson);
                    }
                    else if (point instanceof SummaryPointData) {
                        SummaryPointData summaryPoint = (SummaryPointData) point;
                        ObjectNode summaryJson = objectMapper.createObjectNode();
                        summaryJson.put("count", summaryPoint.getCount());
                        summaryJson.put("sum", summaryPoint.getSum());
                        
                        ArrayNode quantileArray = objectMapper.createArrayNode();
                        summaryPoint.getValues().forEach(quantile -> {
                            ObjectNode quantileJson = objectMapper.createObjectNode();
                            quantileJson.put("quantile", quantile.getQuantile());
                            quantileJson.put("value", quantile.getValue());
                            quantileArray.add(quantileJson);
                        });
                        summaryJson.set("quantiles", quantileArray);
                        
                        pointJson.set("summary", summaryJson);
                    }
                    
                    ObjectNode attributes = objectMapper.createObjectNode();
                    point.getAttributes().forEach((key, value) -> 
                        attributes.put(key.getKey(), value.toString())
                    );
                    pointJson.set("attributes", attributes);
                    
                    dataPoints.add(pointJson);
                }
                
                metricJson.set("dataPoints", dataPoints);
                metricsArray.add(metricJson);
            }

            String json = objectMapper.writeValueAsString(metricsArray);

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
                System.err.println("CustomMetricsExporter: Failed to send metrics to Fluent Bit. Response code: " 
                    + responseCode);
                result.fail();
            }

            connection.disconnect();
        } catch (Exception e) {
            System.err.println("CustomMetricsExporter: Exception occurred while exporting metrics");
            e.printStackTrace();
            result.fail();
        }
        return result;
    }

    @Override
    public CompletableResultCode flush() {
        System.out.println("CustomMetricsExporter: flush method called");
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        System.out.println("CustomMetricsExporter: shutdown method called");
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return temporalitySelector.getAggregationTemporality(instrumentType);
    }
}