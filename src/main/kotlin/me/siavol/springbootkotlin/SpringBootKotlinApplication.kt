package me.siavol.springbootkotlin

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@SpringBootApplication
@EnableConfigurationProperties(BlogProperties::class)
class SpringBootKotlinApplication

fun main(args: Array<String>) {
    runApplication<SpringBootKotlinApplication>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}

@Configuration(proxyBeanMethods = false)
class MyConfiguration {
    @Bean
    fun otlpHttpSpanExporter(@Value("\${tracing.url}") url: String?): OtlpGrpcSpanExporter {
        return OtlpGrpcSpanExporter.builder().setEndpoint(url!!).build()
    }
}
