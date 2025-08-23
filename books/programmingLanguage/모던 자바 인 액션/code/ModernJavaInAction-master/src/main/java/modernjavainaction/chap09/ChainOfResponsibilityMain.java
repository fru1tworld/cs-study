package modernjavainaction.chap09;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ChainOfResponsibilityMain {

  public static void main(String[] args) {
    ProcessingObject<String> p1 = new HeaderTextProcessing();
    ProcessingObject<String> p2 = new SpellCheckerProcessing();
    p1.setSuccessor(p2);
    String result1 = p1.handle("Aren't labdas really sexy?!!");
    System.out.println(result1);

    UnaryOperator<String> headerProcessing = (String text) -> "From Raoul, Mario and Alan: " + text;
    UnaryOperator<String> spellCheckerProcessing = (String text) -> text.replaceAll("labda", "lambda");
    Function<String, String> pipeline = headerProcessing.andThen(spellCheckerProcessing);
    String result2 = pipeline.apply("Aren't labdas really sexy?!!");
    System.out.println(result2);
  }

  private static abstract class ProcessingObject<T> {

    protected ProcessingObject<T> successor;

    public void setSuccessor(ProcessingObject<T> successor) {
      this.successor = successor;
    }

    public T handle(T input) {
      T r = handleWork(input);
      if (successor != null) {
        return successor.handle(r);
      }
      return r;
    }

    abstract protected T handleWork(T input);

  }

  private static class HeaderTextProcessing extends ProcessingObject<String> {

    @Override
    public String handleWork(String text) {
      return "From Raoul, Mario and Alan: " + text;
    }

  }

  private static class SpellCheckerProcessing extends ProcessingObject<String> {

    @Override
    public String handleWork(String text) {
      return text.replaceAll("labda", "lambda");
    }

  }

}
