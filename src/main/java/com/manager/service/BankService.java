package com.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager.data.dto.Currency;
import com.manager.data.dto.ExchangeRateDto;
import com.manager.data.dto.FullCurrencyData;
import com.manager.repository.ExchangeRatesRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@AllArgsConstructor
@Slf4j
public class BankService {

  private PrivatClient client;
  private ObjectMapper objectMapper;
  private ExchangeRatesRepository repository;

  public List<ExchangeRateDto> checkArchivedRates(String date) {
    var localDate = convertDateToLocalDate(date);

    return repository.findAllByDate(localDate);
  }

  @SneakyThrows
  public List<Currency> getAllRecordsForDates(String startDate, String endDate, String currency) {
    var localStartDate = convertDateToLocalDate(startDate);
    var localEndDate = convertDateToLocalDate(endDate);

    var days = localStartDate.datesUntil(localEndDate.plusDays(1)).toList();

    var recordsForPeriod = Objects.isNull(currency)
            ? repository.findAllByDateBetweenAndCurrencyIn(localStartDate, localEndDate, List.of("USD", "EUR"))
            : repository.findAllByDateBetweenAndCurrency(localStartDate, localEndDate, currency);

    var missingDays = days.stream().filter(day -> recordsForPeriod.stream()
            .map(ExchangeRateDto::getDate)
            .noneMatch(day::equals)).toList();

    var allRates = getRatesForMissingDays(currency, missingDays);
    allRates.addAll(convertRatesDtoToCurrencyRates(recordsForPeriod));

    return allRates;
  }

  private List<Currency> getRatesForMissingDays(String currency, List<LocalDate> missingDays) throws InterruptedException {
    List<Currency> allRates = new ArrayList<>();

    for (LocalDate date : missingDays) {
      var missingCurrencies = getRequiredCurrenciesFromApi(convertLocalDateToDate(date));

      if (missingCurrencies.isEmpty()) {
        return allRates;
      }

      missingCurrencies.forEach(this::saveCurrencies);

      if (Objects.isNull(currency)) {
        allRates.addAll(missingCurrencies);
      } else {
        allRates.addAll(missingCurrencies.stream().filter(c -> c.currency().equals(currency)).toList());
      }

      if (missingDays.size() > 2) {
        var duration = Duration.ofSeconds(8);
        log.info("Waiting for {} seconds", duration.getSeconds());
        Thread.sleep(duration);
      }
    }
    return allRates;
  }

  public List<Currency> getAllRecordsForPeriod(String period, String currency) {
    var endDate = LocalDate.now();
    LocalDate startDate;

    switch (period) {
      case "week" -> startDate = endDate.minusWeeks(1);
      case "month" -> startDate = endDate.minusMonths(1);
      case "quarter" -> startDate = endDate.minusMonths(3);
      case "half-year" -> startDate = endDate.minusMonths(6);
      default -> startDate = LocalDate.now().minusDays(1);
    }

    return getAllRecordsForDates(convertLocalDateToDate(startDate), convertLocalDateToDate(endDate), currency);
  }


  private List<Currency> convertRatesDtoToCurrencyRates(List<ExchangeRateDto> list) {
    return list.stream()
            .map(c -> new Currency(c.getDate(), c.getCurrency(), c.getSaleRate(), c.getPurchaseRate()))
            .toList();
  }

  public List<Currency> getExchangeRates(String date) {
    var archivedRates = checkArchivedRates(date);

    if (!archivedRates.isEmpty()) {
      return convertRatesDtoToCurrencyRates(archivedRates);
    }

    var requiredCurrencies = getRequiredCurrenciesFromApi(date);
    requiredCurrencies.forEach(this::saveCurrencies);

    return requiredCurrencies;
  }

  private void saveCurrencies(Currency c) {
    repository.save(ExchangeRateDto.builder()
            .date(c.date())
            .currency(c.currency())
            .saleRate(c.saleRate())
            .purchaseRate(c.purchaseRate())
            .build());
  }

  private List<Currency> getRequiredCurrenciesFromApi(String date) {
    String json = client.getCurrencyRatesJson(date);
    log.info("Processing data for the date: {}", date);
    var currencyData = extractCurrencyValueFromJSON(json);

    return getFilteredCurrencies(currencyData);
  }

  private List<Currency> getFilteredCurrencies(FullCurrencyData fullCurrencyData) {
    return fullCurrencyData.getExchangeRate().stream()
            .filter(c -> Set.of("USD", "EUR").contains(c.getCurrency()))
            .map(c -> new Currency(fullCurrencyData.getDate(), c.getCurrency(), c.getSaleRate(), c.getPurchaseRate()))
            .toList();
  }

  private FullCurrencyData extractCurrencyValueFromJSON(String json) {
    try {
      return objectMapper.readValue(json, FullCurrencyData.class);
    } catch (Exception e) {
      throw new RuntimeException("Error happened while parsing JSON.", e);
    }
  }

  private LocalDate convertDateToLocalDate(String date) {
    try {
      return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    } catch (DateTimeException ex) {
      throw new RuntimeException("The error occurred while parsing the date {}", ex);
    }
  }

  private String convertLocalDateToDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
  }

}
