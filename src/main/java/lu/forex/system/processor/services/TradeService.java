package lu.forex.system.processor.services;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lu.forex.system.processor.enums.OrderStatus;
import lu.forex.system.processor.enums.SignalIndicator;
import lu.forex.system.processor.enums.Symbol;
import lu.forex.system.processor.enums.TimeFrame;
import lu.forex.system.processor.models.Tick;
import lu.forex.system.processor.models.Trade;
import lu.forex.system.processor.utils.MathUtils;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class TradeService {

  private static final Pair<Integer, Integer> RANGER_TP_SL = Pair.of(100, 500);
  private static final int SKIP_RANGER_TP = 5;
  private static final BigDecimal RISK_SL = BigDecimal.valueOf(0.8);
  private static final BigDecimal TP_TARGET = BigDecimal.valueOf(0.60);

  public static Stream<Trade> getTrades(final @NonNull BufferedReader bufferedReader, final @NonNull TimeFrame timeFrame, final @NonNull Symbol symbol) {

    final Collection<RangerProfit> rangerProfitCollection = IntStream.rangeClosed(0, (RANGER_TP_SL.getValue() - RANGER_TP_SL.getKey()) / SKIP_RANGER_TP).mapToObj(i -> {
      final int power = (SKIP_RANGER_TP * i);
      final int tp = power + RANGER_TP_SL.getKey();
      final int sl = BigDecimal.valueOf(-tp).multiply(RISK_SL).intValue();
      return new RangerProfit(BigDecimal.valueOf(tp), BigDecimal.valueOf(sl));
    }).collect(Collectors.toSet());

    return CandlestickService.getCandlesticks(bufferedReader, timeFrame).filter(candlestick -> !SignalIndicator.NEUTRAL.equals(candlestick.getSignalIndicator()))
        .flatMap(candlestick -> rangerProfitCollection.stream().map(rangerProfit -> {
          final PreTrade preTrade = new PreTrade(rangerProfit,
              new TimeScope(candlestick.getOpenTickTimestamp().getDayOfWeek(), candlestick.getOpenTickTimestamp().getHour() / timeFrame.getSlotTimeH()));
          TickService.getTicks(bufferedReader).forEach(tickTickPair -> {
            if (preTrade.getOrderStatus().equals(OrderStatus.OPEN)) {
              final Tick currentTick = tickTickPair.getKey();
              if (currentTick.getDateTime().isAfter(candlestick.getOpenTickTimestamp())) {
                final Tick lastTick = tickTickPair.getValue();
                final BigDecimal tmpProfit = candlestick.getSignalIndicator().getOrderType().getProfit(lastTick, currentTick, symbol);
                preTrade.setProfit(preTrade.getProfit().add(tmpProfit));
              } else if (currentTick.getDateTime().isEqual(candlestick.getOpenTickTimestamp())) {
                preTrade.setProfit(currentTick.getSpread());
              }
            }
          });
          return preTrade;
        })).collect(Collectors.groupingBy(PreTrade::getTimeScope, Collectors.groupingBy(PreTrade::getRangerProfit))).entrySet().parallelStream()
        .map(timeScopeMapEntry -> timeScopeMapEntry.getValue().entrySet().parallelStream().map(rangerProfitListEntry -> {
          final TimeScope timeScope = timeScopeMapEntry.getKey();
          final RangerProfit rangerProfit = rangerProfitListEntry.getKey();
          final long numberPreTradesTP = rangerProfitListEntry.getValue().stream().filter(preTrade -> OrderStatus.TAKE_PROFIT.equals(preTrade.getOrderStatus())).count();
          final long numberPreTradesSL = rangerProfitListEntry.getValue().stream().filter(preTrade -> OrderStatus.STOP_LOSS.equals(preTrade.getOrderStatus())).count();
          final long numberPreTradesTotal = numberPreTradesTP + numberPreTradesSL;
          final BigDecimal hitPercentage =
              numberPreTradesTotal == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numberPreTradesTP).divide(BigDecimal.valueOf(numberPreTradesTotal), MathUtils.SCALE, MathUtils.ROUNDING_MODE);
          final BigDecimal profitTotal = rangerProfitListEntry.getValue().stream().filter(preTrade -> !OrderStatus.OPEN.equals(preTrade.getOrderStatus())).map(PreTrade::getProfit)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

          return new Trade(rangerProfit.getStopLoss().intValue(), rangerProfit.getTakeProfit().intValue(), timeScope.getWeek(), timeScope.getHour(), numberPreTradesTotal, numberPreTradesTP,
              numberPreTradesSL, hitPercentage, profitTotal);

        }).filter(trade -> trade.getHitPercentage().compareTo(TP_TARGET) >= 0).min(Comparator.comparingDouble(value -> value.getProfitTotal().doubleValue()))).filter(Optional::isPresent)
        .map(Optional::get);
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
