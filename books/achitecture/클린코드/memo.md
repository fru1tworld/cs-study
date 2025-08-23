# Chapter1 깨끗한 코드

- 르블랑의 법칙: 나중은 결코 오지 않는다.

나쁜 코드의 위험을 이해하지 못하는 관리자의 말을 그대로 따르는 행동은 전문가답지 못하다.

코드 감각이 있는 프로그래머는 나쁜 모듈을 좋은 모듈로 개발할 방안을 떠올린다.

코드를 짜는 시간보다 읽는 시간이 더 길다. 따라서 끊임없이 코드를 읽는데 가독성이 매우 중요하다는 것을 알 수 있다.

# Chapter2 의미 있는 이름

의도가 분명해야한다.

```java
public List<int []> getThem(){
    List<int []> list1 = new ArrayList<int []>();
    for(int[] x : theList)
        if(x[0] == 4)
            list1.add(X);
    return list1;
}
```

위 코드의 역할은 무엇일까 ?

```java
public List<int []> getFlaggedCells(){
    List<int []> flaggedCells = new ArrayList<int []>();
    for(int[] cell gameBoard)
        if(ceel[STATUS_VALUE] == FALGGED)
            flaggedCells.add(cell);
    return list1;
}
```

이름만으로 역할을 파악할 수 있다.

```java
public List<Cell> getFlaggedCells(){
    List<Cell> flaggedCells = new ArrayList<int []>();
    for(Cell cell gameBoard)
        if(cell.isFlagged())
            flaggedCells.add(cell);
    return list1;
}
```

int 배열 대신 클래스를 정의하고 함수를 사용해서 상수를 감춰도 좋겠다.

### 그릇된 정보를 피하자

hp, aix, sco는 변수 이름으로 적합하지 않은데 유닉스 플랫폼이나 유닉스 변종을 가리키는 이름이고, 실제 List가 아니라면 List는 특수한 의미이므로 그릇된 정보를 제공할 수 있음

유사한 개념은 유사한 표기법을 사용한다.
그릇된 정보의 예 소문자 L과 대문자 O

```java
int a = 1;
if( O == l)
a = 01;
else
l = 01;
```

위에 코드 지어낸게 아니라 실화임

### 의미 있게 구분 하라

```java
public static void copyChars(char a1[], char a2[]){
    for(int i=0; i<a1.length; i++){
        a2[i] = a1[i];
    }
}
```

불용어 사용을 지양하자 불용어 ? noise word: 검색엔진이 검색에서 무시해버리는 단어 또는 검색엔진이 데이터베이스를 구축할때 색인에서 제외해버리는 단어
a1, a2가 아니라 source, destination이라고 하면 쉽겠다

```java

getActiveAccount();
getActiveAccounts();
getActiveAccountInfo();
```

구분이 되는가 ?

### 발음하기 쉬운 이름을 사용하라

```java
class DtaRcrd102{
    private Data genymhms;
    private DAta modymdhms;
    private final String pszqint = "102"
}
```

```java
class DtaRcrd102{
    private Data generationTimestamp;
    private DAta modificationTimestamp;
    private final String recordId = "102"
}
```

### 검색하기 쉬운 이름 쓰기

```java
for (int j=0; j<34; j++>{
    s += (t[j]*4)/5;
})
```

```java
int realDaysPerIdealDay =4;
const int WORK_DAYS_PER_WEEK = 5;
int sum =0;
for(int j=0; j < NUMBER_OF_TASKS; j++){
    int realTaskDays = taskEstimate[j] * realDaysPerIdealdDay;
    int realTaskWeeks = (realTaskDays /WORK_DAYS_PER_WEEK);
    sum += realTaskWeeks;
}
```

위 코드에서 sum이 별로 유용하지 않으나 최소한 검색이 가능하다.

### 말장난 금지

add는 덧셈이 될수도 있음

집합에 값 하나를 추가한다도 될 수 있음 이 경우에은 insert, append이 적당한듯

### 맥락을 넣자

finestName, lastName,state를 생각해보면 state는 주소라는 것을 파악할 수 있음

근데 state 단독이면 파악하기 힘듬 근데 addrFirstName, addrLastName 같은 맥락을 쓰면 좀 더 맥락이 분명해진다.

물론 Address라는 클래스를 생성하면 더 좋다.

# Chapter3 함수

# Chapter4 주석

# Chapter5 형식 맞추기

# Chapter6 객체와 자료구조

# Chapter7 오류 처리

# Chapter8 경계

# Chapter9 단위 테스트

# Chapter10 클래스

# Chapter11 시스템

# Chapter12 창발성

# Chapter13 동시성

# Chapter14 점진적 개선

# Chapter15 JUnit 들여다보기

# Chapter16 SerialData 리팩터링

# Chapter17 냄새와 휴리스틱

# 부록 A 동시성 II

# 부록 B org.jfree.data.SerialData

# 부록 C 휴리스틱의 교차 참조 목록

```

```
