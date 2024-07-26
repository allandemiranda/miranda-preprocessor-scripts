package lu.forex.system.processor.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtils {

  public static final int SCALE = 50;
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  public static @NonNull BigDecimal getMed(final @NonNull Collection<@NonNull BigDecimal> collection) {
    return getMed(collection.stream(), collection.size());
  }

  public static @NonNull BigDecimal getMed(final BigDecimal @NonNull ... values) {
    return getMed(Arrays.stream(values), values.length);
  }

  private static @NonNull BigDecimal getMed(final @NonNull Stream<@NonNull BigDecimal> bigDecimalStream, final int size) {
    return bigDecimalStream.reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(size), SCALE, ROUNDING_MODE);
  }

  public static @NonNull BigDecimal getMax(final BigDecimal @NonNull ... values) {
    return Arrays.stream(values).reduce(values[0], BigDecimal::max);
  }

  public static @NonNull BigDecimal getMax(final @NonNull Collection<@NonNull BigDecimal> collection) {
    return getMax(collection.toArray(BigDecimal[]::new));
  }

  public static @NonNull BigDecimal getMin(final BigDecimal @NonNull ... values) {
    return Arrays.stream(values).reduce(values[0], BigDecimal::min);
  }

  public static @NonNull BigDecimal getMin(final @NonNull Collection<@NonNull BigDecimal> collection) {
    return getMin(collection.toArray(BigDecimal[]::new));
  }

  public static @NonNull BigDecimal getSum(final @NonNull Collection<@NonNull BigDecimal> collection) {
    return collection.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public static @NonNull BigDecimal getSum(final BigDecimal @NonNull ... values) {
    return getSum(Arrays.asList(values));
  }

  public static @NonNull BigDecimal getSubtract(final @NonNull BigDecimal a, final @NonNull BigDecimal b) {
    return a.subtract(b);
  }

  public static @NonNull BigDecimal getDivision(final @NonNull BigDecimal dividend, final @NonNull int divisor) {
    return dividend.divide(BigDecimal.valueOf(divisor), SCALE, ROUNDING_MODE);
  }

  public static @NonNull BigDecimal getDivision(final @NonNull BigDecimal dividend, final @NonNull BigDecimal divisor) {
    return dividend.divide(divisor, SCALE, ROUNDING_MODE);
  }

  public static @NonNull BigDecimal getMultiplication(final @NonNull BigDecimal a, final @NonNull BigDecimal b) {
    return a.multiply(b);
  }
}
