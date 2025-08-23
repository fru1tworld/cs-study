package modernjavainaction.chap19;

import java.util.function.Consumer;

public class PersistentTrainJourney {

  public static void main(String[] args) {
    TrainJourney tj1 = new TrainJourney(40, new TrainJourney(30, null));
    TrainJourney tj2 = new TrainJourney(20, new TrainJourney(50, null));

    TrainJourney appended = append(tj1, tj2);
    visit(appended, tj -> {
      System.out.print(tj.price + " - ");
    });
    System.out.println();

    // tj1, tj2를 바꾸지 않고 새 TrainJourney를 생성
    TrainJourney appended2 = append(tj1, tj2);
    visit(appended2, tj -> {
      System.out.print(tj.price + " - ");
    });
    System.out.println();

    // tj1은 바뀌었지만 여전히 결과에서 확인할 수 없음
    TrainJourney linked = link(tj1, tj2);
    visit(linked, tj -> {
      System.out.print(tj.price + " - ");
    });
    System.out.println();

	// ... 여기서 이 코드의 주석을 해제하면 tj2가 이미 바뀐 tj1의 끝에 추가된다. 끝없는 visit() 재귀 호출이 일어나면서 StackOverflowError가 발생함.
    /*TrainJourney linked2 = link(tj1, tj2);
    visit(linked2, tj -> { System.out.print(tj.price + " - "); });
    System.out.println();*/

    System.out.println("--------------------");
    compareLinkAndAppend();
  }

  private static void compareLinkAndAppend() {
    System.out.println("Destructive update:");
    TrainJourney firstJourney = new TrainJourney(1, null);
    TrainJourney secondJourney = new TrainJourney(2, null);
    TrainJourney xToZ = link(firstJourney, secondJourney);
    System.out.printf("firstJourney (X to Y) = %s%n", firstJourney);
    System.out.printf("secondJourney (Y to Z) = %s%n", secondJourney);
    System.out.printf("X to Z = %s%n", xToZ);

    System.out.println();
    System.out.println("The functional way:");
    firstJourney = new TrainJourney(1, null);
    secondJourney = new TrainJourney(2, null);
    xToZ = append(firstJourney, secondJourney);
    System.out.printf("firstJourney (X to Y) = %s%n", firstJourney);
    System.out.printf("secondJourney (Y to Z) = %s%n", secondJourney);
    System.out.printf("X to Z = %s%n", xToZ);
  }

  static class TrainJourney {

    public int price;
    public TrainJourney onward;

    public TrainJourney(int p, TrainJourney t) {
      price = p;
      onward = t;
    }

    @Override
    public String toString() {
      return String.format("TrainJourney[%d] -> %s", price, onward);
    }

  }

  static TrainJourney link(TrainJourney a, TrainJourney b) {
    if (a == null) {
      return b;
    }
    TrainJourney t = a;
    while (t.onward != null) {
      t = t.onward;
    }
    t.onward = b;
    return a;
  }

  static TrainJourney append(TrainJourney a, TrainJourney b) {
    return a == null ? b : new TrainJourney(a.price, append(a.onward, b));
  }

  static void visit(TrainJourney journey, Consumer<TrainJourney> c) {
    if (journey != null) {
      c.accept(journey);
      visit(journey.onward, c);
    }
  }

}
