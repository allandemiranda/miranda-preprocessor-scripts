package lu.forex.system.processor.models;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class Trade {

  private final int stopLoss;
  private final int takeProfit;
  private final DayOfWeek slotWeek;
  private final int slotStart;

  private final long ordersTotal;
  private final long takeProfitTotal;
  private final long stopLossTotal;
  private final BigDecimal hitPercentage;
  private final BigDecimal profitTotal;

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Trade trade = (Trade) o;
    return stopLoss == trade.stopLoss && takeProfit == trade.takeProfit && slotStart == trade.slotStart && slotWeek == trade.slotWeek;
  }

  @Override
  public int hashCode() {
    return Objects.hash(stopLoss, takeProfit, slotWeek, slotStart);
  }
}
