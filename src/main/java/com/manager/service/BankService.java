package com.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager.data.dto.Currency;
import com.manager.data.dto.ExchangeRateDto;
import com.manager.data.dto.FullCurrencyData;
import com.manager.exception.DataParsingException;
import com.manager.repository.ExchangeRatesRepository;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class BankService {

  private final PrivatClient client;
  private final ObjectMapper objectMapper;
  private final ExchangeRatesRepository repository;

  public List<Currency> getAllRecordsForPeriod(String period, String currency) {
    var endDate = LocalDate.now();

    var startDate = switch (period) {
      case "week" -> endDate.minusWeeks(1);
      case "month" -> endDate.minusMonths(1);
      case "quarter" -> endDate.minusMonths(3);
      case "half-year" -> endDate.minusMonths(6);
      case "nine-months" -> endDate.minusMonths(9);
      case "year" -> endDate.minusMonths(12);
      default -> LocalDate.now().minusDays(1);
    };

    return getAllRecordsForDates(convertLocalDateToDate(startDate), convertLocalDateToDate(endDate), currency);
  }

  public List<ExchangeRateDto> checkArchivedRates(String date) {
    var localDate = convertDateToLocalDate(date);

    return repository.findAllByDate(localDate);
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

  private List<Currency> getRatesForMissingDays(String currency, List<LocalDate> missingDays) {
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
        try {
          Thread.sleep(duration);
        } catch (InterruptedException e) {
          throw new RuntimeException("Error while waiting for the next request", e);
        }
      }
    }
    return allRates;
  }

  private List<Currency> convertRatesDtoToCurrencyRates(List<ExchangeRateDto> list) {
    return list.stream()
            .map(c -> new Currency(c.getDate(), c.getCurrency(), c.getSaleRate(), c.getPurchaseRate()))
            .toList();
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
      throw new DataParsingException("Error happened while parsing JSON.", e);
    }
  }

  private LocalDate convertDateToLocalDate(String date) {
    try {
      return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    } catch (DateTimeException ex) {
      throw new DataParsingException("The error occurred while parsing the date {}", ex);
    }
  }

  private String convertLocalDateToDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
  }

}
