package com.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivatClient {

  private final WebClient client;

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
            .retryWhen(Retry.max(3)
                    .filter(e -> e instanceof TimeoutException)
                    .doBeforeRetry(retrySignal -> log.warn("Retrying...")))
            .doOnError(TimeoutException.class, e -> log.error("Request timed out: {}", date, e))
            .block();
  }

}
