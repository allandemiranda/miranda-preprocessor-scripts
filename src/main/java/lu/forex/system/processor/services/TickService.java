package lu.forex.system.processor.services;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lu.forex.system.processor.models.Tick;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class TickService {

  /**
   * @return The ticks Pair ( CURRENT | LAST )
   */
  @SneakyThrows
  public static Stream<Pair<Tick, Tick>> getTicks(final@NonNull BufferedReader bufferedReader) {
      final AtomicReference<Tick> lastTick = new AtomicReference<>(new Tick(LocalDateTime.MIN, BigDecimal.valueOf(-1d), BigDecimal.valueOf(-1d)));
      return bufferedReader.lines().skip(1).map(line -> {
        final Tick lineTick = getLineTick(line);
        if (lineTick.getDateTime().isAfter(lastTick.get().getDateTime())) {
          final LocalDateTime dateTime = lineTick.getDateTime();
          final BigDecimal bid = lineTick.getBid().compareTo(BigDecimal.ZERO) > 0 ? lineTick.getBid() : lastTick.get().getBid();
          final BigDecimal ask = lineTick.getAsk().compareTo(BigDecimal.ZERO) > 0 ? lineTick.getAsk() : lastTick.get().getAsk();

          if (bid.compareTo(BigDecimal.ZERO) > 0 && ask.compareTo(BigDecimal.ZERO) > 0) {
            final Tick currentTick = new Tick(dateTime, bid, ask);
            return Pair.of(currentTick, lastTick.getAndSet(currentTick));
          }
        }
        return null;
      }).filter(Objects::nonNull);
  }

  private static @NonNull Tick getLineTick(final @NonNull String line) {
    final String[] parts = line.split("\t");
    final String date = parts[0].replace(".", "-");
    final String time = parts[1];
    final String dataTime = date.concat("T").concat(time);
    final LocalDateTime localDateTime = LocalDateTime.parse(dataTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    final BigDecimal bid = parts[2].isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(parts[2]));
    final BigDecimal ask = parts[3].isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(Double.parseDouble(parts[3]));
    return new Tick(localDateTime, bid, ask);
  }
}
