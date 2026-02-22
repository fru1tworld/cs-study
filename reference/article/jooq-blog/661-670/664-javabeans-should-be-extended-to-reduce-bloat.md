# JavaBeans는 비대함을 줄이기 위해 확장되어야 한다

> 원문: https://blog.jooq.org/javabeans-should-be-extended-to-reduce-bloat/

게시일: 2012년 10월 19일, 작성자: Lukas Eder

JavaBeans는 Java 세계에서 오랜 시간 동안 존재해 왔습니다. 어느 시점에서 사람들은 getter와 setter가 직접 접근해서는 안 되는 객체 속성에 대한 좋은 추상화를 제공한다는 것을 깨달았습니다. 전형적인 bean은 다음과 같습니다:

```java
public class MyBean {
    private int myProperty;

    public int getMyProperty() {
        return myProperty;
    }

    public void setMyProperty(int myProperty) {
        this.myProperty = myProperty;
    }
}
```

다양한 표현 언어와 다른 표기법에서 간단한 속성 표기법을 사용하여 "myProperty"에 접근할 수 있습니다:

```java
// 아래는 myBean.getMyProperty()로 해석됩니다
myBean.myProperty

// 이것은 myBean.setMyProperty(5)로 해석될 수 있습니다
myBean.myProperty = 5
```

## Java 속성에 대한 비판

C#과 같은 다른 언어에서는 일반 코드에서 속성 표현식을 인라인으로 사용하여 getter와 setter를 호출할 수 있습니다. 왜 Java는 안 될까요?

### Getter와 Setter 명명

왜 개발자들은 객체 속성을 조작할 때 비대한 "get"/"is"와 "set" 접두사를 사용해야 할까요? 게다가 첫 글자의 대소문자도 바뀝니다. 대소문자를 구분하는 속성 사용 검색을 위해서는 복잡한 정규 표현식을 작성해야 합니다.

### Void를 반환하는 Setter

void를 반환하는 것은 API 호출 지점에서 상당한 비대함을 만듭니다. Java 초기부터 메서드 체이닝은 널리 퍼진 관행이었습니다. StringBuilder의 체이닝 가능한 append() 메서드를 그리워하지 않을 사람은 없을 것입니다—정말 유용하니까요. 왜 컴파일러는 setter를 호출한 후 속성 컨테이너에 다시 접근하는 것을 허용하지 않을까요?

## 더 나은 Java

다음과 같은 API 설계가 있다고 합시다:

```java
public interface API {
    void oneMethod();
    void anotherMethod();
    void setX(int x);
    int  getX();
}
```

다음과 같이 사용할 수 있어야 합니다:

```java
API api = ...
int x = api.oneMethod()     // void를 반환하면 api를 반환해야 합니다
           .anotherMethod() // void를 반환하면 api를 반환해야 합니다
           .x;              // Getter 접근
```

이것을 JSR로 만듭시다!
