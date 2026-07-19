package com.ecommerce.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;
import vn.payos.core.ClientOptions;

import java.util.Locale;

@Configuration
public class PayosSdkConfig {

    @Bean(destroyMethod = "close")
    public PayOS payOS(PayosProperties properties) {
        ClientOptions options = ClientOptions.builder()
                .clientId(properties.getClientId())
                .apiKey(properties.getApiKey())
                .checksumKey(properties.getChecksumKey())
                .logLevel(ClientOptions.LogLevel.valueOf(properties.getLogLevel().toUpperCase(Locale.ROOT)))
                .build();
        return new PayOS(options);
    }
}
