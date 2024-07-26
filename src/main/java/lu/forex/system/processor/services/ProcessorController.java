package lu.forex.system.processor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lu.forex.system.processor.enums.Indicator;
import lu.forex.system.processor.enums.OrderStatus;
import lu.forex.system.processor.enums.OrderType;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.TechnicalIndicator;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.models.technicalindicator.AverageDirectionalIndex;
import lu.forex.system.processor.models.technicalindicator.RelativeStrengthIndex;
import lu.forex.system.processor.utils.TimeFrameUtils;
import lu.forex.system.processor.utils.XmlUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Log4j2
public class ProcessorController {

  private static final Pair<Integer, Integer> RANGER_TP_SL = Pair.of(50, 500);
  private static final int SKIP_RANGER_TP = 5;
  private static final BigDecimal RISK_SL = BigDecimal.valueOf(1.5);
  private static final BigDecimal TP_TARGET = BigDecimal.valueOf(0.60);

  public static void main(String[] args) {

    final File rootFolder = new File("C:\\Users\\AllanDeMirandaSilva\\Downloads\\processing");
    final File inputFolder = new File(rootFolder, "inputMt5");
    final File outputFolder = new File(rootFolder, "outputMt5");

    Arrays.stream(Objects.requireNonNull(inputFolder.listFiles())).parallel().forEach(inputFile -> start(inputFile, outputFolder));

  }

  /**
   * @param inputFile    Input file from mt5 with the ticks
   * @param outputFolder The output folder
   */
  public static void start(final @NonNull File inputFile, final @NonNull File outputFolder) {
    final Symbol symbol = Symbol.valueOf(inputFile.getName().split("_")[0]);
    log.info("Starting processor for {} using file", symbol.name(), inputFile.getName());

    final Pair<Integer, Integer>[] ranger = IntStream.rangeClosed(0, (RANGER_TP_SL.getValue() - RANGER_TP_SL.getKey()) / SKIP_RANGER_TP).mapToObj(i -> {
      final int power = (SKIP_RANGER_TP * i);
      final int key = power + RANGER_TP_SL.getKey();
      final int value = BigDecimal.valueOf(-key).multiply(RISK_SL).intValue();
      return Pair.of(key, value);
    }).map(p -> Pair.of(p.getKey(), p.getValue())).toArray(Pair[]::new);

    Arrays.stream(TimeFrame.values()).parallel().forEach(timeFrame -> {
      final ArrayList<Candlestick> candlesticksNotNeutral = getCandlesticksNotNeutral(inputFile, timeFrame);
      log.info("Generating orders for {} from {} candlesticks not neutral {} timeframe", symbol.name(), candlesticksNotNeutral.size(), timeFrame.name());
      final Trade[] trades = getTrades(candlesticksNotNeutral, inputFile, ranger, timeFrame, symbol);
      if (trades.length != 0) {
        printTradePerformance(candlesticksNotNeutral, trades, inputFile, outputFolder, timeFrame, symbol);
      } else {
        log.warn("No trades found for {} on timeframe {}", symbol.name(), timeFrame.name());
      }
    });

    log.warn("Symbol {} processed end", symbol);
  }

  private static void printTradePerformance(final @NonNull List<Candlestick> candlesticksNotNeutral, final @NonNull Trade @NonNull [] trades, final @NonNull File inputFile,
      final @NonNull File outputFolder, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol) {
    final String symbolName = symbol.name();
    log.info("Generating trade file for {} from {} trades {} timeframe", symbolName, trades.length, timeFrame.name());

    final DayOfWeek[] dayOfWeeks = Arrays.stream(DayOfWeek.values()).filter(dayOfWeek -> !DayOfWeek.SATURDAY.equals(dayOfWeek) && !DayOfWeek.SUNDAY.equals(dayOfWeek))
        .toArray(DayOfWeek[]::new);
    final LocalTime[] times = IntStream.range(0, BigDecimal.valueOf(24.0 / timeFrame.getSlotTimeH()).intValue()).mapToObj(value -> LocalTime.of(value * timeFrame.getSlotTimeH(), 0, 0))
        .toArray(LocalTime[]::new);

    final Map<Trade, Map<OrderStatus, List<BigDecimal>>> endCollection = Arrays.stream(trades).map(trade -> {
      final Map<OrderStatus, List<BigDecimal>> collectionMap = candlesticksNotNeutral.parallelStream().filter(
          candlestick -> trade.getSlotWeek().equals(candlestick.getTimestamp().getDayOfWeek()) && !trade.getSlotStart().isAfter(candlestick.getTimestamp().toLocalTime())
              && !trade.getSlotEnd().isBefore(candlestick.getTimestamp().toLocalTime())).map(candlestick -> {

        OrderStatus orderStatus = OrderStatus.OPEN;
        BigDecimal profit = BigDecimal.ZERO;

        try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
          final Tick currentTick = Tick.builder().dateTime(LocalDateTime.MIN).bid(BigDecimal.ZERO).ask(BigDecimal.ZERO).build();
          final OrderType orderType = candlestick.getSignalIndicator().getOrderType();

          final BigDecimal val = BigDecimal.valueOf(trade.getTakeProfit());
          final BigDecimal val1 = BigDecimal.valueOf(trade.getStopLoss());
          String lineReader;
          Tick lastTick = null;

          while (Objects.nonNull(lineReader = bufferedReader.readLine())) {
            if (!lineReader.startsWith("<DATE>")) {
              final Tick lineTick = getDataTick(lineReader);
              if (lineTick.getDateTime().isAfter(currentTick.getDateTime())) {
                currentTick.setDateTime(lineTick.getDateTime());

                if (lineTick.getBid().compareTo(BigDecimal.ZERO) > 0) {
                  currentTick.setBid(lineTick.getBid());
                }
                if (lineTick.getAsk().compareTo(BigDecimal.ZERO) > 0) {
                  currentTick.setAsk(lineTick.getAsk());
                }

                if (currentTick.getBid().compareTo(BigDecimal.ZERO) > 0 && currentTick.getAsk().compareTo(BigDecimal.ZERO) > 0) {
                  if(currentTick.getDateTime().isAfter(candlestick.getCalculatedTickTime())) {

                    assert lastTick != null;
                    profit = profit.add(orderType.getProfit(lastTick, currentTick, symbol));

                    if (profit.compareTo(val) >= 0) {
                      orderStatus = OrderStatus.TAKE_PROFIT;
                      break;
                    } else if (profit.compareTo(val1) <= 0) {
                      orderStatus = OrderStatus.STOP_LOSS;
                      break;
                    }

                  } else if (candlestick.getCalculatedTickTime().isEqual(currentTick.getDateTime())) {
                    profit = profit.add(currentTick.getSpread());
                    if (profit.compareTo(val1) <= 0) {
                      orderStatus = OrderStatus.STOP_LOSS;
                      break;
                    }
                  }

                  lastTick = Tick.builder().dateTime(currentTick.getDateTime()).bid(currentTick.getBid()).ask(currentTick.getAsk()).build();
                }
              }
            }
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        if (orderStatus == OrderStatus.OPEN) {
          log.error("verificar porque tem uma ordem no candlestick {} e trade {} que esta terminando OPEN -> prof {}", candlestick.getSignalIndicator(), trade, profit);
          profit = BigDecimal.ZERO;
        }

        return new SimpleEntry<>(orderStatus, profit);
      }).collect(Collectors.groupingBy(SimpleEntry::getKey, Collectors.collectingAndThen(Collectors.toList(), list -> list.stream().map(SimpleEntry::getValue).toList())));
      return new SimpleEntry<>(trade, collectionMap);
    }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

    try (final Workbook workbook = new XSSFWorkbook()) {
      List.of("TP", "PERCENTAGE_TP", "SL", "TOTAL", "BALANCE").forEach(sheetName -> {
        final Sheet sheet = workbook.createSheet(sheetName);
        final Row headerRow = sheet.createRow(0);
        IntStream.range(0, dayOfWeeks.length).forEach(i -> headerRow.createCell(i + 1).setCellValue(dayOfWeeks[i].toString()));
        IntStream.range(0, times.length).forEach(i -> sheet.createRow(i + 1).createCell(0).setCellValue(times[i].toString()));

        IntStream.range(0, dayOfWeeks.length).forEach(i -> {
          final DayOfWeek week = dayOfWeeks[i];
          IntStream.range(0, times.length).forEach(j -> {
            final LocalTime time = times[j];
            endCollection.entrySet().stream().filter(tradeMapEntry -> tradeMapEntry.getKey().getSlotWeek().equals(week) && tradeMapEntry.getKey().getSlotStart().equals(time))
                .forEach(tradeMapEntry -> {
                  switch (sheetName) {
                    case "TP" -> XmlUtils.setCellValue(
                        tradeMapEntry.getValue().entrySet().stream().filter(entry -> OrderStatus.TAKE_PROFIT.equals(entry.getKey())).mapToInt(entry -> entry.getValue().size()).sum(),
                        sheet.getRow(j + 1).createCell(i + 1));
                    case "PERCENTAGE_TP" -> {
                      final double tp = tradeMapEntry.getValue().entrySet().stream().filter(entry -> OrderStatus.TAKE_PROFIT.equals(entry.getKey()))
                          .mapToInt(entry -> entry.getValue().size()).sum();
                      final double total = tradeMapEntry.getValue().entrySet().stream().filter(entry -> !OrderStatus.OPEN.equals(entry.getKey())).mapToInt(entry -> entry.getValue().size())
                          .sum();
                      XmlUtils.setCellValue((tp / total) * 100.0, sheet.getRow(j + 1).createCell(i + 1));
                    }
                    case "SL" -> XmlUtils.setCellValue(
                        tradeMapEntry.getValue().entrySet().stream().filter(entry -> OrderStatus.STOP_LOSS.equals(entry.getKey())).mapToInt(entry -> entry.getValue().size()).sum(),
                        sheet.getRow(j + 1).createCell(i + 1));
                    case "TOTAL" -> XmlUtils.setCellValue(
                        tradeMapEntry.getValue().entrySet().stream().filter(entry -> !OrderStatus.OPEN.equals(entry.getKey())).mapToInt(entry -> entry.getValue().size()).sum(),
                        sheet.getRow(j + 1).createCell(i + 1));
                    case "BALANCE" -> XmlUtils.setCellValue(
                        tradeMapEntry.getValue().entrySet().stream().filter(entry -> !OrderStatus.OPEN.equals(entry.getKey())).flatMap(entry -> entry.getValue().stream())
                            .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue(), sheet.getRow(j + 1).createCell(i + 1));
                    default -> XmlUtils.setCellValue(0, sheet.getRow(j + 1).createCell(i + 1));
                  }
                });
          });
        });
      });

      final Sheet sheet = workbook.createSheet("TRADES");
      final Row headerRow = sheet.createRow(0);
      final String[] header = new String[]{"week", "start", "tp", "sl"};
      IntStream.range(0, header.length).forEach(i -> headerRow.createCell(i).setCellValue(header[i]));
      IntStream.range(0, trades.length).forEach(i -> {
        final Row row = sheet.createRow(i + 1);
        row.createCell(0).setCellValue(trades[i].getSlotWeek().toString());
        row.createCell(1).setCellValue(trades[i].getSlotStart().toString());
        row.createCell(2).setCellValue(trades[i].getTakeProfit());
        row.createCell(3).setCellValue(trades[i].getStopLoss());
      });

      final var fileXlsx = new File(outputFolder, symbolName.concat("_").concat(timeFrame.name()).concat("_trades.xlsx"));
      workbook.write(new FileOutputStream(fileXlsx));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static @NonNull Trade @NonNull [] getTrades(final @NonNull ArrayList<Candlestick> candlesticksNotNeutral, final @NonNull File inputFile, final Pair<Integer, Integer>[] ranger,
      final TimeFrame timeFrame, final Symbol symbol) {
    log.info("Generating Trades for {} at timeframe {}", symbol.name(), timeFrame.name());
    return IntStream.range(0, candlesticksNotNeutral.size()).boxed().collect(Collectors.groupingBy(o -> Pair.of(candlesticksNotNeutral.get(o).getTimestamp().getDayOfWeek(),
        BigDecimal.valueOf(candlesticksNotNeutral.get(o).getTimestamp().getHour() / timeFrame.getSlotTimeH()).intValue()))).entrySet().parallelStream().map(entry -> {
      final Map<Integer, List<BigDecimal>> ordersMap = entry.getValue().parallelStream().flatMap(c -> {

        final Candlestick candlestick = candlesticksNotNeutral.get(c);
        final OrderType orderType = candlestick.getSignalIndicator().getOrderType();

        final Map<Integer, BigDecimal> indexRByProfit = new HashMap<>();

        try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
          final Tick currentTick = Tick.builder().dateTime(LocalDateTime.MIN).bid(BigDecimal.ZERO).ask(BigDecimal.ZERO).build();
          BigDecimal profit = BigDecimal.ZERO;
          int r = 0;
          String lineReader;
          Tick lastTick = null;
          while (Objects.nonNull(lineReader = bufferedReader.readLine())) {
            if (!lineReader.startsWith("<DATE>")) {
              final Tick lineTick = getDataTick(lineReader);
              if (lineTick.getDateTime().isAfter(currentTick.getDateTime())) {
                currentTick.setDateTime(lineTick.getDateTime());

                if (lineTick.getBid().compareTo(BigDecimal.ZERO) > 0) {
                  currentTick.setBid(lineTick.getBid());
                }
                if (lineTick.getAsk().compareTo(BigDecimal.ZERO) > 0) {
                  currentTick.setAsk(lineTick.getAsk());
                }

                if (currentTick.getBid().compareTo(BigDecimal.ZERO) > 0 && currentTick.getAsk().compareTo(BigDecimal.ZERO) > 0) {
                  if(currentTick.getDateTime().isAfter(candlestick.getCalculatedTickTime())) {
                    assert lastTick != null;
                    profit = profit.add(orderType.getProfit(lastTick, currentTick, symbol));

                    if (profit.compareTo(BigDecimal.valueOf(ranger[r].getKey())) >= 0) {
                      indexRByProfit.put(r++, profit);
                    } else if (profit.compareTo(BigDecimal.valueOf(ranger[r].getValue())) <= 0) {
                      while (++r < ranger.length) {
                        if (profit.compareTo(BigDecimal.valueOf(ranger[r].getKey())) >= 0) {
                          indexRByProfit.put(r++, profit);
                        } else if (profit.compareTo(BigDecimal.valueOf(ranger[r].getValue())) > 0) {
                          break;
                        }
                      }
                    }
                  } else if (candlestick.getCalculatedTickTime().isEqual(currentTick.getDateTime())) {
                    profit = profit.add(currentTick.getSpread());
                    if (profit.compareTo(BigDecimal.valueOf(ranger[r].getValue())) <= 0) {
                      while (++r < ranger.length) {
                        if (profit.compareTo(BigDecimal.valueOf(ranger[r].getValue())) > 0) {
                          break;
                        }
                      }
                    }
                  }

                  if(r >= ranger.length) {
                    break;
                  }
                  lastTick = Tick.builder().dateTime(currentTick.getDateTime()).bid(currentTick.getBid()).ask(currentTick.getAsk()).build();
                }
              }
            }
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        return indexRByProfit.entrySet().stream();
      }).collect(Collectors.groupingBy(Entry::getKey, Collectors.collectingAndThen(Collectors.toList(), list -> list.stream().map(Entry::getValue).toList())));

      final BigDecimal taxTpTarget = BigDecimal.valueOf(entry.getValue().size()).multiply(TP_TARGET);

      final Entry<Integer, List<BigDecimal>> tmp = ordersMap.entrySet().stream()
          .filter(integerListEntry -> BigDecimal.valueOf(integerListEntry.getValue().size()).compareTo(taxTpTarget) >= 0).min(Comparator.comparing(Entry::getKey)).orElse(null);

      if (Objects.isNull(tmp)) {
        return null;
      } else {
        return Trade.builder().stopLoss(ranger[tmp.getKey()].getValue()).takeProfit(ranger[tmp.getKey()].getKey()).slotWeek(entry.getKey().getKey())
            .slotStart(LocalTime.of(entry.getKey().getValue() * timeFrame.getSlotTimeH(), 0, 0))
            .slotEnd(LocalTime.of(entry.getKey().getValue() * timeFrame.getSlotTimeH() + timeFrame.getSlotTimeH() - 1, 59, 59)).build();
      }
    }).filter(Objects::nonNull).toArray(Trade[]::new);
  }

  /**
   * @param inputFile Input file from mt5 with the ticks
   * @return The collection from timeframe by not neutral candlesticks list
   */
  private static @NonNull ArrayList<Candlestick> getCandlesticksNotNeutral(final @NonNull File inputFile, final @NonNull TimeFrame timeFrame) {
    final ArrayList<Candlestick> candlesticksNotNeutral = new ArrayList<Candlestick>();

    try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
      final Tick currentTick = Tick.builder().dateTime(LocalDateTime.MIN).bid(BigDecimal.ZERO).ask(BigDecimal.ZERO).build();

      final LinkedList<Tick> ticksForCandlesticks = new LinkedList<Tick>();
      final LinkedList<Candlestick> candlesticksBuffer = new LinkedList<Candlestick>();
      final int sizeCandlesticksBuffer = getNewTechnicalIndicators().stream().mapToInt(TechnicalIndicator::getNumberOfCandlesticksToCalculate).max().orElseThrow();

      String lineReader;
      Tick lastTick = null;
      while (Objects.nonNull(lineReader = bufferedReader.readLine())) {
        if (!lineReader.startsWith("<DATE>")) {
          final Tick lineTick = getDataTick(lineReader);
          if (lineTick.getDateTime().isAfter(currentTick.getDateTime())) {
            currentTick.setDateTime(lineTick.getDateTime());

            if (lineTick.getBid().compareTo(BigDecimal.ZERO) > 0) {
              currentTick.setBid(lineTick.getBid());
            }
            if (lineTick.getAsk().compareTo(BigDecimal.ZERO) > 0) {
              currentTick.setAsk(lineTick.getAsk());
            }

            if (currentTick.getBid().compareTo(BigDecimal.ZERO) > 0 && currentTick.getAsk().compareTo(BigDecimal.ZERO) > 0) {
              final Tick tmpLastTick = Objects.isNull(lastTick) ? currentTick : lastTick;
              //

              final LocalDateTime lastCandlestickTimestamp = TimeFrameUtils.getCandlestickTimestamp(tmpLastTick.getDateTime(), timeFrame);
              final LocalDateTime candlestickTimestamp = TimeFrameUtils.getCandlestickTimestamp(currentTick.getDateTime(), timeFrame);
              if (candlestickTimestamp.equals(lastCandlestickTimestamp)) {
                ticksForCandlesticks.add(Tick.builder().dateTime(currentTick.getDateTime()).bid(currentTick.getBid()).ask(currentTick.getAsk()).build());
              } else {

                final LocalDateTime calculatedTickTime = currentTick.getDateTime();
                final BigDecimal high = ticksForCandlesticks.stream().map(Tick::getBid).reduce(BigDecimal::max).orElseThrow();
                final BigDecimal low = ticksForCandlesticks.stream().map(Tick::getBid).reduce(BigDecimal::min).orElseThrow();
                final BigDecimal open = ticksForCandlesticks.getFirst().getBid();
                final BigDecimal close = ticksForCandlesticks.getLast().getBid();

                ticksForCandlesticks.clear();
                ticksForCandlesticks.add(Tick.builder().dateTime(currentTick.getDateTime()).bid(currentTick.getBid()).ask(currentTick.getAsk()).build());

                final Set<TechnicalIndicator> technicalIndicators = getNewTechnicalIndicators();
                final Candlestick newCandlestick = Candlestick.builder().timestamp(lastCandlestickTimestamp).calculatedTickTime(calculatedTickTime).high(high).low(low).open(open).close(close)
                    .technicalIndicators(technicalIndicators).build();

                candlesticksBuffer.addFirst(newCandlestick);

                if (candlesticksBuffer.size() > sizeCandlesticksBuffer) {
                  candlesticksBuffer.removeLast();
                }

                newCandlestick.getTechnicalIndicators().parallelStream().forEach(technicalIndicator -> technicalIndicator.calculate(candlesticksBuffer.toArray(Candlestick[]::new)));
                if (!SignalIndicator.NEUTRAL.equals(newCandlestick.getSignalIndicator())) {
                  if (candlesticksBuffer.size() > 1) {
                    if (!newCandlestick.getSignalIndicator().equals(candlesticksBuffer.get(1).getSignalIndicator())) {
                      candlesticksNotNeutral.add(newCandlestick);
                    }
                  } else {
                    candlesticksNotNeutral.add(newCandlestick);
                  }
                }
              }

              //
              lastTick = Tick.builder().dateTime(currentTick.getDateTime()).bid(currentTick.getBid()).ask(currentTick.getAsk()).build();
            }
          }
        }

      }
    } catch (IOException e) {
      log.error("Can't read file {} to make the candlesticks", inputFile.getName(), e);
    }

    return candlesticksNotNeutral;

  }

  @SneakyThrows
  private static void printDebug(Candlestick[] candlesticks, File debugFile) {
    try (final Workbook workbook = new XSSFWorkbook()) {
      final Sheet sheet = workbook.createSheet("symbol");
      final Row headerRow = sheet.createRow(0);
      final String[] header = new String[]{"Timestamp", "Open", "High", "Low", "Close", "ADX_adx", "ADX_+di(P)", "ADX_-di(P)", "ADX_tr1", "ADX_+dm1", "ADX_-dm1", "ADX_dx",
          "ADX_signalIndicator", "RSI_gain", "RSI_loss", "RSI_averageGain", "RSI_averageLoss", "RSI_rsi", "RSI_signalIndicator", "Candlestick_signalIndicator"};
      IntStream.range(0, header.length).forEach(i -> headerRow.createCell(i).setCellValue(header[i]));

      IntStream.range(0, candlesticks.length).forEach(i -> {
        final Row row = sheet.createRow(i + 1);
        IntStream.range(0, header.length).forEach(j -> {
          final Cell cell = row.createCell(j);
          switch (j) {
            case 0 -> XmlUtils.setCellValue(candlesticks[i].getTimestamp().toString().replace("T", " ").split("\\.")[0], cell);
            case 1 -> XmlUtils.setCellValue(candlesticks[i].getOpen().doubleValue(), cell);
            case 2 -> XmlUtils.setCellValue(candlesticks[i].getHigh().doubleValue(), cell);
            case 3 -> XmlUtils.setCellValue(candlesticks[i].getLow().doubleValue(), cell);
            case 4 -> XmlUtils.setCellValue(candlesticks[i].getClose().doubleValue(), cell);
            case 5 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("adx");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 6 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("+di(P)");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 7 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("-di(P)");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 8 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("tr1");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 9 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("+dm1");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 10 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("-dm1");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 11 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("dx");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 12 -> XmlUtils.setCellValue(
                candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.ADX)).findFirst()
                    .orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getSignal().name(), cell);
            case 13 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.RSI))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("gain");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 14 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.RSI))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("loss");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 15 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.RSI))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("averageGain");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 16 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.RSI))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("averageLoss");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 17 -> {
              final BigDecimal bigDecimal = candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.RSI))
                  .findFirst().orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getData().get("rsi");
              if (Objects.nonNull(bigDecimal)) {
                XmlUtils.setCellValue(bigDecimal.doubleValue(), cell);
              }
            }
            case 18 -> XmlUtils.setCellValue(
                candlesticks[i].getTechnicalIndicators().stream().filter(technicalIndicator -> technicalIndicator.getIndicator().equals(Indicator.RSI)).findFirst()
                    .orElse(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build()).getSignal().name(), cell);
            case 19 -> XmlUtils.setCellValue(candlesticks[i].getSignalIndicator().name(), cell);
          }
        });
      });
      workbook.write(new FileOutputStream(debugFile));
    }
  }

  /**
   * @return The collection of new technical indicators
   */
  private static @NonNull Set<@NonNull TechnicalIndicator> getNewTechnicalIndicators() {
    return Set.of(AverageDirectionalIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build(),
        RelativeStrengthIndex.builder().data(new HashMap<>()).signal(SignalIndicator.NEUTRAL).build());
  }

  /**
   * @param line Line from the file historic ticks on MT5
   * @return The timestamp, bid, and ask value from the tick
   */
  private static @NonNull Tick getDataTick(final @NonNull String line) {
    final String[] parts = line.split("\t");
    final String date = parts[0].replace(".", "-");
    final String time = parts[1];
    final String dataTime = date.concat("T").concat(time);
    final LocalDateTime localDateTime = LocalDateTime.parse(dataTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    final BigDecimal bid = parts[2].isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(parts[2]));
    final BigDecimal ask = parts[3].isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(parts[3]));
    return Tick.builder().dateTime(localDateTime).bid(bid).ask(ask).build();
  }

}
