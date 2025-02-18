package com.manager.data.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FullExchangeRates {

  private String baseCurrency;
  private String currency;
  private BigDecimal saleRateNB;
  private BigDecimal purchaseRateNB;
  private BigDecimal saleRate;
  private BigDecimal purchaseRate;

}
