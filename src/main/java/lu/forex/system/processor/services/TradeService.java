package lu.forex.system.processor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lu.forex.system.processor.enums.OrderStatus;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.enums.TimeFrame.Frame;
import lu.forex.system.processor.models.Candlestick;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.utils.MathUtils;
import org.apache.commons.lang3.tuple.Pair;

@Log4j2
@UtilityClass
public class TradeService {

  private static final BigDecimal TP_POINTS = BigDecimal.valueOf(100);
  private static final BigDecimal RISK_SL = BigDecimal.valueOf(0.70);
  private static final BigDecimal TP_TARGET = BigDecimal.valueOf(0.60);

//  public static @NonNull Collection<Trade> getTrades(final @NonNull File inputFile, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol, final @NonNull List<Candlestick> candlestickList, final @NonNull Map<TimeScope, List<PreTrade>> timeScopeMapMap) {
//    final List<Trade> tradeList = timeScopeMapMap.entrySet().parallelStream().map(timeScopeMapEntry -> {
//      final TimeScope timeScope = timeScopeMapEntry.getKey();
//      final List<PreTrade> profitListMap = timeScopeMapEntry.getValue();
//      return getTrade(inputFile, symbol, profitListMap, timeScope, getRangerProfit(), timeFrame);
//    }).filter(Objects::nonNull).toList();
//    log.info("Created {} trades from {} symbol at timeframe {}", tradeList.size(), symbol.name(), timeFrame.name());
//    return tradeList;
//  }

  public static @NonNull RangerProfit getRangerProfit() {
    return new RangerProfit(TP_POINTS, TP_POINTS.multiply(RISK_SL).multiply(BigDecimal.valueOf(-1)));
  }

  public static @NonNull Map<TimeScope, List<PreTrade>> getTimeScopeListMap(final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol, final @NonNull List<Candlestick> candlestickList) {
    final Map<TimeScope, List<PreTrade>> timeScopeMapMap = candlestickList.stream()
        .map(candlestick -> new PreTrade(getRangerProfit(), new TimeScope(candlestick.getOpenTickTimestamp().getDayOfWeek(), candlestick.getOpenTickTimestamp().getHour() / timeFrame.getSlotTimeH()), candlestick.getSignalIndicator(), candlestick.getOpenTickTimestamp()))
        .collect(Collectors.groupingBy(PreTrade::getTimeScope));
    log.info("We have {} TimeScope to analise in symbol {} at timeframe {}", timeScopeMapMap.size(), symbol.name(), timeFrame.name());
    return timeScopeMapMap;
  }

  public static @NonNull List<Candlestick> getCandlestickList(final @NonNull File inputFile, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol) {
    log.info("Getting Candlesticks/Trades for symbol {} at timeframe {}", symbol.name(), timeFrame.name());

    try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile))) {
      final Candlestick[] candlestickArray = CandlestickService.getCandlesticks(bufferedReader, timeFrame, symbol)
          .filter(candlestick -> !SignalIndicator.NEUTRAL.equals(candlestick.getSignalIndicator())).toArray(Candlestick[]::new);
      final LinkedList<Candlestick> candlestickList = new LinkedList<>();
      for (int i = 0; i < candlestickArray.length; i++) {
        candlestickList.add(candlestickArray[i]);
        LocalDateTime time = candlestickArray[i].getTimestamp();
        for (int j = i + 1; j < candlestickArray.length; j++, i++) {
          if (Frame.MINUTE.equals(timeFrame.getFrame())) {
            time = time.plusMinutes(timeFrame.getTimeValue());
          } else {
            time = time.plusHours(timeFrame.getTimeValue());
          }
          if (!time.equals(candlestickArray[j].getTimestamp()) || !candlestickArray[i].getSignalIndicator().equals(candlestickArray[j].getSignalIndicator())) {
            break;
          }
        }
      }

      log.info("We have {} candlesticks not neutral in symbol {} at timeframe {}", candlestickList.size(), symbol.name(), timeFrame.name());
      return candlestickList;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Trade getTrade(final @NonNull File inputFile, final @NonNull Symbol symbol, final List<PreTrade> profitListMap, final @NonNull TimeScope timeScope, final RangerProfit rangerProfit, final TimeFrame timeFrame) {

    try (final BufferedReader tickTradeBufferedReader = new BufferedReader(new FileReader(inputFile))) {
      final Tick soptTick = TickService.getTicks(tickTradeBufferedReader).filter(tickTickPair -> processTickProfitAndGetBadPercent(symbol, profitListMap, tickTickPair)).findFirst().map(Pair::getKey).orElse(null);
      if(Objects.nonNull(soptTick)) {
        log.warn("Trade Process {} - {}h from {} symbol at timeframe {} not stop on the END at {}", timeScope.getWeek().toString(), timeScope.hour, symbol.name(), timeFrame, soptTick.getDateTime().toString());
      } else {
        log.info("Trade Process {} - {}h from {} symbol at timeframe {} stop on the END", timeScope.getWeek().toString(), timeScope.hour, symbol.name(), timeFrame);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    final long numberPreTradesTP = profitListMap.stream().filter(preTrade -> OrderStatus.TAKE_PROFIT.equals(preTrade.getOrderStatus())).count();
    final long numberPreTradesSL = profitListMap.stream().filter(preTrade -> OrderStatus.STOP_LOSS.equals(preTrade.getOrderStatus())).count();
    final long numberPreTradesTotal = numberPreTradesTP + numberPreTradesSL;
    final BigDecimal hitPercentage =
        numberPreTradesTotal == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numberPreTradesTP).divide(BigDecimal.valueOf(numberPreTradesTotal), MathUtils.SCALE, MathUtils.ROUNDING_MODE);
    final BigDecimal profitTotal = profitListMap.stream().filter(preTrade -> !OrderStatus.OPEN.equals(preTrade.getOrderStatus())).map(PreTrade::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

    if (hitPercentage.compareTo(TP_TARGET) >= 0) {
      return new Trade(rangerProfit.getStopLoss().intValue(), rangerProfit.getTakeProfit().intValue(), timeScope.getWeek(), timeScope.getHour(), numberPreTradesTotal, numberPreTradesTP,
          numberPreTradesSL, hitPercentage, profitTotal);
    } else {
      return null;
    }
  }

  private static boolean processTickProfitAndGetBadPercent(final Symbol symbol, final @NonNull List<PreTrade> profitListMap, final Pair<Tick, Tick> tickTickPair) {
    final Collection<OrderStatus> orderStatuses = profitListMap.stream().map(preTrade -> {
      if(preTrade.getOrderStatus().equals(OrderStatus.OPEN)) {
        final Tick currentTick = tickTickPair.getKey();
        if (currentTick.getDateTime().isAfter(preTrade.getOpenTickTimestamp())) {
          final Tick lastTick = tickTickPair.getValue();
          final BigDecimal tmpProfit = preTrade.getSignalIndicator().getOrderType().getProfit(lastTick, currentTick, symbol);
          preTrade.setProfit(preTrade.getProfit().add(tmpProfit));
        } else if (currentTick.getDateTime().isEqual(preTrade.getOpenTickTimestamp())) {
          preTrade.setProfit(currentTick.getSpread());
        }
      }
      return preTrade.getOrderStatus();
    }).toList();
    final long goodNum = orderStatuses.stream().filter(orderStatus -> orderStatus.equals(OrderStatus.OPEN) || orderStatus.equals(OrderStatus.TAKE_PROFIT)).count();
    return MathUtils.getDivision(BigDecimal.valueOf(goodNum), BigDecimal.valueOf(orderStatuses.size())).compareTo(TP_TARGET) < 0;
  }

  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor
  public class RangerProfit {

    private final BigDecimal takeProfit;
    private final BigDecimal stopLoss;
  }

  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor
  public class TimeScope {

    private final DayOfWeek week;
    private final int hour;
  }

  @Getter
  @Setter
  @RequiredArgsConstructor
  public class PreTrade {

    private final RangerProfit rangerProfit;
    private final TimeScope timeScope;
    private final SignalIndicator signalIndicator;
    private final LocalDateTime openTickTimestamp;
    private BigDecimal profit = BigDecimal.ZERO;

    public OrderStatus getOrderStatus() {
      if (this.getProfit().compareTo(this.getRangerProfit().getTakeProfit()) >= 0) {
        return OrderStatus.TAKE_PROFIT;
      } else if (this.getProfit().compareTo(this.getRangerProfit().getStopLoss()) <= 0) {
        return OrderStatus.STOP_LOSS;
      } else {
        return OrderStatus.OPEN;
      }
    }
  }

}
