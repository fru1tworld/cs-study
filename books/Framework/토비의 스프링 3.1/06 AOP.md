# 6장 AOP

AOP는 IoC/DI, 서비스 추상화와 더불어 스프링의 3대 기반기술의 하나다.

## 6.1 트랜잭션 코드의 분리

### 메소드 분리

트랜잭션 경계설정과 비즈니스 로직이 공존하는 메소드를 분리할 수 있다.

```java
public void upgradeLevels() throws Exception {
    TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
    try {
        upgradeLevelsInternal(); // 비즈니스 로직
        this.transactionManager.commit(status);
    } catch (Exception e) {
        this.transactionManager.rollback(status);
        throw e;
    }
}

private void upgradeLevelsInternal() {
    List<User> users = userDao.getAll();
    for (User user : users) {
        if (canUpgradeLevel(user)) {
            upgradeLevel(user);
        }
    }
}
```

### DI를 이용한 클래스의 분리

트랜잭션 코드를 클래스 밖으로 뽑아내면서도 기존 클라이언트의 수정 없이 유지하려면 DI를 적용한다.

```java
public interface UserService {
    void add(User user);
    void upgradeLevels();
}

public class UserServiceImpl implements UserService {
    UserDao userDao;
    MailSender mailSender;

    public void upgradeLevels() {
        List<User> users = userDao.getAll();
        for (User user : users) {
            if (canUpgradeLevel(user)) {
                upgradeLevel(user);
            }
        }
    }
}

public class UserServiceTx implements UserService {
    UserService userService;
    PlatformTransactionManager transactionManager;

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void add(User user) {
        userService.add(user);
    }

    public void upgradeLevels() {
        TransactionStatus status = this.transactionManager
                .getTransaction(new DefaultTransactionDefinition());
        try {
            userService.upgradeLevels();
            this.transactionManager.commit(status);
        } catch (RuntimeException e) {
            this.transactionManager.rollback(status);
            throw e;
        }
    }
}
```

## 6.2 고립된 단위 테스트

### 복잡한 의존관계 속의 테스트

UserService를 테스트하는 것처럼 보이지만 사실은 그 뒤에 존재하는 훨씬 더 많은 오브젝트와 환경, 서비스, 서버, 네트워크까지 함께 테스트하는 셈이 된다.

### 테스트 대상 오브젝트 고립시키기

테스트의 대상이 환경이나, 외부 서버, 다른 클래스의 코드에 종속되고 영향을 받지 않도록 고립시킬 필요가 있다. 이를 위해 테스트를 위한 대역을 사용한다.

### 테스트 대역의 종류

- ** 테스트 스텁(test stub)**: 테스트 대상 오브젝트의 의존객체로서 존재하면서 테스트 동안에 코드가 정상적으로 수행될 수 있도록 돕는 것
- ** 목 오브젝트(mock object)**: 스텁처럼 테스트 오브젝트가 정상적으로 실행되도록 도와주면서, 테스트 오브젝트와 자신 사이에서 일어나는 커뮤니케이션 내용을 저장해뒀다가 테스트 결과를 검증하는 데 활용

### 단위 테스트와 통합 테스트

- ** 단위 테스트**: 테스트 대상 클래스를 목 오브젝트 등의 테스트 대역을 이용해 의존 오브젝트나 외부의 리소스를 사용하지 않도록 고립시켜서 테스트하는 것
- ** 통합 테스트**: 두 개 이상의, 성격이나 계층이 다른 오브젝트가 연동하도록 만들어 테스트하거나, 외부의 DB나 파일, 서비스 등의 리소스가 참여하는 테스트

### Mockito 프레임워크

간단한 메소드 호출만으로 다이나믹하게 특정 인터페이스를 구현한 테스트용 목 오브젝트를 만들 수 있다.

```java
UserDao mockUserDao = mock(UserDao.class);
when(mockUserDao.getAll()).thenReturn(this.users);
verify(mockUserDao, times(2)).update(any(User.class));
```

## 6.3 다이나믹 프록시와 팩토리 빈

### 프록시와 프록시 패턴, 데코레이터 패턴

** 프록시(proxy)** 는 클라이언트가 사용하려고 하는 실제 대상인 것처럼 위장해서 클라이언트의 요청을 받아주는 대리자 역할을 한다.

프록시를 통해 최종적으로 요청을 위임받아 처리하는 실제 오브젝트를 ** 타깃(target)** 또는 ** 실체(real subject)** 라고 부른다.

프록시의 사용 목적에 따라 두 가지로 구분할 수 있다:
- 클라이언트가 타깃에 접근하는 방법을 제어하기 위해서 → ** 프록시 패턴**
- 타깃에 부가적인 기능을 부여해주기 위해서 → ** 데코레이터 패턴**

### 데코레이터 패턴

데코레이터 패턴은 타깃에 부가적인 기능을 런타임 시 다이나믹하게 부여해주기 위해 프록시를 사용하는 패턴이다.

```
클라이언트 → 데코레이터A → 데코레이터B → 타깃
```

프록시로서 동작하는 각 데코레이터는 위임하는 대상에도 인터페이스로 접근하기 때문에 자신이 최종 타깃으로 위임하는지, 아니면 다음 단계의 데코레이터 프록시로 위임하는지 알지 못한다.

### 프록시 패턴

프록시 패턴은 타깃의 기능 자체에는 관여하지 않으면서 접근하는 방법을 제어해주는 프록시를 이용하는 것이다.

타깃 오브젝트를 생성하기가 복잡하거나 당장 필요하지 않은 경우에는 꼭 필요한 시점까지 오브젝트를 생성하지 않는 편이 좋다. 그런데 타깃 오브젝트에 대한 레퍼런스가 미리 필요할 수 있다. 이럴 때 프록시 패턴을 적용하면 된다.

### 다이나믹 프록시

프록시는 기존 코드에 영향을 주지 않으면서 타깃의 기능을 확장하거나 접근 방법을 제어할 수 있는 유용한 방법이다. 그러나 프록시를 만드는 것은 번거롭다.

#### 리플렉션

다이나믹 프록시는 리플렉션 기능을 이용해서 프록시를 만들어준다. 리플렉션은 자바의 코드 자체를 추상화해서 접근하도록 만든 것이다.

```java
Method lengthMethod = String.class.getMethod("length");
int length = (Integer) lengthMethod.invoke(name); // name.length()
```

#### 다이나믹 프록시 적용

다이나믹 프록시는 프록시 팩토리에 의해 런타임 시 다이나믹하게 만들어지는 오브젝트다.

```java
public class UppercaseHandler implements InvocationHandler {
    Hello target;

    public UppercaseHandler(Hello target) {
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String ret = (String) method.invoke(target, args);
        return ret.toUpperCase(); // 부가기능
    }
}

Hello proxiedHello = (Hello) Proxy.newProxyInstance(
    getClass().getClassLoader(),
    new Class[] { Hello.class },
    new UppercaseHandler(new HelloTarget())
);
```

### 팩토리 빈

스프링은 클래스 정보를 가지고 디폴트 생성자를 통해 오브젝트를 만드는 방법 외에도 빈을 만들 수 있는 여러 가지 방법을 제공한다.

대표적으로 ** 팩토리 빈** 을 이용한 빈 생성 방법이 있다. 팩토리 빈이란 스프링을 대신해서 오브젝트의 생성로직을 담당하도록 만들어진 특별한 빈을 말한다.

```java
public interface FactoryBean<T> {
    T getObject() throws Exception; // 빈 오브젝트를 생성해서 돌려준다
    Class<?> getObjectType(); // 생성되는 오브젝트의 타입을 알려준다
    boolean isSingleton(); // getObject()가 돌려주는 오브젝트가 싱글톤인지 알려준다
}
```

### 트랜잭션 프록시 팩토리 빈

```java
public class TxProxyFactoryBean implements FactoryBean<Object> {
    Object target;
    PlatformTransactionManager transactionManager;
    String pattern;
    Class<?> serviceInterface;

    public Object getObject() throws Exception {
        TransactionHandler txHandler = new TransactionHandler();
        txHandler.setTarget(target);
        txHandler.setTransactionManager(transactionManager);
        txHandler.setPattern(pattern);
        return Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[] { serviceInterface },
            txHandler
        );
    }

    public Class<?> getObjectType() {
        return serviceInterface;
    }

    public boolean isSingleton() {
        return false;
    }
}
```

### 프록시 팩토리 빈 방식의 장점과 한계

#### 장점
- 프록시 팩토리 빈의 재사용
- 다이나믹 프록시를 이용하면 타깃 인터페이스를 구현하는 클래스를 일일이 만드는 번거로움을 제거할 수 있다

#### 한계
- 한 번에 여러 개의 클래스에 공통적인 부가기능을 제공하는 것은 불가능하다
- 하나의 타깃에 여러 개의 부가기능을 적용하려고 할 때도 문제다
- TransactionHandler 오브젝트가 프록시 팩토리 빈 개수만큼 만들어진다

## 6.4 스프링의 프록시 팩토리 빈

### ProxyFactoryBean

스프링은 프록시 오브젝트를 생성해주는 기술을 추상화한 팩토리 빈을 제공한다.

```java
ProxyFactoryBean pfBean = new ProxyFactoryBean();
pfBean.setTarget(new HelloTarget());
pfBean.addAdvice(new UppercaseAdvice());

Hello proxiedHello = (Hello) pfBean.getObject();
```

### 어드바이스: 타깃이 필요 없는 순수한 부가기능

MethodInterceptor처럼 타깃 오브젝트에 적용하는 부가기능을 담은 오브젝트를 스프링에서는 ** 어드바이스(advice)** 라고 부른다.

```java
static class UppercaseAdvice implements MethodInterceptor {
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String ret = (String) invocation.proceed();
        return ret.toUpperCase();
    }
}
```

### 포인트컷: 부가기능 적용 대상 메소드 선정 방법

** 포인트컷(pointcut)** 은 메소드 선정 알고리즘을 담은 오브젝트다.

```java
ProxyFactoryBean pfBean = new ProxyFactoryBean();
pfBean.setTarget(new HelloTarget());

NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
pointcut.setMappedName("sayH*");

pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));
```

### 어드바이저: 포인트컷 + 어드바이스

어드바이저 = 포인트컷(메소드 선정 알고리즘) + 어드바이스(부가기능)

## 6.5 스프링 AOP

### 자동 프록시 생성

프록시 팩토리 빈 방식의 접근 방법의 한계로 인해 등장한 것이 자동 프록시 생성기다.

#### 빈 후처리기를 이용한 자동 프록시 생성기

**DefaultAdvisorAutoProxyCreator** 는 어드바이저를 이용한 자동 프록시 생성기다.

빈을 만들 때마다, 스프링 DI 컨테이너는 후처리기에 빈을 보낸다. 빈의 어드바이저 속 포인트컷을 통해 해당 빈이 프록시 적용 대상인지 확인한다. 적용 대상일 시, 내장된 프록시 생성기에게 프록시를 생성하게 한다.

```xml
<bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" />
```

### 포인트컷 표현식을 이용한 포인트컷

스프링은 아주 간단하고 효과적인 방법으로 포인트컷의 클래스와 메소드를 선정하는 알고리즘을 작성할 수 있는 방법을 제공한다. 이것을 ** 포인트컷 표현식** 이라고 부른다.

```java
AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
pointcut.setExpression("execution(* *..*ServiceImpl.upgrade*(..))");
```

#### 포인트컷 표현식 문법

```
execution([접근제한자 패턴] 리턴타입패턴 [패키지.클래스패턴.]메소드이름패턴(파라미터패턴) [throws 예외패턴])
```

예시:
- `execution(* *(..))`  - 모든 메소드
- `execution(* hello(..))`  - hello 이름의 메소드
- `execution(* hello())`  - 파라미터가 없는 hello 메소드
- `execution(* hello(String))`  - String 파라미터 하나를 갖는 hello 메소드
- `execution(* com.example..*.*(..))`  - com.example 패키지와 그 서브패키지의 모든 메소드

### AOP란 무엇인가?

#### 트랜잭션 서비스 추상화

트랜잭션 경계설정 코드를 비즈니스 로직을 담은 코드에 넣으면 특정 트랜잭션 기술에 종속된다. 그래서 트랜잭션 적용이라는 추상적인 작업 내용은 유지하되, 구체적인 구현 방법을 자유롭게 바꿀 수 있도록 서비스 추상화 기법을 적용했다.

#### 프록시와 데코레이터 패턴

트랜잭션을 어떻게 다룰 것인가는 DI를 이용해 해결했지만, 여전히 비즈니스 로직 코드에는 트랜잭션을 적용하고 있다는 사실이 드러나 있다. 그래서 DI를 이용해 데코레이터 패턴을 적용하는 방법을 사용했다.

#### 다이나믹 프록시와 프록시 팩토리 빈

프록시를 이용해서 비즈니스 로직 코드에서 트랜잭션 코드는 모두 제거할 수 있었지만, 비즈니스 로직 인터페이스의 모든 메소드마다 트랜잭션 기능을 부여하는 코드를 넣어 프록시 클래스를 만드는 작업이 큰 짐이 됐다. 그래서 프록시 클래스 없이도 프록시 오브젝트를 런타임 시에 만들어주는 JDK 다이나믹 프록시 기술을 적용했다.

#### 자동 프록시 생성 방법과 포인트컷

타깃 오브젝트마다 거의 비슷한 내용의 ProxyFactoryBean 빈 설정정보를 추가하는 부분을 제거하기 위해 스프링 컨테이너의 빈 생성 후처리 기법을 활용해 컨테이너 초기화 시점에서 자동으로 프록시를 만들어주는 방법을 도입했다.

#### 부가기능의 모듈화

관심사가 같은 코드를 분리해 한 데 모으는 것은 소프트웨어 개발의 가장 기본이 되는 원칙이다. 하지만 트랜잭션 적용 코드는 기존의 객체지향 설계 패러다임과는 구분되는 새로운 특성이 있다. 트랜잭션 경계설정 기능은 다른 모듈의 코드에 부가적으로 부여되는 기능이라는 특징이 있기 때문이다.

이렇게 부가기능처럼 특정 관점을 기준으로 바라볼 수 있게 만들어주는 모듈을 ** 애스펙트(aspect)** 라고 한다.

### AOP: 애스펙트 지향 프로그래밍

애플리케이션의 핵심적인 기능에서 부가적인 기능을 분리해서 애스펙트라는 독특한 모듈로 만들어서 설계하고 개발하는 방법을 ** 애스펙트 지향 프로그래밍(Aspect Oriented Programming)** 또는 약자로 **AOP** 라고 부른다.

AOP는 OOP를 대체하는 새로운 개념이 아니라, OOP를 돕는 보조적인 기술이다.

## 6.6 AOP 용어 정리

### 타깃 (Target)

부가기능을 부여할 대상. 핵심기능을 담은 클래스일 수도 있지만, 경우에 따라서는 다른 부가기능을 제공하는 프록시 오브젝트일 수도 있다.

### 어드바이스 (Advice)

타깃에게 제공할 부가기능을 담은 모듈. 어드바이스는 오브젝트로 정의하기도 하지만 메소드 레벨에서 정의할 수도 있다.

### 조인 포인트 (Join Point)

어드바이스가 적용될 수 있는 위치를 말한다. 스프링의 프록시 AOP에서 조인 포인트는 메소드의 실행 단계뿐이다. 타깃 오브젝트가 구현한 인터페이스의 모든 메소드는 조인 포인트가 된다.

### 포인트컷 (Pointcut)

어드바이스를 적용할 조인 포인트를 선별하는 작업 또는 그 기능을 정의한 모듈을 말한다. 스프링 AOP의 조인 포인트는 메소드의 실행이므로 스프링의 포인트컷은 메소드를 선정하는 기능을 갖고 있다.

### 프록시 (Proxy)

클라이언트와 타깃 사이에 투명하게 존재하면서 부가기능을 제공하는 오브젝트. DI를 통해 타깃 대신 클라이언트에게 주입되며, 클라이언트의 메소드 호출을 대신 받아서 타깃에 위임해주면서, 그 과정에서 부가기능을 부여한다.

### 어드바이저 (Advisor)

포인트컷과 어드바이스를 하나씩 갖고 있는 오브젝트. 어드바이저는 어떤 부가기능(어드바이스)을 어디에(포인트컷) 전달할 것인가를 알고 있는 AOP의 가장 기본이 되는 모듈이다.

### 애스펙트 (Aspect)

OOP의 클래스와 마찬가지로 애스펙트는 AOP의 기본 모듈이다. 한 개 또는 그 이상의 포인트컷과 어드바이스의 조합으로 만들어지며 보통 싱글톤 형태의 오브젝트로 존재한다. 따라서 클래스와 같은 모듈 정의와 오브젝트와 같은 실체의 구분이 특별히 없다.

## 6.7 스프링 AOP 적용 방법

### AOP 적용 기술

#### 프록시를 이용한 AOP

스프링 AOP의 부가기능을 담은 어드바이스가 적용되는 대상은 오브젝트의 메소드다. 프록시 방식을 사용했기 때문에 메소드 호출 과정에 참여해서 부가기능을 제공해주게 되어 있다.

독립적으로 개발한 부가기능 모듈을 다양한 타깃 오브젝트의 메소드에 다이나믹하게 적용해주기 위해 가장 중요한 역할을 맡고 있는 게 바로 프록시다. 그래서 스프링 AOP는 프록시 방식의 AOP라고 할 수 있다.

#### AspectJ를 이용한 AOP

AspectJ는 프록시처럼 간접적인 방법이 아니라, 타깃 오브젝트를 뜯어고쳐서 부가기능을 직접 넣어주는 직접적인 방법을 사용한다.

컴파일된 타깃의 클래스 파일 자체를 수정하거나 클래스가 JVM에 로딩되는 시점을 가로채서 바이트코드를 조작하는 복잡한 방법을 사용한다.

AspectJ는 프록시 방식보다 훨씬 강력하고 유연한 AOP가 가능하다:
- 바이트코드를 직접 조작해서 AOP를 적용하면 스프링과 같은 컨테이너가 사용되지 않는 환경에서도 손쉽게 AOP의 적용이 가능해진다
- 프록시 방식보다 훨씬 유연하고 강력한 AOP가 가능하다

## 6.8 트랜잭션 속성

### 트랜잭션 정의

트랜잭션이라고 모두 같은 방식으로 동작하는 것은 아니다. 물론 트랜잭션의 기본 개념인 더 이상 쪼갤 수 없는 최소 단위의 작업이라는 개념은 항상 유효하다.

DefaultTransactionDefinition이 구현하고 있는 TransactionDefinition 인터페이스는 트랜잭션의 동작방식에 영향을 줄 수 있는 네 가지 속성을 정의하고 있다:

#### 트랜잭션 전파 (Transaction Propagation)

트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때 또는 없을 때 어떻게 동작할 것인가를 결정하는 방식을 말한다.

- **PROPAGATION_REQUIRED**: 진행 중인 트랜잭션이 없으면 새로 시작하고, 이미 시작된 트랜잭션이 있으면 이에 참여한다
- **PROPAGATION_REQUIRES_NEW**: 항상 새로운 트랜잭션을 시작한다
- **PROPAGATION_NOT_SUPPORTED**: 트랜잭션 없이 동작하도록 만든다

#### 격리수준 (Isolation Level)

모든 DB 트랜잭션은 격리수준을 갖고 있어야 한다. 서버환경에서는 여러 개의 트랜잭션이 동시에 진행될 수 있다.

#### 제한시간 (Timeout)

트랜잭션을 수행하는 제한시간을 설정할 수 있다.

#### 읽기전용 (Read Only)

읽기전용으로 설정해두면 트랜잭션 내에서 데이터를 조작하는 시도를 막아줄 수 있다. 또한 데이터 액세스 기술에 따라서 성능이 향상될 수도 있다.

### @Transactional

스프링은 @Transactional을 적용할 때 4단계의 대체(fallback) 정책을 이용하게 해준다. 타깃 메소드, 타깃 클래스, 선언 메소드, 선언 타입(클래스, 인터페이스)의 순서로 @Transactional이 적용됐는지 차례로 확인하고, 가장 먼저 발견되는 속성정보를 사용하게 하는 방법이다.

```java
@Transactional
public interface UserService {
    void add(User user);
    void deleteAll();
    void update(User user);
    void upgradeLevels();

    @Transactional(readOnly=true)
    User get(String id);

    @Transactional(readOnly=true)
    List<User> getAll();
}
```

## 6.9 정리

- 트랜잭션 경계설정 코드를 분리해서 별도의 클래스로 만들고 비즈니스 로직 클래스와 동일한 인터페이스를 구현하면 DI의 확장 기능을 이용해 클라이언트의 변경 없이도 깔끔하게 분리된 트랜잭션 부가기능을 만들 수 있다
- 트랜잭션처럼 환경과 외부 리소스에 영향을 받는 코드를 분리하면 비즈니스 로직에만 충실한 테스트를 만들 수 있다
- 목 오브젝트를 이용하면 의존관계 속에 있는 오브젝트도 손쉽게 고립된 테스트로 만들 수 있다
- DI를 이용한 트랜잭션의 분리는 데코레이터 패턴과 프록시 패턴으로 이해될 수 있다
- 번거로운 프록시 클래스 작성은 JDK의 다이나믹 프록시를 사용하면 간단하게 만들 수 있다
- 다이나믹 프록시는 스태틱 팩토리 메소드를 사용하기 때문에 빈으로 등록하기 번거롭다. 따라서 팩토리 빈으로 만들어야 한다. 스프링은 자동 프록시 생성 기술에 대한 추상화 서비스를 제공하는 프록시 팩토리 빈을 제공한다
- 프록시 팩토리 빈의 설정이 반복되는 문제를 해결하기 위해 자동 프록시 생성기와 포인트컷을 활용할 수 있다. 자동 프록시 생성기는 부가기능이 담긴 어드바이스를 제공하는 프록시를 스프링 컨테이너 초기화 시점에 자동으로 만들어준다
- 포인트컷은 AspectJ 포인트컷 표현식을 사용해서 작성하면 편리하다
- AOP는 OOP만으로는 모듈화하기 힘든 부가기능을 효과적으로 모듈화하도록 도와주는 기술이다
- 스프링은 자주 사용되는 AOP 설정과 트랜잭션 속성을 지정하는 데 사용할 수 있는 전용 태그를 제공한다
- AOP를 이용해 트랜잭션 속성을 지정하는 방법에는 포인트컷 표현식과 메소드 이름 패턴을 이용하는 방법과 타깃에 직접 부여하는 @Transactional 애노테이션을 사용하는 방법이 있다
- @Transactional을 이용한 트랜잭션 속성을 테스트에 적용하면 손쉽게 DB를 사용하는 코드의 테스트를 만들 수 있다
