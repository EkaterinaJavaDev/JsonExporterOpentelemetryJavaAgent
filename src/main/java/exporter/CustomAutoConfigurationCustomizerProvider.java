package exporter;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

public class CustomAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addTracerProviderCustomizer((tracerProviderBuilder, config) -> {
            // Добавляем кастомный SpanExporter
            tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(new CustomJsonExporter()));
            return tracerProviderBuilder;
        });
    }

    @Override
    public int order() {
        return 1000; // Укажите порядок загрузки, если нужно
    }
}
