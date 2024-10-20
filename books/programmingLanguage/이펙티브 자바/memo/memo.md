# 플라이웨이트 패턴의 핵심 아이디어

**플라이웨이트 패턴(Flyweight Pattern)**은 소프트웨어 디자인 패턴 중 하나로, 객체를 가능한 한 공유하여 메모리 사용량을 최소화하고 성능을 최적화하는 데 사용됩니다.

이 패턴은 동일하거나 비슷한 객체를 여러 개 생성해야 할 때 유용합니다.

### 플라이웨이트 패턴의 핵심 아이디어

- 공유: 동일한 데이터를 여러 객체에서 공유하여 메모리를 절약.
- 내부 상태(Intrinsic State): 객체 간에 공유 가능한 불변 데이터를 정의.
- 외부 상태(Extrinsic State): 객체 간에 공유되지 않는 고유 데이터를 정의하며, 호출 시 전달.

### 구조

1. Flyweight 인터페이스: 공유 가능한 객체를 위한 인터페이스를 정의.
2. Concrete Flyweight: Flyweight 인터페이스를 구현하며, 상태를 저장하거나 외부 상태를 사용하는 객체.
3. Flyweight Factory: Flyweight 객체를 생성하고 관리하며, 동일한 객체를 재사용하도록 보장.
4. Client: Flyweight 객체를 사용하는 부분으로, 외부 상태를 전달하고 내부 상태를 참조.

```java
// Flyweight 인터페이스
interface Shape {
    void draw(String color); // 외부 상태
}

// Concrete Flyweight
class Circle implements Shape {
    private final String intrinsicState = "Circle"; // 내부 상태

    @Override
    public void draw(String color) {
        System.out.println("Drawing " + intrinsicState + " with color " + color);
    }
}

// Flyweight Factory
class ShapeFactory {
    private static final Map<String, Shape> shapeMap = new HashMap<>();

    public static Shape getCircle() {
        String key = "Circle";
        if (!shapeMap.containsKey(key)) {
            shapeMap.put(key, new Circle());
            System.out.println("Creating new Circle object.");
        }
        return shapeMap.get(key);
    }
}

// Client
public class FlyweightPatternExample {
    public static void main(String[] args) {
        Shape circle1 = ShapeFactory.getCircle();
        circle1.draw("Red");

        Shape circle2 = ShapeFactory.getCircle();
        circle2.draw("Blue");

        System.out.println(circle1 == circle2); // true (객체 공유)
    }
}

```

장점

- 객체 수를 줄여 메모리 사용량 절감.
- 성능 최적화.
- 동일한 데이터에 대해 객체를 효율적으로 관리.

단점

- 복잡도가 증가.
- 외부 상태 관리가 필요.
- 공유할 수 없는 상태가 많으면 적용하기 어려움.
