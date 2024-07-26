package lu.forex.system.processor.models;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import lombok.NonNull;
import lu.forex.system.processor.enums.Indicator;
import lu.forex.system.processor.enums.SignalIndicator;

public interface TechnicalIndicator extends Serializable {

  @NonNull
  Indicator getIndicator();

  @NonNull
  Map<String, BigDecimal> getData();

  @NonNull
  SignalIndicator getSignal();

  void setSignal(@NonNull SignalIndicator signal);

  @NonNull
  int getNumberOfCandlesticksToCalculate();

  @NonNull
  void calculate(final @NonNull Candlestick @NonNull [] candlesticks);
}
