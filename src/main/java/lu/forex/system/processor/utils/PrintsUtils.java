package lu.forex.system.processor.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.services.CandlestickService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Log4j2
@UtilityClass
public class PrintsUtils {

  @SneakyThrows
  public static void printCandlesticksExcel(final @NonNull BufferedReader bufferedReader, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol, final @NonNull File outputFolder) {
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
  public static void printTradesExcel(final @NonNull Collection<Trade> tradesCollection, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol, final @NonNull File outputFolder) {
    log.info("Printing Trades Excel for symbol {} at timeframe {}", symbol.name(), timeFrame.name());
    final DayOfWeek[] dayOfWeeks = Arrays.stream(DayOfWeek.values()).filter(dayOfWeek -> !DayOfWeek.SATURDAY.equals(dayOfWeek) && !DayOfWeek.SUNDAY.equals(dayOfWeek))
        .toArray(DayOfWeek[]::new);
    final int[] times = IntStream.range(0, 24 / timeFrame.getSlotTimeH()).toArray();
    try (final Workbook workbook = new XSSFWorkbook()) {
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
                  case "PERCENTAGE_TP" -> trade.getHitPercentage();
                  case "BALANCE" -> trade.getProfitTotal();
                  default -> throw new IllegalStateException("Unexpected value: " + sheetName);
                }, sheet.getRow(j + 1).createCell(i + 1)));
          });
        });
      });
      workbook.write(new FileOutputStream(new File(outputFolder, symbol.name().concat("_").concat(timeFrame.name()).concat("_trades.xlsx"))));
    }
    log.info("Trades Excel for symbol {} at timeframe {} printed", symbol.name(), timeFrame.name());
  }
}
