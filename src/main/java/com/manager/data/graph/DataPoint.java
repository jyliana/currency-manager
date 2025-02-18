package com.manager.data.graph;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class DataPoint {

  private Date date;
  private String currency;
  private BigDecimal saleRate;

}
