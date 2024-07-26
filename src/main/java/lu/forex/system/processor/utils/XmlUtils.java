package lu.forex.system.processor.utils;

import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.Cell;

@UtilityClass
public class XmlUtils {

  public static void setCellValue(final Object o, final @NonNull Cell cell) {
    if (Objects.nonNull(o)) {
      switch (o) {
        case String text -> cell.setCellValue(text);
        case Integer integer -> cell.setCellValue(integer);
        case Double doubles -> cell.setCellValue(doubles);
        case Long longs -> cell.setCellValue(longs);
        default -> throw new IllegalStateException("Unexpected value: " + o);
      }
    }
  }
}
