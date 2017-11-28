package com.example.webfluxdemo;

import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.webfluxdemo.model.Tweet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class WebClientTest {
    
    @Test
    public void test() throws InterruptedException {
        
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.setLevel(Level.WARN);
        
        final Runtime runtime = Runtime.getRuntime();
        
        restTemplate();
        
        System.out.printf("Memory in use while reading: %d MB\n",
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        
    }

    private void restTemplate() {
        
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost:8080")
                .build();
        
        Tweet[] tweets = restTemplate.getForObject("/tweets", Tweet[].class);
        System.out.printf("Tweets: %d%n", tweets.length);
        
    }

    private void webClient() {
        
        WebClient webClient = WebClient.create("http://localhost:8080");
        
        Long count = webClient.get()
            .uri("/tweets")
            .exchange()
            .flatMapMany(response -> response.bodyToFlux(Tweet.class))
            .count()
            .block();
        
        System.out.printf("Tweets: %d%n", count);
        
    }

}
