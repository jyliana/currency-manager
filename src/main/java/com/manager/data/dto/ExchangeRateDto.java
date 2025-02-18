package com.manager.data.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateDto {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private LocalDate date;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "sale_rate", nullable = false)
  private BigDecimal saleRate;

  @Column(name = "purchase_rate", nullable = false)
  private BigDecimal purchaseRate;

}
