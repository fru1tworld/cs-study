# Ecto: Elixir 언어를 위한 Slick 쿼리 DSL

> 원문: https://blog.jooq.org/ecto-a-slick-query-dsl-for-the-elixir-language/

게시일: 2013년 12월 23일 | 저자: lukaseder

## 개요

이 블로그 게시물은 Elixir 프로그래밍 언어를 위한 데이터베이스 쿼리용 도메인 특화 언어인 Ecto를 소개합니다. 저자는 Elixir가 비교적 새로운 언어로, Stack Overflow 활동이 제한적이라고 언급합니다(2013년 12월 기준 약 50개의 태그된 질문).

## Elixir에 대하여

Elixir는 Ruby를 연상시키는 구문을 가진 프로그래밍 언어로 설명됩니다. 게시물에는 샘플 함수 정의가 포함되어 있습니다:

```elixir
defmodule Hello do
  IO.puts "Defining the function world"

  def world do
    IO.puts "Hello World"
  end
end
```

## Ecto의 목적

게시물에 따르면, "Ecto는 Elixir에서 쿼리를 작성하고 데이터베이스와 상호작용하기 위한 도메인 특화 언어입니다."

## 코드 예제

이 글에서는 여러 쿼리 패턴을 보여줍니다:

간단한 SELECT:
```elixir
use Ecto.Query

query = from w in Weather,
      where: w.prcp > 0 or w.prcp == nil,
     select: w

Repo.all(query)
```

JOIN 연산:
```elixir
query = from p in Post,
      where: p.id == 42,
  left_join: c in p.comments,
     select: assoc(p, c)

[post] = Repo.all(query)
post.comments.to_list
```

## 분석

저자는 Ecto를 LINQ와 Clojure의 sqlkorma 요소를 결합한 것으로 특징짓고, "slick(세련된)(하지만 타입 안전하지 않은?) 쿼리 DSL"로 설명합니다. 게시물은 Ecto가 Elixir 개발자들을 위한 주요 데이터베이스 접근 옵션을 대표한다고 제안하며 마무리됩니다.
