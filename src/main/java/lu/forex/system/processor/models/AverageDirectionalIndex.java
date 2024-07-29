package lu.forex.system.processor.models;

import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lu.forex.system.processor.enums.SignalIndicator;

@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class AverageDirectionalIndex {

  private BigDecimal keyAdx;
  private BigDecimal keyPDiP;
  private BigDecimal keyNDiP;
  private BigDecimal keyTr1;
  private BigDecimal keyPDm1;
  private BigDecimal keyNDm1;
  private BigDecimal keyDx;
  private SignalIndicator signal = SignalIndicator.NEUTRAL;

}
