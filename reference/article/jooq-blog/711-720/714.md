# Java에서 CSS 선택자

> 원문: https://blog.jooq.org/css-selectors-in-java/

CSS 선택자는 DOM 탐색을 위한 XPath의 훌륭하고 직관적인 대안입니다.

XPath가 더 완전하고 더 많은 기능을 갖추고 있지만, CSS 선택자는 HTML DOM에 맞게 설계되었으며, HTML에서는 문서 콘텐츠가 보통 XML보다 덜 구조적입니다.

다음은 CSS 선택자와 동등한 XPath 표현식의 몇 가지 예시입니다:

```
CSS:   document > library > books > book
XPath: //document/library/books/book

CSS:   document book
XPath: //document//book

CSS:   document book#id3
XPath: //document//book[@id='3']

CSS:   document book[title='CSS for dummies']
XPath: //document//book[@title='CSS for dummies']
```

이것은 XPath에서 의사 선택자(pseudo-selector)를 구현할 때 더 흥미로워집니다:

```
CSS:   book:first-child
XPath: //book[not(preceding-sibling::*)]

CSS:   book:empty
XPath: //book[not(*|@*|node())]
```

w3c 명세에 따라 선택자 표현식을 파싱할 수 있는 아주 훌륭한 라이브러리는 Christer Sandberg의 "css-selectors"입니다: https://github.com/chrsan/css-selectors

다음 버전의 jOOX에는 더 간단한 DOM 탐색을 위해 css-selector의 파서가 포함될 예정입니다.

다음 두 표현식은 동일한 결과를 반환합니다:

```java
Match match1 = $(document).find("book:empty");
Match match2 = $(document).xpath("//book[not(*|@*|node())]");
```
