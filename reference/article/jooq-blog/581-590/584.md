# 재미있는 메트릭: 가장 많이 사용되는 Java 키워드

> 원문: https://blog.jooq.org/silly-metrics-the-most-used-java-keywords/

말해보세요...

- 실제로 몇 번이나 무언가를 "synchronized" 했는지 궁금해본 적 없으신가요?
- "do {} while ()" 루프 구조를 충분히 자주 사용하지 않는 것에 대해 걱정해본 적 없으신가요?
- "volatile"을 적용하는 전문가이신가요?
- "try"보다 "catch"를 더 자주 하시나요?
- 여러분의 프로그램은 "true"에 가깝나요 아니면 "false"에 가깝나요?
- 그리고 어떻게 그 ["goto"](https://blog.jooq.org/rare-uses-of-a-controlflowexception/)가 소스 코드에 들어가게 된 건가요??

최근에 제가 작성한 다른 유익한 글들 사이에서 약간의 기분 전환을 위한 글입니다. [jOOQ](https://www.jooq.org)에서 가장 많이 사용된 Java 키워드의 완전히 쓸모없는 랭킹입니다. 결국, 유용한 메트릭은 이미 [ohloh](http://www.ohloh.net/p/jooq)에서 검토하거나, [FindBugs](http://findbugs.sourceforge.net/)와 [JArchitect](http://www.jarchitect.com/Metrics.aspx)로 수집할 수 있으니까요.

자, 이제 확인해보세요. 여기 랭킹이 있습니다!

```
Keyword      Count
public       8127
return       6801
final        6608
import       5938
static       3903
new          3110
extends      2111
int          1822
throws       1756
void         1707
if           1661
this         1464
private      1347
class        1239
case         841
else         839
package      711
boolean      506
throw        495
for          421
long         404
true         384
byte         345
interface    337
false        332
protected    293
super        265
break        200
try          149
switch       146
implements   139
catch        127
default      112
instanceof   107
char         96
short        91
abstract     54
double       43
transient    42
finally      34
float        34
enum         25
while        23
continue     12
synchronized 8
volatile     6
do           1
```

여러분 자신의 Java 키워드 랭킹이 궁금하신가요? 이 값들을 계산하는 스크립트를 GitHub에 ASL 2.0 라이선스로 공개했습니다. 여기서 소스를 확인하세요:

https://github.com/lukaseder/silly-metrics

사용해보시고, 여러분만의 랭킹을 공개하세요! 그리고 다른 언어의 키워드를 카운트하거나, 완전히 다른 재미있고 쓸모없는 메트릭을 계산하는 풀 리퀘스트를 자유롭게 제공해주세요.
