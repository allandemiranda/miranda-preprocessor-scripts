package lu.forex.system.processor.models;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Externalizing {
  private final String symbol;
  private final String timeframe;
  private final Collection<Candlestick> candlesticksMemory;
  private final Collection<Trade> tradesMemory;
  private final Tick lastTick;
}
