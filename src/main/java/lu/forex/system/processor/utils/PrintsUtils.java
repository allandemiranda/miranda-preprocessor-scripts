package lu.forex.system.processor.utils;

import com.opencsv.CSVWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.services.CandlestickService;
import lu.forex.system.processor.services.TickService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Log4j2
@UtilityClass
public class PrintsUtils {

  @SneakyThrows
  public static void printCandlesticksExcel(final @NonNull BufferedReader bufferedReader, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol,
      final @NonNull File outputFolder) {
    log.info("Printing Candlesticks Excel for symbol {} at timeframe {}", symbol.name(), timeFrame.name());
    try (final Workbook workbook = new XSSFWorkbook()) {
      final Sheet sheet = workbook.createSheet(symbol.name());
      final Row headerRow = sheet.createRow(0);
      final String[] header = new String[]{"Timestamp", "Open", "High", "Low", "Close", "ADX_adx", "ADX_+di(P)", "ADX_-di(P)", "ADX_tr1", "ADX_+dm1", "ADX_-dm1", "ADX_dx",
          "ADX_signalIndicator", "RSI_gain", "RSI_loss", "RSI_averageGain", "RSI_averageLoss", "RSI_rsi", "RSI_signalIndicator", "Candlestick_signalIndicator"};
      IntStream.range(0, header.length).forEach(i -> headerRow.createCell(i).setCellValue(header[i]));

      final AtomicInteger i = new AtomicInteger(1);
      CandlestickService.getCandlesticks(bufferedReader, timeFrame, symbol).forEach(candlestick -> {
        final Row row = sheet.createRow(i.getAndIncrement());
        IntStream.range(0, header.length).forEach(j -> {
          final Cell cell = row.createCell(j);
          switch (j) {
            case 0 -> XmlUtils.setCellValue(candlestick.getTimestamp(), cell);
            case 1 -> XmlUtils.setCellValue(candlestick.getBody().getOpen(), cell);
            case 2 -> XmlUtils.setCellValue(candlestick.getBody().getHigh(), cell);
            case 3 -> XmlUtils.setCellValue(candlestick.getBody().getLow(), cell);
            case 4 -> XmlUtils.setCellValue(candlestick.getBody().getClose(), cell);
            case 5 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyAdx(), cell);
            case 6 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyPDiP(), cell);
            case 7 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyNDiP(), cell);
            case 8 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyTr1(), cell);
            case 9 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyPDm1(), cell);
            case 10 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyNDm1(), cell);
            case 11 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyDx(), cell);
            case 12 -> XmlUtils.setCellValue(candlestick.getAdx().getSignal(), cell);
            case 13 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyGain(), cell);
            case 14 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyLoss(), cell);
            case 15 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyAverageGain(), cell);
            case 16 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyAverageLoss(), cell);
            case 17 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyRsi(), cell);
            case 18 -> XmlUtils.setCellValue(candlestick.getRsi().getSignal(), cell);
            case 19 -> XmlUtils.setCellValue(candlestick.getSignalIndicator(), cell);
          }
        });
      });
      workbook.write(new FileOutputStream(new File(outputFolder, symbol.name().concat("_").concat(timeFrame.name()).concat("_candlesticks.xlsx"))));
    }
    log.info("Candlesticks Excel for symbol {} at timeframe {} printed", symbol.name(), timeFrame.name());
  }

  @SneakyThrows
  public static void printCandlesticksMemoryExcel(final @NonNull File inputFile, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol, final @NonNull File outputFolder) {
    log.info("Printing Memory Candlesticks Excel for symbol {} at timeframe {}", symbol.name(), timeFrame.name());
    try (final Workbook workbook = new XSSFWorkbook(); FileWriter fileWriter = new FileWriter(
        new File(outputFolder, symbol.name().concat("_").concat(timeFrame.name()).concat("_candlesticks_memory.csv"))); CSVWriter csvWriter = new CSVWriter(fileWriter)) {
      final Sheet sheet = workbook.createSheet(symbol.name());
      final Row headerRow = sheet.createRow(0);
      final String[] header = new String[]{"Day", "Hour", "Open", "High", "Low", "Close", "ADX_adx", "ADX_+di(P)", "ADX_-di(P)", "ADX_tr1", "ADX_+dm1", "ADX_-dm1", "ADX_dx",
          "ADX_signalIndicator", "RSI_gain", "RSI_loss", "RSI_averageGain", "RSI_averageLoss", "RSI_rsi", "RSI_signalIndicator", "Candlestick_signalIndicator"};
      IntStream.range(0, header.length).forEach(i -> headerRow.createCell(i).setCellValue(header[i]));
      csvWriter.writeNext(header);

      final AtomicInteger i = new AtomicInteger(1);
      CandlestickService.getCandlesticksMemory(inputFile, timeFrame, symbol).forEach(candlestick -> {
        final Row row = sheet.createRow(i.getAndIncrement());
        IntStream.range(0, header.length).forEach(j -> {
          final Cell cell = row.createCell(j);
          switch (j) {
            case 0 -> XmlUtils.setCellValue(candlestick.getTimestamp().toLocalDate(), cell);
            case 1 -> XmlUtils.setCellValue(candlestick.getTimestamp().toLocalTime(), cell);
            case 2 -> XmlUtils.setCellValue(candlestick.getBody().getOpen(), cell);
            case 3 -> XmlUtils.setCellValue(candlestick.getBody().getHigh(), cell);
            case 4 -> XmlUtils.setCellValue(candlestick.getBody().getLow(), cell);
            case 5 -> XmlUtils.setCellValue(candlestick.getBody().getClose(), cell);
            case 6 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyAdx(), cell);
            case 7 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyPDiP(), cell);
            case 8 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyNDiP(), cell);
            case 9 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyTr1(), cell);
            case 10 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyPDm1(), cell);
            case 11 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyNDm1(), cell);
            case 12 -> XmlUtils.setCellValue(candlestick.getAdx().getKeyDx(), cell);
            case 13 -> XmlUtils.setCellValue(candlestick.getAdx().getSignal(), cell);
            case 14 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyGain(), cell);
            case 15 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyLoss(), cell);
            case 16 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyAverageGain(), cell);
            case 17 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyAverageLoss(), cell);
            case 18 -> XmlUtils.setCellValue(candlestick.getRsi().getKeyRsi(), cell);
            case 19 -> XmlUtils.setCellValue(candlestick.getRsi().getSignal(), cell);
            case 20 -> XmlUtils.setCellValue(candlestick.getSignalIndicator(), cell);
          }
        });
        final String[] lineCsv = IntStream.range(0, header.length).mapToObj(j -> switch (j) {
          case 0 -> candlestick.getTimestamp().toLocalDate();
          case 1 -> candlestick.getTimestamp().toLocalTime();
          case 2 -> candlestick.getBody().getOpen();
          case 3 -> candlestick.getBody().getHigh();
          case 4 -> candlestick.getBody().getLow();
          case 5 -> candlestick.getBody().getClose();
          case 6 -> candlestick.getAdx().getKeyAdx();
          case 7 -> candlestick.getAdx().getKeyPDiP();
          case 8 -> candlestick.getAdx().getKeyNDiP();
          case 9 -> candlestick.getAdx().getKeyTr1();
          case 10 -> candlestick.getAdx().getKeyPDm1();
          case 11 -> candlestick.getAdx().getKeyNDm1();
          case 12 -> candlestick.getAdx().getKeyDx();
          case 13 -> candlestick.getAdx().getSignal();
          case 14 -> candlestick.getRsi().getKeyGain();
          case 15 -> candlestick.getRsi().getKeyLoss();
          case 16 -> candlestick.getRsi().getKeyAverageGain();
          case 17 -> candlestick.getRsi().getKeyAverageLoss();
          case 18 -> candlestick.getRsi().getKeyRsi();
          case 19 -> candlestick.getRsi().getSignal();
          case 20 -> candlestick.getSignalIndicator();
          default -> throw new IllegalStateException("Unexpected value: " + j);
        }).map(Object::toString).toArray(String[]::new);
        csvWriter.writeNext(lineCsv);
      });
      workbook.write(new FileOutputStream(new File(outputFolder, symbol.name().concat("_").concat(timeFrame.name()).concat("_candlesticks_memory.xlsx"))));
    }
    log.info("Memory Candlesticks Excel for symbol {} at timeframe {} printed", symbol.name(), timeFrame.name());
  }

  @SneakyThrows
  public static void printLastTickMemoryExcel(final @NonNull File inputFile, final @NonNull Symbol symbol, final @NonNull File outputFolder) {
    log.info("Printing Last Tick Excel for symbol {}", symbol.name());
    try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile)); FileWriter fileWriter = new FileWriter(
        new File(outputFolder, symbol.name().concat("_lastTick.csv"))); CSVWriter csvWriter = new CSVWriter(fileWriter)) {
      final AtomicReference<Tick> tick = new AtomicReference<>(new Tick(LocalDateTime.MIN, BigDecimal.valueOf(-1d), BigDecimal.valueOf(-1d)));
      TickService.getTicks(bufferedReader).forEach(t -> tick.set(t.getKey()));
      final String[] header = new String[]{"dateTime", "bid", "ask", "symbol"};
      csvWriter.writeNext(header);
      final String[] line = new String[]{tick.get().getDateTime().toString(), String.valueOf(tick.get().getBid().doubleValue()), String.valueOf(tick.get().getAsk().doubleValue()),
          symbol.name()};
      csvWriter.writeNext(line);
    }
  }

  @SneakyThrows
  public static Tick lastTickMemoryExternalizing(final @NonNull File inputFile) {
    log.info("Printing Last Tick Externalizing");
    try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
      final AtomicReference<Tick> tick = new AtomicReference<>(new Tick(LocalDateTime.MIN, BigDecimal.valueOf(-1d), BigDecimal.valueOf(-1d)));
      TickService.getTicks(bufferedReader).forEach(t -> tick.set(t.getKey()));
      return tick.get();
    }
  }

  @SneakyThrows
  public static void printTradesExcel(final @NonNull Collection<Trade> tradesCollection, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol, final @NonNull File outputFolder) {
    log.info("Printing Trades Excel for symbol {} at timeframe {}", symbol.name(), timeFrame.name());
    final DayOfWeek[] dayOfWeeks = Arrays.stream(DayOfWeek.values()).filter(dayOfWeek -> !DayOfWeek.SATURDAY.equals(dayOfWeek) && !DayOfWeek.SUNDAY.equals(dayOfWeek))
        .toArray(DayOfWeek[]::new);
    final int[] times = IntStream.range(0, 24 / timeFrame.getSlotTimeH()).toArray();
    try (final Workbook workbook = new XSSFWorkbook(); FileWriter fileWriter = new FileWriter(
        new File(outputFolder, symbol.name().concat("_").concat(timeFrame.name()).concat("_trades_memory.csv"))); CSVWriter csvWriter = new CSVWriter(fileWriter)) {
      Stream.of("TP", "SL", "TOTAL", "PERCENTAGE_TP", "BALANCE").forEach(sheetName -> {
        final Sheet sheet = workbook.createSheet(sheetName);
        final Row headerRow = sheet.createRow(0);
        IntStream.range(0, dayOfWeeks.length).forEach(i -> XmlUtils.setCellValue(dayOfWeeks[i], headerRow.createCell(i + 1)));
        IntStream.range(0, times.length).forEach(i -> XmlUtils.setCellValue(LocalTime.of(i * timeFrame.getSlotTimeH(), 0, 0), sheet.createRow(i + 1).createCell(0)));

        IntStream.range(0, dayOfWeeks.length).forEach(i -> {
          final DayOfWeek week = dayOfWeeks[i];
          IntStream.range(0, times.length).forEach(j -> {
            final int hour = times[j];
            tradesCollection.stream().filter(trade -> trade.getSlotWeek().equals(week) && trade.getSlotStart() == hour).findFirst()
                .ifPresent(trade -> XmlUtils.setCellValue(switch (sheetName) {
                  case "TP" -> trade.getTakeProfitTotal();
                  case "SL" -> trade.getStopLossTotal();
                  case "TOTAL" -> trade.getOrdersTotal();
                  case "PERCENTAGE_TP" -> trade.getHitPercentage().multiply(BigDecimal.valueOf(100d));
                  case "BALANCE" -> trade.getProfitTotal();
                  default -> throw new IllegalStateException("Unexpected value: " + sheetName);
                }, sheet.getRow(j + 1).createCell(i + 1)));
          });
        });
      });

      final Sheet sheet = workbook.createSheet("TRADES");
      final Row headerRow = sheet.createRow(0);
      final String[] header = new String[]{"TIME_START", "TIME_END", "WEEK", "TP", "SL", "PERCENTAGE_TP", "ORDERS_TP", "ORDERS_SL", "ORDERS_TOTAL", "ORDERS_PROFIT"};
      IntStream.range(0, header.length).forEach(i -> headerRow.createCell(i).setCellValue(header[i]));
      csvWriter.writeNext(header);
      final AtomicInteger i = new AtomicInteger(1);
      tradesCollection.forEach(trade -> {
        final Row row = sheet.createRow(i.getAndIncrement());
        IntStream.range(0, header.length).forEach(j -> {
          final Cell cell = row.createCell(j);
          switch (j) {
            case 0 -> XmlUtils.setCellValue(LocalTime.of(trade.getSlotStart() * timeFrame.getSlotTimeH(), 0, 0), cell);
            case 1 -> XmlUtils.setCellValue(LocalTime.of(trade.getSlotStart() * timeFrame.getSlotTimeH(), 0, 0).plusHours(timeFrame.getSlotTimeH()).minusSeconds(1), cell);
            case 2 -> XmlUtils.setCellValue(trade.getSlotWeek(), cell);
            case 3 -> XmlUtils.setCellValue(trade.getTakeProfit(), cell);
            case 4 -> XmlUtils.setCellValue(trade.getStopLoss(), cell);
            case 5 -> XmlUtils.setCellValue(trade.getHitPercentage().multiply(BigDecimal.valueOf(100d)), cell);
            case 6 -> XmlUtils.setCellValue(trade.getTakeProfitTotal(), cell);
            case 7 -> XmlUtils.setCellValue(trade.getStopLossTotal(), cell);
            case 8 -> XmlUtils.setCellValue(trade.getOrdersTotal(), cell);
            case 9 -> XmlUtils.setCellValue(trade.getProfitTotal(), cell);
            default -> throw new IllegalStateException("Unexpected value: " + trade.toString());
          }
        });
        final String[] line = IntStream.range(0, header.length).mapToObj(j -> switch (j) {
          case 0 -> LocalTime.of(trade.getSlotStart() * timeFrame.getSlotTimeH(), 0, 0);
          case 1 -> LocalTime.of(trade.getSlotStart() * timeFrame.getSlotTimeH(), 0, 0).plusHours(timeFrame.getSlotTimeH()).minusSeconds(1);
          case 2 -> trade.getSlotWeek();
          case 3 -> trade.getTakeProfit();
          case 4 -> trade.getStopLoss();
          case 5 -> trade.getHitPercentage().multiply(BigDecimal.valueOf(100d));
          case 6 -> trade.getTakeProfitTotal();
          case 7 -> trade.getStopLossTotal();
          case 8 -> trade.getOrdersTotal();
          case 9 -> trade.getProfitTotal();
          default -> throw new IllegalStateException("Unexpected value: " + trade.toString());
        }).map(Object::toString).toArray(String[]::new);
        csvWriter.writeNext(line);
      });

      workbook.write(new FileOutputStream(new File(outputFolder, symbol.name().concat(timeFrame.name()).concat("_trades.xlsx"))));
    }
    log.info("Trades Excel for symbol {} at timeframe {} printed", symbol.name(), timeFrame.name());
  }
}
