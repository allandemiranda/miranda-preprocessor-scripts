package lu.forex.system.processor.utils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lu.forex.system.processor.enums.TimeFrame;

@UtilityClass
public class TimeFrameUtils {

  public static @NonNull LocalDateTime getCandlestickTimestamp(final @NonNull LocalDateTime timestamp, final @NonNull TimeFrame timeFrame) {
    return switch (timeFrame.getFrame()) {
      case MINUTE -> getMinuteTime(timestamp, timeFrame);
      case HOUR -> getHourTime(timestamp, timeFrame);
    };
  }

  private static @NonNull LocalDateTime getHourTime(final @NonNull LocalDateTime timestamp, final @NonNull TimeFrame timeFrame) {
    final int div = timestamp.getHour() / timeFrame.getTimeValue();
    final int newHour = div * timeFrame.getTimeValue();
    final LocalTime candlestickTime = LocalTime.of(newHour, 0, 0);
    return LocalDateTime.of(timestamp.toLocalDate(), candlestickTime);
  }

  private static @NonNull LocalDateTime getMinuteTime(final @NonNull LocalDateTime timestamp, final @NonNull TimeFrame timeFrame) {
    final int div = timestamp.getMinute() / timeFrame.getTimeValue();
    final int newMinute = div * timeFrame.getTimeValue();
    final LocalTime candlestickTime = LocalTime.of(timestamp.getHour(), newMinute, 0);
    return LocalDateTime.of(timestamp.toLocalDate(), candlestickTime);
  }
}
