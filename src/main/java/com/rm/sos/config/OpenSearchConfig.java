package com.rm.sos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host}")
    private String host;

    @Value("${opensearch.port}")
    private int port;

    @Bean
    public RestClient restClient() {
        return RestClient
                .builder(new HttpHost(host, port, "http"))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper(){
        // Build ObjectMapper with Java 8 time support
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public OpenSearchClient openSearchClient(RestClient restClient, ObjectMapper mapper) {
        RestClientTransport transport = new RestClientTransport( restClient,new JacksonJsonpMapper(mapper));
        return new OpenSearchClient(transport);
    }
}