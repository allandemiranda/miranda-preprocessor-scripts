package lu.forex.system.processor.utils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lu.forex.system.processor.enums.SignalIndicator;
import org.apache.poi.ss.usermodel.Cell;

@UtilityClass
public class XmlUtils {

  public static void setCellValue(final Object o, final @NonNull Cell cell) {
    if (Objects.nonNull(o)) {
      switch (o) {
        case String text -> cell.setCellValue(text);
        case Integer integer -> cell.setCellValue(integer);
        case Double doubles -> cell.setCellValue(doubles);
        case BigDecimal decimals -> cell.setCellValue(decimals.doubleValue());
        case Long longs -> cell.setCellValue(longs);
        case LocalDateTime localDateTime -> cell.setCellValue(localDateTime.toString().replace("T", " ").split("\\.")[0]);
        case LocalTime localTime -> cell.setCellValue(localTime.toString());
        case LocalDate localDate -> cell.setCellValue(localDate.toString());
        case DayOfWeek dayOfWeek -> cell.setCellValue(dayOfWeek.name());
        case SignalIndicator signalIndicator -> cell.setCellValue(signalIndicator.name());
        default -> throw new IllegalStateException("Unexpected value: " + o);
      }
    }
  }
}
