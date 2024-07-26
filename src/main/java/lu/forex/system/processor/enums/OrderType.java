package lu.forex.system.processor.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import lombok.NonNull;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.utils.MathUtils;

public enum OrderType {
  BUY, SELL;

  public BigDecimal getProfit(final @NonNull Tick last, final @NonNull Tick current, final @NonNull Symbol symbol) {
    final BigDecimal tmpProfit = switch (this) {
      case BUY -> current.getBid().subtract(last.getBid()).divide(symbol.getPip(), MathUtils.SCALE, RoundingMode.HALF_UP);
      case SELL -> last.getAsk().subtract(current.getAsk()).divide(symbol.getPip(), MathUtils.SCALE, RoundingMode.HALF_UP);
    };
    if (last.getDateTime().getDayOfWeek().equals(DayOfWeek.TUESDAY) && current.getDateTime().getDayOfWeek().equals(DayOfWeek.WEDNESDAY)) {
      return switch (this) {
        case BUY -> tmpProfit.add(symbol.getSwapLong().multiply(symbol.getSwapLong()));
        case SELL -> tmpProfit.add(symbol.getPip().multiply(symbol.getSwapShort()));
      };
    } else {
      return tmpProfit;
    }
  }
}
