package modernjavainaction.chap19;

import java.util.function.DoubleUnaryOperator;

public class Currying {

  public static void main(String[] args) {
    DoubleUnaryOperator convertCtoF = curriedConverter(9.0 / 5, 32);
    DoubleUnaryOperator convertUSDtoGBP = curriedConverter(0.6, 0);
    DoubleUnaryOperator convertKmtoMi = curriedConverter(0.6214, 0);

    System.out.printf("24 °C = %.2f °F%n", convertCtoF.applyAsDouble(24));
    System.out.printf("US$100 = £%.2f%n", convertUSDtoGBP.applyAsDouble(100));
    System.out.printf("20 km = %.2f miles%n", convertKmtoMi.applyAsDouble(20));

    DoubleUnaryOperator convertFtoC = expandedCurriedConverter(-32, 5.0 / 9, 0);
    System.out.printf("98.6 °F = %.2f °C", convertFtoC.applyAsDouble(98.6));
  }

  static double converter(double x, double y, double z) {
    return x * y + z;
  }

  static DoubleUnaryOperator curriedConverter(double y, double z) {
    return (double x) -> x * y + z;
  }

  static DoubleUnaryOperator expandedCurriedConverter(double w, double y, double z) {
    return (double x) -> (x + w) * y + z;
  }

}
