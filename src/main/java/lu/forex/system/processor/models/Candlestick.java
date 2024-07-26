package lu.forex.system.processor.models;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.enums.TimeFrame;

@Data
@Builder
public class Candlestick implements Serializable {

  @Serial
  private static final long serialVersionUID = 4716144876031926000L;

  private LocalDateTime timestamp;
  private LocalDateTime calculatedTickTime;
  private BigDecimal high;
  private BigDecimal low;
  private BigDecimal open;
  private BigDecimal close;
  private Set<@NonNull TechnicalIndicator> technicalIndicators;

  public @NonNull SignalIndicator getSignalIndicator() {
    if (this.getTechnicalIndicators().stream().filter(technicalIndicator -> SignalIndicator.BULLISH.equals(technicalIndicator.getSignal())).count() == 2) {
      return SignalIndicator.BULLISH;
    } else if (this.getTechnicalIndicators().stream().filter(technicalIndicator -> SignalIndicator.BEARISH.equals(technicalIndicator.getSignal())).count() == 2) {
      return SignalIndicator.BEARISH;
    } else {
      return SignalIndicator.NEUTRAL;
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Candlestick that = (Candlestick) o;
    return Objects.equals(timestamp, that.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(timestamp);
  }
}
