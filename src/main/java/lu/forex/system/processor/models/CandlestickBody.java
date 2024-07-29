package lu.forex.system.processor.models;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class CandlestickBody {

  private final BigDecimal open;
  @Setter(AccessLevel.PRIVATE)
  private BigDecimal high;
  @Setter(AccessLevel.PRIVATE)
  private BigDecimal low;
  @Setter(AccessLevel.PRIVATE)
  private BigDecimal close;

  public CandlestickBody(final @NonNull Tick tick) {
    final BigDecimal price = getPrice(tick);
    this.close = price;
    this.open = price;
    this.low = price;
    this.high = price;
  }

  @NonNull
  private static BigDecimal getPrice(final @NonNull Tick tick) {
    return tick.getBid();
  }

  public void updatePrice(final @NonNull Tick tick) {
    final BigDecimal price = getPrice(tick);
    this.setClose(price);
    if (price.compareTo(this.getLow()) < 0) {
      this.setLow(price);
    } else if (price.compareTo(this.getHigh()) > 0) {
      this.setHigh(price);
    }
  }

}