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
public class RelativeStrengthIndex {

  private BigDecimal keyGain;
  private BigDecimal keyLoss;
  private BigDecimal keyRsi;
  private BigDecimal keyAverageGain;
  private BigDecimal keyAverageLoss;
  private SignalIndicator signal = SignalIndicator.NEUTRAL;

}
