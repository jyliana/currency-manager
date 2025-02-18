package com.manager.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Component
@AllArgsConstructor
@Slf4j
public class PrivatClient {

  private WebClient client;

  @SneakyThrows
  public String getCurrencyRatesJson(String date) {
    Function<UriBuilder, URI> queryParam = uriBuilder -> uriBuilder
            .queryParam("date", date)
            .build();

    log.info("Getting currencies for the date: {}", date);

    return client.get()
            .uri(queryParam)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response ->
                    Mono.error(new RuntimeException("API not found or client error occurred.")))
            .onStatus(HttpStatusCode::is5xxServerError, response ->
                    Mono.error(new RuntimeException("Server error occurred.")))
            .bodyToMono(String.class)
//            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(Random.from(new Random()).nextInt(10) + 5))
//                    .filter(throwable -> throwable instanceof TimeoutException))
            .doOnError(TimeoutException.class, e -> log.error("Request timed out: {}", date, e))
            .block();
  }

}
