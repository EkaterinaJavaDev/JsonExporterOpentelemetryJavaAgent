package exporter;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class CustomJsonExporterProvider implements ConfigurableSpanExporterProvider {

    @Override
    public SpanExporter createExporter(ConfigProperties config) {
    	System.out.println("+++++++++++++=================createExporter+++++++++++==============");
        return new CustomJsonExporter(); // Здесь возвращаем ваш кастомный экспортёр
    }

    @Override
    public String getName() {
    	System.out.println("+++++++++++++=================getName+++++++++++==============");
        return "customjson"; // Имя, используемое для идентификации экспортёра
    }
}

