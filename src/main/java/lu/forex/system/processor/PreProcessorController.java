package lu.forex.system.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.Externalizing;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.services.CandlestickService;
import lu.forex.system.processor.services.TradeService;
import lu.forex.system.processor.utils.PrintsUtils;

@Log4j2
public class PreProcessorController {

  public static void main(String @NonNull [] args) {
    final File rootFolder = new File(args[0]);
    final File inputFolder = new File(rootFolder, args[1]);
    final File outputFolder = new File(rootFolder, args[2]);

    start(inputFolder, outputFolder);
  }

  private static void start(final @NonNull File inputFolder, final File outputFolder) {
    Arrays.stream(Objects.requireNonNull(inputFolder.listFiles())).parallel()
        .filter(file -> Arrays.stream(Symbol.values()).anyMatch(symbol -> symbol.name().equals(file.getName().split("_")[0])))
        .forEach(inputFile -> Arrays.stream(TimeFrame.values()).parallel().forEach(timeFrame -> {
          final Symbol symbol = Symbol.valueOf(inputFile.getName().split("_")[0]);

          PrintsUtils.printLastTickMemoryExcel(inputFile, symbol, outputFolder);
          PrintsUtils.printCandlesticksMemoryExcel(inputFile, timeFrame, symbol, outputFolder);

          try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
            PrintsUtils.printCandlesticksExcel(bufferedReader, timeFrame, symbol, outputFolder);
          } catch (IOException e) {
            log.warn("Can't print the candlesticks excel for symbol {} at timeframe {}", symbol.name(), timeFrame.name(), e);
          }

          try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
            final Collection<Trade> trades = TradeService.getTrades(inputFile, bufferedReader, timeFrame, symbol);
            PrintsUtils.printTradesExcel(trades, timeFrame, symbol, outputFolder);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        }));
  }

  public static Collection<Externalizing> getExternalizingCollection(@NonNull final File inputFolder) {
    return Arrays.stream(Objects.requireNonNull(inputFolder.listFiles())).parallel()
        .filter(file -> Arrays.stream(Symbol.values()).anyMatch(symbol -> symbol.name().equals(file.getName().split("_")[0])))
        .flatMap(inputFile -> Arrays.stream(TimeFrame.values()).parallel().map(timeFrame -> {
          try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
            final Symbol symbol = Symbol.valueOf(inputFile.getName().split("_")[0]);
            final Tick lastTick = PrintsUtils.lastTickMemoryExternalizing(inputFile);
            final Collection<Candlestick> memoryCandlesticks = CandlestickService.getCandlesticksMemory(inputFile, timeFrame, symbol);
            final Collection<Trade> trades = TradeService.getTrades(inputFile, bufferedReader, timeFrame, symbol);
            return new Externalizing(symbol.name(), timeFrame.name(), memoryCandlesticks, trades, lastTick);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        })).toList();
  }

}
