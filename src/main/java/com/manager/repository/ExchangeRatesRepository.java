package com.manager.repository;

import com.manager.data.dto.ExchangeRateDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExchangeRatesRepository extends JpaRepository<ExchangeRateDto, UUID> {

  List<ExchangeRateDto> findAllByDate(LocalDate date);

  List<ExchangeRateDto> findAllByDateBetweenAndCurrency(LocalDate startDate, LocalDate endDate, String currency);

  List<ExchangeRateDto> findAllByDateBetweenAndCurrencyIn(LocalDate startDate, LocalDate endDate, List<String> currencies);

}