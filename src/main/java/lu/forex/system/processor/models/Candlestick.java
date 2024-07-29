package lu.forex.system.processor.models;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.utils.TimeFrameUtils;

@Getter
@Setter
@ToString
public class Candlestick {

  private final LocalDateTime timestamp;
  private final LocalDateTime openTickTimestamp;
  private final CandlestickBody body;
  private final AverageDirectionalIndex adx = new AverageDirectionalIndex();
  private final RelativeStrengthIndex rsi = new RelativeStrengthIndex();
  private SignalIndicator signalIndicator = SignalIndicator.NEUTRAL;

  public Candlestick(final @NonNull Tick tick, final @NonNull TimeFrame timeFrame) {
    this.timestamp = TimeFrameUtils.getCandlestickTimestamp(tick.getDateTime(), timeFrame);
    this.openTickTimestamp = tick.getDateTime();
    this.body = new CandlestickBody(tick);
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
