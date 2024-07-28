package lu.forex.system.processor.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.utils.MathUtils;

@Getter
@AllArgsConstructor
public enum SignalIndicator {
  BULLISH(OrderType.BUY), BEARISH(OrderType.SELL), NEUTRAL(null);

  private final OrderType orderType;

  public enum OrderType {
    BUY, SELL;

    @NonNull
    public BigDecimal getProfit(final @NonNull Tick lastTick, final @NonNull Tick currentTick, final @NonNull Symbol symbol) {
      final BigDecimal tmpProfit = switch (this) {
        case BUY -> currentTick.getBid().subtract(lastTick.getBid()).divide(symbol.getPip(), MathUtils.SCALE, RoundingMode.HALF_UP);
        case SELL -> lastTick.getAsk().subtract(currentTick.getAsk()).divide(symbol.getPip(), MathUtils.SCALE, RoundingMode.HALF_UP);
      };
      if (lastTick.getDateTime().getDayOfWeek().equals(DayOfWeek.TUESDAY) && currentTick.getDateTime().getDayOfWeek().equals(DayOfWeek.WEDNESDAY)) {
        return switch (this) {
          case BUY -> tmpProfit.add(symbol.getSwapLong());
          case SELL -> tmpProfit.add(symbol.getSwapShort());
        };
      } else {
        return tmpProfit;
      }
    }
  }
}
