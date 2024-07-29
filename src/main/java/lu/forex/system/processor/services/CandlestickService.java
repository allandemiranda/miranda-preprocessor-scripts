package lu.forex.system.processor.services;

import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.utils.TimeFrameUtils;

@Log4j2
@UtilityClass
public class CandlestickService {

  private static final int REPOSITORY_SIZE = 14;

  @SneakyThrows
  public static Stream<Candlestick> getCandlesticks(final @NonNull BufferedReader bufferedReader, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol) {
    log.info("Getting Candlesticks for symbol {} at timeframe {}", symbol.name(), timeFrame.name());
    final LinkedList<Candlestick> repositoryBuffer = getInitCandlestickRepository();

    return TickService.getTicks(bufferedReader).map(tickTickPair -> {
      final Tick currentTick = tickTickPair.getKey();
      final Tick lastTick = tickTickPair.getValue();

      if (TimeFrameUtils.getCandlestickTimestamp(currentTick.getDateTime(), timeFrame).equals(TimeFrameUtils.getCandlestickTimestamp(lastTick.getDateTime(), timeFrame))) {
        repositoryBuffer.getFirst().getBody().updatePrice(currentTick);
        return null;
      } else if (LocalDateTime.MIN.equals(lastTick.getDateTime())) {
        updateRepositoryBuffer(timeFrame, repositoryBuffer, currentTick);
        return null;
      } else {
        AdxService.calculate(repositoryBuffer.stream().filter(Objects::nonNull).toArray(Candlestick[]::new));
        RsiService.calculate(repositoryBuffer.stream().filter(Objects::nonNull).toArray(Candlestick[]::new));
        calculateSignalIndicator(repositoryBuffer.get(0), repositoryBuffer.get(1));
        final Candlestick lastCandlestick = repositoryBuffer.getFirst();
        updateRepositoryBuffer(timeFrame, repositoryBuffer, currentTick);
        return lastCandlestick;
      }
    }).filter(Objects::nonNull);
  }

  private static void updateRepositoryBuffer(final @NonNull TimeFrame timeFrame, final @NonNull LinkedList<Candlestick> repositoryBuffer, final @NonNull Tick currentTick) {
    repositoryBuffer.addFirst(new Candlestick(currentTick, timeFrame));
    repositoryBuffer.removeLast();
  }

  private static LinkedList<Candlestick> getInitCandlestickRepository() {
    return IntStream.range(0, REPOSITORY_SIZE).mapToObj(i -> (Candlestick) null).collect(Collectors.toCollection(LinkedList::new));
  }

  public static void calculateSignalIndicator(final @NonNull Candlestick currentCandlestick, final Candlestick lastCandlestick) {
    if (Objects.nonNull(lastCandlestick) && currentCandlestick.getAdx().getSignal().equals(currentCandlestick.getRsi().getSignal())) {
      currentCandlestick.setSignalIndicator(currentCandlestick.getAdx().getSignal());
    }
  }

}
