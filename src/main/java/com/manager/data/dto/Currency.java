package com.manager.data.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Currency(LocalDate date, String currency, BigDecimal saleRate, BigDecimal purchaseRate)  {
}
