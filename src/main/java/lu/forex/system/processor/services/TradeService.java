package lu.forex.system.processor.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

  private static final Pair<Integer, Integer> RANGER_TP_SL = Pair.of(100, 150);
  private static final int SKIP_RANGER_TP = 5;
  private static final BigDecimal RISK_SL = BigDecimal.valueOf(0.8);
  private static final BigDecimal TP_TARGET = BigDecimal.valueOf(0.60);

  public static @NonNull Collection<Trade> getTrades(final @NonNull File inputFile, final @NonNull BufferedReader bufferedReader, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol) {
    log.info("Getting Trades for symbol {} at timeframe {}", symbol.name(), timeFrame.name());

    final Collection<RangerProfit> rangerProfitCollection = IntStream.rangeClosed(0, (RANGER_TP_SL.getValue() - RANGER_TP_SL.getKey()) / SKIP_RANGER_TP).mapToObj(i -> {
      final int power = (SKIP_RANGER_TP * i);
      final int tp = power + RANGER_TP_SL.getKey();
      final int sl = BigDecimal.valueOf(-tp).multiply(RISK_SL).intValue();
      return new RangerProfit(BigDecimal.valueOf(tp), BigDecimal.valueOf(sl));
    }).collect(Collectors.toSet());

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

    final Map<TimeScope, Map<RangerProfit, List<PreTrade>>> timeScopeMapMap = candlestickList.parallelStream().flatMap(candlestick -> rangerProfitCollection.parallelStream().map(
        rangerProfit -> new PreTrade(rangerProfit, new TimeScope(candlestick.getOpenTickTimestamp().getDayOfWeek(), candlestick.getOpenTickTimestamp().getHour() / timeFrame.getSlotTimeH()),
            candlestick.getSignalIndicator(), candlestick.getOpenTickTimestamp()))).collect(Collectors.groupingBy(PreTrade::getTimeScope, Collectors.groupingBy(PreTrade::getRangerProfit)));
    log.info("We have {} pre trades to analise in symbol {} at timeframe {}", timeScopeMapMap.values().stream().mapToInt(m -> m.values().size()).sum(), symbol.name(), timeFrame.name());

    final List<Trade> tradeList = timeScopeMapMap.entrySet().parallelStream().map(timeScopeMapEntry -> {
      final TimeScope timeScope = timeScopeMapEntry.getKey();

      Trade trade = null;

      for (final var rangerProfitListEntry : timeScopeMapEntry.getValue().entrySet().stream().sorted(Comparator.comparing(rangerProfitListEntry -> rangerProfitListEntry.getKey().getTakeProfit())).toList()) {
        final RangerProfit rangerProfit = rangerProfitListEntry.getKey();

        try (final BufferedReader tickTradeBufferedReader = new BufferedReader(new FileReader(inputFile))) {
          TickService.getTicks(tickTradeBufferedReader).forEach(tickTickPair -> rangerProfitListEntry.getValue().parallelStream().forEach(preTrade -> {
            if (preTrade.getOrderStatus().equals(OrderStatus.OPEN)) {
              final Tick currentTick = tickTickPair.getKey();
              if (currentTick.getDateTime().isAfter(preTrade.getOpenTickTimestamp())) {
                final Tick lastTick = tickTickPair.getValue();
                final BigDecimal tmpProfit = preTrade.getSignalIndicator().getOrderType().getProfit(lastTick, currentTick, symbol);
                preTrade.setProfit(preTrade.getProfit().add(tmpProfit));
              } else if (currentTick.getDateTime().isEqual(preTrade.getOpenTickTimestamp())) {
                preTrade.setProfit(currentTick.getSpread());
              }
            }
          }));
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }

        final long numberPreTradesTP = rangerProfitListEntry.getValue().stream().filter(preTrade -> OrderStatus.TAKE_PROFIT.equals(preTrade.getOrderStatus())).count();
        final long numberPreTradesSL = rangerProfitListEntry.getValue().stream().filter(preTrade -> OrderStatus.STOP_LOSS.equals(preTrade.getOrderStatus())).count();
        final long numberPreTradesTotal = numberPreTradesTP + numberPreTradesSL;
        final BigDecimal hitPercentage =
            numberPreTradesTotal == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numberPreTradesTP).divide(BigDecimal.valueOf(numberPreTradesTotal), MathUtils.SCALE, MathUtils.ROUNDING_MODE);
        final BigDecimal profitTotal = rangerProfitListEntry.getValue().stream().filter(preTrade -> !OrderStatus.OPEN.equals(preTrade.getOrderStatus())).map(PreTrade::getProfit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (hitPercentage.compareTo(TP_TARGET) >= 0) {
          trade = new Trade(rangerProfit.getStopLoss().intValue(), rangerProfit.getTakeProfit().intValue(), timeScope.getWeek(), timeScope.getHour(), numberPreTradesTotal, numberPreTradesTP,
              numberPreTradesSL, hitPercentage, profitTotal);
          break;
        }
      }

      return trade;
    }).filter(Objects::nonNull).toList();
    log.info("Created {} trades from {} symbol at timeframe {}", tradeList.size(), symbol.name(), timeFrame.name());
    return tradeList;
  }

  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor
  private class RangerProfit {

    private final BigDecimal takeProfit;
    private final BigDecimal stopLoss;
  }

  @Getter
  @EqualsAndHashCode
  @RequiredArgsConstructor
  private class TimeScope {

    private final DayOfWeek week;
    private final int hour;
  }

  @Getter
  @Setter
  @RequiredArgsConstructor
  private class PreTrade {

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
