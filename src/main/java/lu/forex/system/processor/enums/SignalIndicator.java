package lu.forex.system.processor.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SignalIndicator {
  BULLISH(OrderType.BUY), BEARISH(OrderType.SELL), NEUTRAL(null);

  final OrderType orderType;
}
