package lu.forex.system.processor.enums;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Symbol {
  //@formatter:off
  EURUSD(BigDecimal.valueOf(-0.7), BigDecimal.valueOf(-1.0), BigDecimal.valueOf(0.00001)),
  GBPUSD(BigDecimal.valueOf(-0.2), BigDecimal.valueOf(-2.2), BigDecimal.valueOf(0.00001))
  ;
  //@formatter:on

  final BigDecimal swapLong;
  final BigDecimal swapShort;
  final BigDecimal pip;
}
