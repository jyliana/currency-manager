package com.manager.data.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class FullCurrencyData {

  @JsonFormat(pattern = "dd.MM.yyyy")
  private LocalDate date;
  private String bank;
  private String baseCurrency;
  private String baseCurrencyLit;
  private List<FullExchangeRates> exchangeRate;

}
