package exporter;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class CustomAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addTracerProviderCustomizer((tracerProviderBuilder, config) -> {
            tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(new CustomSpansExporter()));
            return tracerProviderBuilder;
        });
        
        autoConfiguration.addMeterProviderCustomizer((meterProviderBuilder, config) -> {
            MetricExporter metricExporter = new CustomMetricsExporter();
            
            long intervalMinutes = Long.parseLong(System.getProperty("metric.interval.minutes", "30"));

            PeriodicMetricReader reader = PeriodicMetricReader.builder(metricExporter)
                .setInterval(intervalMinutes, TimeUnit.MINUTES)
                .build();

            meterProviderBuilder.registerMetricReader(reader);

            return meterProviderBuilder;
        });
        
        autoConfiguration.addLoggerProviderCustomizer((logEmitterProviderBuilder, config) -> {
        	LogRecordExporter logExporter = new CustomLogsExporter();
            logEmitterProviderBuilder.addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter));
            return logEmitterProviderBuilder;
        });
    }

    @Override
    public int order() {
        return 1000;
    }
}
