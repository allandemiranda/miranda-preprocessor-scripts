package lu.forex.system.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.services.TradeService;
import lu.forex.system.processor.utils.PrintsUtils;
import org.apache.commons.lang3.tuple.Triple;

public class PreProcessorController {

  public static void main(String[] args) {
    final File rootFolder = new File("C:\\Users\\AllanDeMirandaSilva\\Downloads\\processing");
    final File inputFolder = new File(rootFolder, "inputMt5");
    final File outputFolder = new File(rootFolder, "outputMt5");

    Arrays.stream(Objects.requireNonNull(inputFolder.listFiles())).parallel()
        .filter(file -> Arrays.stream(Symbol.values()).anyMatch(symbol -> symbol.name().equals(file.getName().split("_")[0])))
        .flatMap(file -> Arrays.stream(TimeFrame.values()).map(timeFrame -> Triple.of(file, timeFrame, Symbol.valueOf(file.getName().split("_")[0])))).forEach(triple -> {
          final File inputFile = triple.getLeft();
          final TimeFrame timeFrame = triple.getMiddle();
          final Symbol symbol = triple.getRight();

          try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
            PrintsUtils.printCandlesticksExcel(bufferedReader, timeFrame, symbol, outputFolder);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }

          try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
            final Collection<Trade> trades = TradeService.getTrades(bufferedReader, timeFrame, symbol).toList();
            PrintsUtils.printTradesExcel(trades, timeFrame, symbol, outputFolder);
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }

        });

  }
}
