package com.manager.controller;

import com.manager.chart.ChartCreator;
import com.manager.data.dto.Currency;
import com.manager.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class CurrencyController {

  private final BankService service;
  private final ChartCreator creator;

  @GetMapping("/day")
  public ResponseEntity<List<Currency>> getCurrencyRates(@RequestParam String date) {
    var result = service.getExchangeRates(date);

    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  @GetMapping("/dates")
  public ResponseEntity<List<Currency>> getCurrencyRatesForDates(@RequestParam String startDate,
                                                                 @RequestParam String endDate,
                                                                 @RequestParam(required = false) String currency) {
    var currenciesList = service.getAllRecordsForDates(startDate, endDate, currency);

    currenciesList.stream()
            .collect(Collectors.groupingBy(Currency::currency))
            .forEach((cur, list) -> creator.parsePeriod(list, startDate + "-" + endDate));

    return ResponseEntity.status(HttpStatus.OK).body(currenciesList);
  }


  @GetMapping("/period")
  public ResponseEntity<String> getCurrencyRatesForPeriodForCurrencies(@RequestParam String period,
                                                                       @RequestParam(required = false) String currency) {
    var currenciesList = service.getAllRecordsForPeriod(period, currency);

    currenciesList.stream()
            .collect(Collectors.groupingBy(Currency::currency))
            .forEach((cur, list) -> creator.parsePeriod(list, period));
    var line = Objects.isNull(currency) ? "Charts for USD and EUR successfully saved." :
            String.format("Chart for %s successfully saved.", currency);

    return ResponseEntity.status(HttpStatus.OK).body(line);
  }

}
