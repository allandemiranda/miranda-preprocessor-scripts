package lu.forex.system.processor.services;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.RelativeStrengthIndex;
import lu.forex.system.processor.utils.MathUtils;

@UtilityClass
public class RsiService {

  private static final int PERIOD = 14;
  private static final BigDecimal OVERBOUGHT = BigDecimal.valueOf(70.0);
  private static final BigDecimal OVERSOLD = BigDecimal.valueOf(30.0);

  public static void calculate(final @NonNull Candlestick @NonNull [] candlesticks) {
    final RelativeStrengthIndex[] technicalIndicators = IntStream.range(0, candlesticks.length < PERIOD ? 1 : PERIOD).mapToObj(i -> candlesticks[i].getRsi())
        .toArray(RelativeStrengthIndex[]::new);

    if (candlesticks.length >= 2) {
      final BigDecimal currentClosePrice = candlesticks[0].getBody().getClose();
      final BigDecimal lastClosePrice = candlesticks[1].getBody().getClose();
      final BigDecimal gain = currentClosePrice.compareTo(lastClosePrice) > 0 ? currentClosePrice.subtract(lastClosePrice) : BigDecimal.ZERO;
      technicalIndicators[0].setKeyGain(gain);
      final BigDecimal loss = currentClosePrice.compareTo(lastClosePrice) < 0 ? lastClosePrice.subtract(currentClosePrice) : BigDecimal.ZERO;
      technicalIndicators[0].setKeyLoss(loss);

      if (technicalIndicators.length == PERIOD && IntStream.range(0, PERIOD)
          .noneMatch(i -> Objects.isNull(technicalIndicators[i].getKeyGain()) || Objects.isNull(technicalIndicators[i].getKeyLoss()))) {
        if (Objects.isNull(technicalIndicators[1].getKeyAverageGain())) {
          final BigDecimal averageGain = MathUtils.getMed(IntStream.range(0, PERIOD).mapToObj(i -> technicalIndicators[i].getKeyGain()).toList());
          technicalIndicators[0].setKeyAverageGain(averageGain);
          final BigDecimal averageLoss = MathUtils.getMed(IntStream.range(0, PERIOD).mapToObj(i -> technicalIndicators[i].getKeyLoss()).toList());
          technicalIndicators[0].setKeyAverageLoss(averageLoss);
        } else {
          final BigDecimal averageGain = ((technicalIndicators[1].getKeyAverageGain().multiply(BigDecimal.valueOf(PERIOD - 1L))).add(gain)).divide(BigDecimal.valueOf(PERIOD),
              MathUtils.SCALE, MathUtils.ROUNDING_MODE);
          technicalIndicators[0].setKeyAverageGain(averageGain);
          final BigDecimal averageLoss = ((technicalIndicators[1].getKeyAverageLoss().multiply(BigDecimal.valueOf(PERIOD - 1L))).add(loss)).divide(BigDecimal.valueOf(PERIOD),
              MathUtils.SCALE, MathUtils.ROUNDING_MODE);
          technicalIndicators[0].setKeyAverageLoss(averageLoss);
        }
        final BigDecimal rs = technicalIndicators[0].getKeyAverageGain().divide(technicalIndicators[0].getKeyAverageLoss(), MathUtils.SCALE, MathUtils.ROUNDING_MODE);
        final BigDecimal rsi = BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), MathUtils.SCALE, MathUtils.ROUNDING_MODE));
        technicalIndicators[0].setKeyRsi(rsi);
        if (rsi.compareTo(OVERBOUGHT) > 0) {
          technicalIndicators[0].setSignal(SignalIndicator.BULLISH);
        } else if (rsi.compareTo(OVERSOLD) < 0) {
          technicalIndicators[0].setSignal(SignalIndicator.BEARISH);
        }
      }
    }
  }
}
