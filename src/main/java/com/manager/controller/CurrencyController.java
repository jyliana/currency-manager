package com.manager.controller;

import com.manager.chart.ChartCreator;
import com.manager.data.dto.Currency;
import com.manager.service.BankService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/currencies")
@AllArgsConstructor
public class CurrencyController {

  private BankService service;
  private ChartCreator creator;

  @GetMapping("/day")
  public ResponseEntity<List<Currency>> getCurrencyRates(@RequestParam String date) {
    var json = service.getExchangeRates(date);

    return ResponseEntity.status(HttpStatus.OK).body(json);
  }

  @GetMapping("/dates")
  public ResponseEntity<List<Currency>> getCurrencyRatesForDates(@RequestParam String startDate, @RequestParam String endDate, @RequestParam String currency) {
    var json = service.getAllRecordsForDates(startDate, endDate, currency);
    creator.parsePeriod(json, startDate + "_" + endDate);

    return ResponseEntity.status(HttpStatus.OK).body(json);
  }

  @GetMapping("/period")
  public ResponseEntity<List<Currency>> getCurrencyRatesForPeriod(@RequestParam String period, @RequestParam String currency) {
    var json = service.getAllRecordsForPeriod(period, currency);
    creator.parsePeriod(json, period);
    return ResponseEntity.status(HttpStatus.OK).body(json);
  }

  @GetMapping("/period/both_currencies")
  public ResponseEntity<String> getCurrencyRatesForPeriodForCurrencies(@RequestParam String period) {
    var currenciesList = service.getAllRecordsForPeriod(period, null);

    currenciesList.stream()
            .collect(Collectors.groupingBy(Currency::currency))
            .forEach((currency, list) -> creator.parsePeriod(list, period));

    return ResponseEntity.status(HttpStatus.OK).body("Charts for USD and EUR successfully saved");
  }

}
