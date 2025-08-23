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

import static modernjavainaction.chap10.dsl.MethodChainingOrderBuilder.forCustomer;
import static modernjavainaction.chap10.dsl.MixedBuilder.buy;
import static modernjavainaction.chap10.dsl.MixedBuilder.forCustomer;
import static modernjavainaction.chap10.dsl.MixedBuilder.sell;
import static modernjavainaction.chap10.dsl.NestedFunctionOrderBuilder.at;
import static modernjavainaction.chap10.dsl.NestedFunctionOrderBuilder.buy;
import static modernjavainaction.chap10.dsl.NestedFunctionOrderBuilder.on;
import static modernjavainaction.chap10.dsl.NestedFunctionOrderBuilder.order;
import static modernjavainaction.chap10.dsl.NestedFunctionOrderBuilder.sell;
import static modernjavainaction.chap10.dsl.NestedFunctionOrderBuilder.stock;

import modernjavainaction.chap10.dsl.model.Order;
import modernjavainaction.chap10.dsl.model.Stock;
import modernjavainaction.chap10.dsl.model.Trade;

public class Main {

  public static void main(String[] args) {
    Main main = new Main();
    main.plain();
    main.methodChaining();
    main.nestedFunction();
    main.lambda();
    main.mixed();
  }

  public void plain() {
    Order order = new Order();
    order.setCustomer("BigBank");

    Trade trade1 = new Trade();
    trade1.setType(Trade.Type.BUY);

    Stock stock1 = new Stock();
    stock1.setSymbol("IBM");
    stock1.setMarket("NYSE");

    trade1.setStock(stock1);
    trade1.setPrice(125.00);
    trade1.setQuantity(80);
    order.addTrade(trade1);

    Trade trade2 = new Trade();
    trade2.setType(Trade.Type.BUY);

    Stock stock2 = new Stock();
    stock2.setSymbol("GOOGLE");
    stock2.setMarket("NASDAQ");

    trade2.setStock(stock2);
    trade2.setPrice(375.00);
    trade2.setQuantity(50);
    order.addTrade(trade2);

    System.out.println("Plain:");
    System.out.println(order);
  }

  public void methodChaining() {
    Order order = forCustomer("BigBank")
        .buy(80).stock("IBM").on("NYSE").at(125.00)
        .sell(50).stock("GOOGLE").on("NASDAQ").at(375.00)
        .end();

    System.out.println("Method chaining:");
    System.out.println(order);
  }

  public void nestedFunction() {
    Order order = order("BigBank",
        buy(80,
            stock("IBM", on("NYSE")),
            at(125.00)),
        sell(50,
            stock("GOOGLE", on("NASDAQ")),
            at(375.00))
    );

    System.out.println("Nested function:");
    System.out.println(order);
  }

  public void lambda() {
    Order order = LambdaOrderBuilder.order( o -> {
      o.forCustomer( "BigBank" );
      o.buy( t -> {
        t.quantity(80);
        t.price(125.00);
        t.stock(s -> {
          s.symbol("IBM");
          s.market("NYSE");
        });
      });
      o.sell( t -> {
        t.quantity(50);
        t.price(375.00);
        t.stock(s -> {
          s.symbol("GOOGLE");
          s.market("NASDAQ");
        });
      });
    });

    System.out.println("Lambda:");
    System.out.println(order);
  }

  public void mixed() {
    Order order =
        forCustomer("BigBank",
            buy(t -> t.quantity(80)
                .stock("IBM")
                .on("NYSE")
                .at(125.00)),
            sell(t -> t.quantity(50)
                .stock("GOOGLE")
                .on("NASDAQ")
                .at(375.00)));

    System.out.println("Mixed:");
    System.out.println(order);
  }

}
