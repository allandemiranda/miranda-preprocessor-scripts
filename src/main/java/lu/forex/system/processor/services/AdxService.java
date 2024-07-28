package lu.forex.system.processor.services;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.models.AverageDirectionalIndex;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.utils.MathUtils;

@UtilityClass
public class AdxService {

  private static final int PERIOD = 14;
  private static final BigDecimal TENDENCY_LINE = BigDecimal.valueOf(50);
  private static final BigDecimal DECIMAL = BigDecimal.valueOf(100);

  public static void calculate(final @NonNull Candlestick @NonNull [] candlesticks) {
    final AverageDirectionalIndex[] technicalIndicators = IntStream.range(0, candlesticks.length < PERIOD ? 1 : PERIOD).mapToObj(i -> candlesticks[i].getAdx())
        .toArray(AverageDirectionalIndex[]::new);

    if (candlesticks.length >= 2) {
      // get TR1
      final BigDecimal trOne = MathUtils.getMax(candlesticks[0].getBody().getHigh().subtract(candlesticks[0].getBody().getLow()),
          candlesticks[0].getBody().getHigh().subtract(candlesticks[0].getBody().getClose()), candlesticks[0].getBody().getLow().subtract(candlesticks[1].getBody().getClose()).abs());
      technicalIndicators[0].setKeyTr1(trOne);

      // get +DM1
      final BigDecimal pDmOne =
          candlesticks[0].getBody().getHigh().subtract(candlesticks[1].getBody().getHigh()).compareTo(candlesticks[1].getBody().getLow().subtract(candlesticks[0].getBody().getLow())) > 0
              ? MathUtils.getMax(candlesticks[0].getBody().getHigh().subtract(candlesticks[1].getBody().getHigh()), BigDecimal.ZERO) : BigDecimal.ZERO;
      technicalIndicators[0].setKeyPDm1(pDmOne);

      // get -DM1
      final BigDecimal nDmOne =
          candlesticks[1].getBody().getLow().subtract(candlesticks[0].getBody().getLow()).compareTo(candlesticks[0].getBody().getHigh().subtract(candlesticks[1].getBody().getHigh())) > 0
              ? MathUtils.getMax(candlesticks[1].getBody().getLow().subtract(candlesticks[0].getBody().getLow()), BigDecimal.ZERO) : BigDecimal.ZERO;
      technicalIndicators[0].setKeyNDm1(nDmOne);

      if (technicalIndicators.length == PERIOD && IntStream.range(0, PERIOD)
          .noneMatch(i -> Objects.isNull(technicalIndicators[i].getKeyTr1()) || Objects.isNull(technicalIndicators[i].getKeyPDm1()) || Objects.isNull(technicalIndicators[i].getKeyNDm1()))) {
        // get TR(P)
        final BigDecimal[] trPArray = new BigDecimal[PERIOD];
        IntStream.range(0, PERIOD).parallel().forEach(i -> trPArray[i] = technicalIndicators[i].getKeyTr1());
        final BigDecimal trP = MathUtils.getSum(trPArray);

        // get +DM(P)
        final BigDecimal[] pDmPArray = new BigDecimal[PERIOD];
        IntStream.range(0, PERIOD).parallel().forEach(i -> pDmPArray[i] = technicalIndicators[i].getKeyPDm1());
        final BigDecimal pDmP = MathUtils.getSum(pDmPArray);

        // get -DM(P)
        final BigDecimal[] nDmPArray = new BigDecimal[PERIOD];
        IntStream.range(0, PERIOD).parallel().forEach(i -> nDmPArray[i] = technicalIndicators[i].getKeyNDm1());
        final BigDecimal nDmP = MathUtils.getSum(nDmPArray);

        // get +DI(P)
        final BigDecimal pDiP = MathUtils.getMultiplication(DECIMAL, MathUtils.getDivision(pDmP, trP));
        technicalIndicators[0].setKeyPDiP(pDiP);

        // get -DI(P)
        final BigDecimal nDiP = MathUtils.getMultiplication(DECIMAL, MathUtils.getDivision(nDmP, trP));
        technicalIndicators[0].setKeyNDiP(nDiP);

        // get DI diff
        final BigDecimal diDiff = pDiP.subtract(nDiP).abs();

        // get DI sum
        final BigDecimal diSum = pDiP.add(nDiP);

        // get DX
        final BigDecimal dx = MathUtils.getMultiplication(DECIMAL, MathUtils.getDivision(diDiff, diSum));
        technicalIndicators[0].setKeyDx(dx);

        if (IntStream.range(0, PERIOD).noneMatch(i -> Objects.isNull(technicalIndicators[i].getKeyDx()))) {
          // get ADX
          final BigDecimal[] adxArray = new BigDecimal[PERIOD];
          IntStream.range(0, PERIOD).parallel().forEach(i -> adxArray[i] = technicalIndicators[i].getKeyDx());
          final BigDecimal adx = MathUtils.getMed(adxArray);
          technicalIndicators[0].setKeyAdx(adx);

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
}
