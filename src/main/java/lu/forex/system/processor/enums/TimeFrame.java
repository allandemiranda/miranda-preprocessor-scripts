package lu.forex.system.processor.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public enum TimeFrame {
  //@formatter:off
  M15(15, Frame.MINUTE, 4),
//  M30(2 * M15.getTimeValue(), Frame.MINUTE, 4),
//  H1(1, Frame.HOUR, 4),
//  H2(2 * H1.getTimeValue(), Frame.HOUR, 8),
//  H4(4 * H1.getTimeValue(), Frame.HOUR, 24),
//  H8(8 * H1.getTimeValue(), Frame.HOUR, 24)
  ;
  //@formatter:on

  private final int timeValue;
  @NonNull
  private final Frame frame;
  private final int slotTimeH;

  @Getter
  @AllArgsConstructor
  public enum Frame {
    MINUTE(1), HOUR(60 * MINUTE.minutes);

    @NonNull
    private final int minutes;
  }
}
