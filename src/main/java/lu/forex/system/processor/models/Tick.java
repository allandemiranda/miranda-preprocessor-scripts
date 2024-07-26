package lu.forex.system.processor.models;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Tick implements Serializable {

  @Serial
  private static final long serialVersionUID = -5710627255980078262L;

  private LocalDateTime dateTime;
  private BigDecimal bid;
  private BigDecimal ask;

  public BigDecimal getSpread() {
    return bid.subtract(ask).multiply(BigDecimal.valueOf(-1.0));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Tick tick = (Tick) o;
    return Objects.equals(getDateTime(), tick.getDateTime());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getDateTime());
  }
}
