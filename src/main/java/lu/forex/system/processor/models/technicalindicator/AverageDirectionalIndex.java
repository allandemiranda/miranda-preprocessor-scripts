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
public class AverageDirectionalIndex implements TechnicalIndicator, Serializable {

  @Serial
  private static final long serialVersionUID = 7505982951560792723L;

  private static final String KEY_ADX = "adx";
  private static final String KEY_P_DI_P = "+di(P)";
  private static final String KEY_N_DI_P = "-di(P)";
  private static final String KEY_TR_1 = "tr1";
  private static final String KEY_P_DM_1 = "+dm1";
  private static final String KEY_N_DM_1 = "-dm1";
  private static final String KEY_DX = "dx";

  private static final int PERIOD = 14;
  private static final BigDecimal TENDENCY_LINE = BigDecimal.valueOf(50);
  private static final BigDecimal DECIMAL = BigDecimal.valueOf(100);
  private Map<String, BigDecimal> data;
  private SignalIndicator signal;

  @Override
  public @NonNull Indicator getIndicator() {
    return Indicator.ADX;
  }

  @Override
  public @NonNull int getNumberOfCandlesticksToCalculate() {
    return PERIOD;
  }

  @Override
  public @NonNull void calculate(final @NonNull Candlestick @NonNull [] candlesticks) {
    final TechnicalIndicator[] technicalIndicators = IntStream.range(0, candlesticks.length < PERIOD ? 1 : PERIOD)
        .mapToObj(i -> candlesticks[i].getTechnicalIndicators().stream().filter(this::equals).findFirst().orElseThrow()).toArray(TechnicalIndicator[]::new);

    if (candlesticks.length >= 2) {
      // get TR1
      final BigDecimal trOne = MathUtils.getMax(candlesticks[0].getHigh().subtract(candlesticks[0].getLow()), candlesticks[0].getHigh().subtract(candlesticks[0].getClose()),
          candlesticks[0].getLow().subtract(candlesticks[1].getClose()).abs());
      technicalIndicators[0].getData().put(KEY_TR_1, trOne);

      // get +DM1
      final BigDecimal pDmOne = candlesticks[0].getHigh().subtract(candlesticks[1].getHigh()).compareTo(candlesticks[1].getLow().subtract(candlesticks[0].getLow())) > 0 ? MathUtils.getMax(
          candlesticks[0].getHigh().subtract(candlesticks[1].getHigh()), BigDecimal.ZERO) : BigDecimal.ZERO;
      technicalIndicators[0].getData().put(KEY_P_DM_1, pDmOne);

      // get -DM1
      final BigDecimal nDmOne = candlesticks[1].getLow().subtract(candlesticks[0].getLow()).compareTo(candlesticks[0].getHigh().subtract(candlesticks[1].getHigh())) > 0 ? MathUtils.getMax(
          candlesticks[1].getLow().subtract(candlesticks[0].getLow()), BigDecimal.ZERO) : BigDecimal.ZERO;
      technicalIndicators[0].getData().put(KEY_N_DM_1, nDmOne);

      if (technicalIndicators.length == PERIOD && IntStream.range(0, PERIOD).noneMatch(
          i -> Objects.isNull(technicalIndicators[i].getData().get(KEY_TR_1)) || Objects.isNull(technicalIndicators[i].getData().get(KEY_P_DM_1)) || Objects.isNull(
              technicalIndicators[i].getData().get(KEY_N_DM_1)))) {
        // get TR(P)
        final BigDecimal[] trPArray = new BigDecimal[PERIOD];
        IntStream.range(0, PERIOD).parallel().forEach(i -> trPArray[i] = technicalIndicators[i].getData().get(KEY_TR_1));
        final BigDecimal trP = MathUtils.getSum(trPArray);

        // get +DM(P)
        final BigDecimal[] pDmPArray = new BigDecimal[PERIOD];
        IntStream.range(0, PERIOD).parallel().forEach(i -> pDmPArray[i] = technicalIndicators[i].getData().get(KEY_P_DM_1));
        final BigDecimal pDmP = MathUtils.getSum(pDmPArray);

        // get -DM(P)
        final BigDecimal[] nDmPArray = new BigDecimal[PERIOD];
        IntStream.range(0, PERIOD).parallel().forEach(i -> nDmPArray[i] = technicalIndicators[i].getData().get(KEY_N_DM_1));
        final BigDecimal nDmP = MathUtils.getSum(nDmPArray);

        // get +DI(P)
        final BigDecimal pDiP = MathUtils.getMultiplication(DECIMAL, MathUtils.getDivision(pDmP, trP));
        technicalIndicators[0].getData().put(KEY_P_DI_P, pDiP);

        // get -DI(P)
        final BigDecimal nDiP = MathUtils.getMultiplication(DECIMAL, MathUtils.getDivision(nDmP, trP));
        technicalIndicators[0].getData().put(KEY_N_DI_P, nDiP);

        // get DI diff
        final BigDecimal diDiff = pDiP.subtract(nDiP).abs();

        // get DI sum
        final BigDecimal diSum = pDiP.add(nDiP);

        // get DX
        final BigDecimal dx = MathUtils.getMultiplication(DECIMAL, MathUtils.getDivision(diDiff, diSum));
        technicalIndicators[0].getData().put(KEY_DX, dx);

        if (IntStream.range(0, PERIOD).noneMatch(i -> Objects.isNull(technicalIndicators[i].getData().get(KEY_DX)))) {
          // get ADX
          final BigDecimal[] adxArray = new BigDecimal[PERIOD];
          IntStream.range(0, PERIOD).parallel().forEach(i -> adxArray[i] = technicalIndicators[i].getData().get(KEY_DX));
          final BigDecimal adx = MathUtils.getMed(adxArray);
          technicalIndicators[0].getData().put(KEY_ADX, adx);

          // Setting Signal
          if (adx.compareTo(TENDENCY_LINE) > 0) {
            if (pDiP.compareTo(nDiP) > 0) {
              technicalIndicators[0].setSignal(SignalIndicator.BULLISH);
            } else if (pDiP.compareTo(nDiP) < 0) {
              technicalIndicators[0].setSignal(SignalIndicator.BEARISH);
            }
          }
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
    final AverageDirectionalIndex that = (AverageDirectionalIndex) o;
    return getIndicator() == that.getIndicator();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getIndicator());
  }
}
