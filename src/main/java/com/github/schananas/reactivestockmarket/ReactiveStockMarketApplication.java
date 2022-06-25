package com.github.schananas.reactivestockmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@SpringBootApplication
public class ReactiveStockMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactiveStockMarketApplication.class, args);
    }

}
