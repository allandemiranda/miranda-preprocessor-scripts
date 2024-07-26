package lu.forex.system.processor.repositories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lu.forex.system.processor.enums.Indicator;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.technicalindicator.AverageDirectionalIndex;
import lu.forex.system.processor.utils.XmlUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@RequiredArgsConstructor
@Getter(AccessLevel.PRIVATE)
public class CandlestickRepository implements Serializable {

  @Serial
  private static final long serialVersionUID = -646819376171934548L;
  private final int maxSize;
  //  @Setter(AccessLevel.PRIVATE)
  private Node initNode = null;
  @Setter(AccessLevel.PRIVATE)
  private int size = 0;

  @Deprecated
  public CandlestickRepository(final int maxSize, final File debugFile) {
    this.maxSize = maxSize;
    this.debugFile = debugFile;
  }

  @RequiredArgsConstructor
  @Getter
  @Setter
  private static class Node implements Serializable {

    @Serial
    private static final long serialVersionUID = -364757894664929899L;

    private Node next = null;
    @NonNull
    private Candlestick candlestick;
  }

  public void add(final @NonNull Candlestick candlestick) {
    final Node newNode = new Node(candlestick);
    if (Objects.isNull(this.getInitNode())) {
      this.setInitNode(newNode);
      this.setSize(this.getSize() + 1);
    } else {
      Node node = this.getInitNode();
      while (Objects.nonNull(node.getNext())) {
        node = node.getNext();
      }
      node.setNext(newNode);
      this.setSize(this.getSize() + 1);
      if(this.getSize() > this.getMaxSize()) {
        this.setInitNode(this.getInitNode().getNext());
      }
    }
  }

  public Candlestick getCurrentCandlestick() {
    if(Objects.isNull(this.getInitNode())) {
      return null;
    } else {
      Node node = this.getInitNode();
      while (Objects.nonNull(node.getNext())) {
        node = node.getNext();
      }
      return node.getCandlestick();
    }
  }

  public @NonNull Candlestick @NonNull [] getCandlesticksDes() {
    final Candlestick[] candlesticks = new Candlestick[this.getSize()];
    Node node = this.getInitNode();
    for (int i = candlesticks.length - 1; i >= 0; i--) {
      candlesticks[i] = node.getCandlestick();
      node = node.getNext();
    }
    return candlesticks;
  }

  @Deprecated
  private final ArrayList<Candlestick> candlesticksDebug = new ArrayList<>();
  private File debugFile;

  // !!! REMOVE
  private void setInitNode(final @NonNull Node node) {
    initNode = node;
    if (candlesticksDebug.size() < 100) {
      candlesticksDebug.add(node.getCandlestick());
      if (candlesticksDebug.size() == 100) {
        this.printDebug();
      }
    }
  }

  // !!! REMOVE
  @SneakyThrows
  private void printDebug() {
    try (final Workbook workbook = new XSSFWorkbook()) {
      final Candlestick[] candlesticks = candlesticksDebug.toArray(Candlestick[]::new);
      final Sheet sheet = workbook.createSheet("symbol");
      final Row headerRow = sheet.createRow(0);
      final String[] header = new String[]{"Timestamp", "Open", "High", "Low", "Close", "ADX_adx", "ADX_+di(P)", "ADX_-di(P)", "ADX_tr1", "ADX_+dm1", "ADX_-dm1", "ADX_dx",
          "ADX_signalIndicator", "RSI_gain", "RSI_loss", "RSI_averageGain", "RSI_averageLoss", "RSI_rsi", "RSI_signalIndicator", "Candlestick_signalIndicator"};
      IntStream.range(0, header.length).forEach(i -> headerRow.createCell(i).setCellValue(header[i]));

      IntStream.range(0, candlesticks.length).map(i -> -i).sorted().map(i -> -i).forEach(i -> {
        final Row row = sheet.createRow(candlesticks.length - i);
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
            case 19 -> XmlUtils.setCellValue(candlesticks[i].getSignalIndicator(), cell);
          }
        });
      });
      workbook.write(new FileOutputStream(debugFile));
    }
  }
}
