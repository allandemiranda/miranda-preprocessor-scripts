package lu.forex.system.processor.enums;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Symbol {
  //@formatter:off
  EURUSD(BigDecimal.valueOf(-0.7), BigDecimal.valueOf(-1.0), BigDecimal.valueOf(0.00001)),
  USDJPY(BigDecimal.valueOf(-0.1), BigDecimal.valueOf(-0.6), BigDecimal.valueOf(0.001)),
  GBPUSD(BigDecimal.valueOf(-0.2), BigDecimal.valueOf(-2.2), BigDecimal.valueOf(0.00001)),
  USDCAD(BigDecimal.valueOf(-0.8), BigDecimal.valueOf(-0.4), BigDecimal.valueOf(0.00001)),
  AUDUSD(BigDecimal.valueOf(6.3), BigDecimal.valueOf(-14.8), BigDecimal.valueOf(0.00001)),
  USDCHF(BigDecimal.valueOf(0.1), BigDecimal.valueOf(-1.0), BigDecimal.valueOf(0.00001)),
  NZDUSD(BigDecimal.valueOf(2.8), BigDecimal.valueOf(-6.7), BigDecimal.valueOf(0.00001)),
  USDHKD(BigDecimal.valueOf(0.2), BigDecimal.valueOf(-5.8), BigDecimal.valueOf(0.00001)),
  ;
  //@formatter:on

  final BigDecimal swapLong;
  final BigDecimal swapShort;
  final BigDecimal pip;
}
