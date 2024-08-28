package lu.forex.system.processor;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.services.TradeService;
import lu.forex.system.processor.services.TradeService.PreTrade;
import lu.forex.system.processor.services.TradeService.TimeScope;
import lu.forex.system.processor.utils.PrintsUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

@Log4j2
public class PreProcessorController {

  public static void main(String @NonNull [] args) {
    final File rootFolder = new File(args[0]);
    final File inputFolder = new File(rootFolder, args[1]);
    final File outputFolder = new File(rootFolder, args[2]);

    start(inputFolder, outputFolder);

  }

  private static void start(final @NonNull File inputFolder, final File outputFolder) {
    final List<File> inputFiles = Arrays.stream(Objects.requireNonNull(inputFolder.listFiles())).filter(file -> Arrays.stream(Symbol.values()).anyMatch(symbol -> symbol.name().equals(file.getName().split("_")[0]))).toList();
    log.info("Number of files to read {}", inputFiles.size());
    final List<Pair<File, TimeFrame>> filePairs = inputFiles.parallelStream().flatMap(inputFile -> Arrays.stream(TimeFrame.values()).map(timeFrame -> Pair.of(inputFile, timeFrame))).toList();
    log.info("Number of timeframes x files to read {}", filePairs.size());
    final List<TradesInfo> tradesInfos = filePairs.parallelStream().flatMap(pair -> {
      final File inputFile = pair.getLeft();
      final TimeFrame timeFrame = pair.getRight();
      final Symbol symbol = Symbol.valueOf(inputFile.getName().split("_")[0]);
      final List<Candlestick> candlestickList = TradeService.getCandlestickList(inputFile, timeFrame, symbol);
      return TradeService.getTimeScopeListMap(timeFrame, symbol, candlestickList).entrySet().parallelStream().map(timeScopeMapEntry -> {
        final TimeScope timeScope = timeScopeMapEntry.getKey();
        final List<PreTrade> profitListMap = timeScopeMapEntry.getValue();
        return new TradesInfo(inputFile, timeFrame, symbol, candlestickList, timeScope, profitListMap);
      });
    }).toList();
    log.info("Number of trades: {}", tradesInfos.size());

    AtomicInteger tradesCounter = new AtomicInteger(0);

    tradesInfos.parallelStream().map(tradesInfo -> {
      final File inputFile = tradesInfo.getInputFile();
      final TimeFrame timeFrame = tradesInfo.getTimeFrame();
      final Symbol symbol = tradesInfo.getSymbol();
      final TimeScope timeScope = tradesInfo.getTimeScope();
      final List<PreTrade> profitListMap = tradesInfo.getProfitListMap();
      final Trade trade = TradeService.getTrade(inputFile, symbol, profitListMap, timeScope, TradeService.getRangerProfit(), timeFrame);
      final int incremented = tradesCounter.incrementAndGet();
      final double finished = (incremented * 100.0)/tradesInfos.size();
      log.info("Number of trades finished: {} of {} -> {}%", incremented, tradesInfos.size(), finished);
      return Triple.of(timeFrame, symbol, trade);
    }).collect(Collectors.groupingBy(triple -> Pair.of(triple.getLeft(), triple.getMiddle()))).forEach((timeFrameSymbolPair, triples) -> {
      final List<Trade> tradeList = triples.stream().map(Triple::getRight).filter(Objects::nonNull).toList();
      final TimeFrame timeFrame = timeFrameSymbolPair.getLeft();
      final Symbol symbol = timeFrameSymbolPair.getRight();
      PrintsUtils.printTradesExcel(tradeList, timeFrame, symbol, outputFolder);
    });

    log.warn("End of process !");
  }

  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor
  private static class TradesInfo {
    private final File inputFile;
    private final TimeFrame timeFrame;
    private final Symbol symbol;
    private final List<Candlestick> candlestickList;

    private final TimeScope timeScope;
    private final List<PreTrade> profitListMap;
  }

}
