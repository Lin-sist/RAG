package com.enterprise.rag.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

/**
 * 代理感知的 WebClient 配置
 * <p>
 * 当 proxy.enabled=true 时，所有通过此配置创建的 WebClient 都会走 HTTP 代理。
 * 用于访问 NVIDIA NIM / OpenAI / 通义千问等需要翻墙或走代理的外部 API。
 * <p>
 * 使用方式：注入 {@code WebClient.Builder} Bean，然后按需添加 baseUrl、header 等配置。
 */
@Slf4j
@Configuration
public class ProxyWebClientConfig {

    @Value("${proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${proxy.port:7890}")
    private int proxyPort;

    /**
     * 提供一个代理感知的 WebClient.Builder Bean。
     * <p>
     * 注意：Spring 默认的 WebClient.Builder 也是 prototype scope，
     * 这里用 @Bean 覆盖，确保所有注入点都拿到带代理的版本。
     */
    @Bean
    public WebClient.Builder proxyWebClientBuilder() {
        WebClient.Builder builder = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024));

        if (proxyEnabled) {
            log.info("HTTP Proxy enabled: {}:{}", proxyHost, proxyPort);
            HttpClient httpClient = HttpClient.create()
                    .proxy(proxy -> proxy
                            .type(ProxyProvider.Proxy.HTTP)
                            .host(proxyHost)
                            .port(proxyPort));
            builder.clientConnector(new ReactorClientHttpConnector(httpClient));
        } else {
            log.info("HTTP Proxy disabled, connecting directly");
        }

        return builder;
    }
}
