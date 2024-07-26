package lu.forex.system.processor.models;

import java.io.Serial;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Trade implements Serializable {

  @Serial
  private static final long serialVersionUID = 1494334968768706815L;

  private int stopLoss;
  private int takeProfit;
  private DayOfWeek slotWeek;
  private LocalTime slotStart;
  private LocalTime slotEnd;

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Trade trade = (Trade) o;
    return getStopLoss() == trade.getStopLoss() && getTakeProfit() == trade.getTakeProfit() && getSlotWeek() == trade.getSlotWeek() && Objects.equals(getSlotStart(), trade.getSlotStart())
           && Objects.equals(getSlotEnd(), trade.getSlotEnd());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStopLoss(), getTakeProfit(), getSlotWeek(), getSlotStart(), getSlotEnd());
  }
}
