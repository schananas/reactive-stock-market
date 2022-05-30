package io.bux.matchingengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@SpringBootApplication
public class MatchingEngineApplication {

    @Bean
    RestTemplate restTemplate(ProtobufHttpMessageConverter hmc) {
        return new RestTemplate(List.of(hmc));
    }

    @Bean
    ProtobufHttpMessageConverter protobufHttpMessageConverter() {
        return new ProtobufHttpMessageConverter();
    }

    public static void main(String[] args) {
        SpringApplication.run(MatchingEngineApplication.class, args);
    }

}
