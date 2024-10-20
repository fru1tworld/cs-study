/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package modernjavainaction.chap10.dsl;

import static modernjavainaction.chap10.dsl.MixedBuilder.buy;
import static modernjavainaction.chap10.dsl.MixedBuilder.forCustomer;
import static modernjavainaction.chap10.dsl.MixedBuilder.sell;

import java.util.function.DoubleUnaryOperator;

import modernjavainaction.chap10.dsl.model.Order;
import modernjavainaction.chap10.dsl.model.Tax;

public class TaxCalculator {

  public static double calculate(Order order, boolean useRegional, boolean useGeneral, boolean useSurcharge) {
    double value = order.getValue();
    if (useRegional) {
      value = Tax.regional(value);
    }
    if (useGeneral) {
      value = Tax.general(value);
    }
    if (useSurcharge) {
      value = Tax.surcharge(value);
    }
    return value;
  }

  private boolean useRegional;
  private boolean useGeneral;
  private boolean useSurcharge;

  public TaxCalculator withTaxRegional() {
    useRegional = true;
    return this;
  }

  public TaxCalculator withTaxGeneral() {
    useGeneral= true;
    return this;
  }

  public TaxCalculator withTaxSurcharge() {
    useSurcharge = true;
    return this;
  }

  public double calculate(Order order) {
    return calculate(order, useRegional, useGeneral, useSurcharge);
  }

  public DoubleUnaryOperator taxFunction = d -> d;

  public TaxCalculator with(DoubleUnaryOperator f) {
    taxFunction = taxFunction.andThen(f);
    return this;
  }

  public double calculateF(Order order) {
    return taxFunction.applyAsDouble(order.getValue());
  }

  public static void main(String[] args) {
    Order order =
        forCustomer("BigBank",
            buy(t -> t.quantity(80)
                .stock("IBM")
                .on("NYSE")
                .at(125.00)),
            sell(t -> t.quantity(50)
                .stock("GOOGLE")
                .on("NASDAQ")
                .at(125.00)));

    double value = TaxCalculator.calculate(order, true, false, true);
    System.out.printf("Boolean arguments: %.2f%n", value);

    value = new TaxCalculator().withTaxRegional()
        .withTaxSurcharge()
        .calculate(order);
    System.out.printf("Method chaining: %.2f%n", value);

    value = new TaxCalculator().with(Tax::regional)
        .with(Tax::surcharge)
        .calculateF(order);
    System.out.printf("Method references: %.2f%n", value);
  }

}
