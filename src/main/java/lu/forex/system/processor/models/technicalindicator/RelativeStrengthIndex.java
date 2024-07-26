package lu.forex.system.processor.models.technicalindicator;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lu.forex.system.processor.enums.Indicator;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.TechnicalIndicator;
import lu.forex.system.processor.utils.MathUtils;

@Data
@Builder
public class RelativeStrengthIndex implements TechnicalIndicator, Serializable {

  @Serial
  private static final long serialVersionUID = -8856119737492040481L;

  private static final String KEY_GAIN = "gain";
  private static final String KEY_LOSS = "loss";
  private static final String KEY_RSI = "rsi";
  private static final String KEY_AVERAGE_GAIN = "averageGain";
  private static final String KEY_AVERAGE_LOSS = "averageLoss";

  private static final int PERIOD = 14;
  private static final BigDecimal OVERBOUGHT = BigDecimal.valueOf(70.0);
  private static final BigDecimal OVERSOLD = BigDecimal.valueOf(30.0);

  private Map<String, BigDecimal> data;
  private SignalIndicator signal;

  @Override
  public @NonNull Indicator getIndicator() {
    return Indicator.RSI;
  }

  @Override
  public @NonNull int getNumberOfCandlesticksToCalculate() {
    return 14;
  }

  @Override
  public @NonNull void calculate(final @NonNull Candlestick @NonNull [] candlesticks) {
    final TechnicalIndicator[] technicalIndicators = IntStream.range(0, candlesticks.length < PERIOD ? 1 : PERIOD)
        .mapToObj(i -> candlesticks[i].getTechnicalIndicators().stream().filter(this::equals).findFirst().orElseThrow()).toArray(TechnicalIndicator[]::new);

    if (candlesticks.length >= 2) {
      final BigDecimal currentClosePrice = candlesticks[0].getClose();
      final BigDecimal lastClosePrice = candlesticks[1].getClose();
      final BigDecimal gain = currentClosePrice.compareTo(lastClosePrice) > 0 ? currentClosePrice.subtract(lastClosePrice) : BigDecimal.ZERO;
      technicalIndicators[0].getData().put(KEY_GAIN, gain);
      final BigDecimal loss = currentClosePrice.compareTo(lastClosePrice) < 0 ? lastClosePrice.subtract(currentClosePrice) : BigDecimal.ZERO;
      technicalIndicators[0].getData().put(KEY_LOSS, loss);

      if (technicalIndicators.length == PERIOD && IntStream.range(0, PERIOD)
          .noneMatch(i -> Objects.isNull(technicalIndicators[i].getData().get(KEY_GAIN)) || Objects.isNull(technicalIndicators[i].getData().get(KEY_LOSS)))) {
        if (Objects.isNull(technicalIndicators[1].getData().get(KEY_AVERAGE_GAIN))) {
          final BigDecimal averageGain = MathUtils.getMed(IntStream.range(0, PERIOD).mapToObj(i -> technicalIndicators[i].getData().get(KEY_GAIN)).toList());
          technicalIndicators[0].getData().put(KEY_AVERAGE_GAIN, averageGain);
          final BigDecimal averageLoss = MathUtils.getMed(IntStream.range(0, PERIOD).mapToObj(i -> technicalIndicators[i].getData().get(KEY_LOSS)).toList());
          technicalIndicators[0].getData().put(KEY_AVERAGE_LOSS, averageLoss);
        } else {
          final BigDecimal averageGain = ((technicalIndicators[1].getData().get(KEY_AVERAGE_GAIN).multiply(BigDecimal.valueOf(PERIOD - 1L))).add(gain)).divide(BigDecimal.valueOf(PERIOD),
              MathUtils.SCALE, MathUtils.ROUNDING_MODE);
          technicalIndicators[0].getData().put(KEY_AVERAGE_GAIN, averageGain);
          final BigDecimal averageLoss = ((technicalIndicators[1].getData().get(KEY_AVERAGE_LOSS).multiply(BigDecimal.valueOf(PERIOD - 1L))).add(loss)).divide(BigDecimal.valueOf(PERIOD),
              MathUtils.SCALE, MathUtils.ROUNDING_MODE);
          technicalIndicators[0].getData().put(KEY_AVERAGE_LOSS, averageLoss);
        }
        final BigDecimal rs = technicalIndicators[0].getData().get(KEY_AVERAGE_GAIN).divide(technicalIndicators[0].getData().get(KEY_AVERAGE_LOSS), MathUtils.SCALE, MathUtils.ROUNDING_MODE);
        final BigDecimal rsi = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), MathUtils.SCALE, MathUtils.ROUNDING_MODE));
        technicalIndicators[0].getData().put(KEY_RSI, rsi);
        if (rsi.compareTo(OVERBOUGHT) > 0) {
          technicalIndicators[0].setSignal(SignalIndicator.BULLISH);
        } else if (rsi.compareTo(OVERSOLD) < 0) {
          technicalIndicators[0].setSignal(SignalIndicator.BEARISH);
        }
      }
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
    final RelativeStrengthIndex that = (RelativeStrengthIndex) o;
    return getIndicator() == that.getIndicator();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getIndicator());
  }
}
