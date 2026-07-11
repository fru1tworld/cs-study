# 시맨틱 압축 (Semantic Compression)

> 원문: [Semantic Compression](https://caseymuratori.com/blog_0015)
> 저자: Casey Muratori | 작성일: 2014-05-28 (The Witness 개발 블로그) | 정리일: 2026-07-10

원문은 저작권이 유보된 개인 블로그 글이므로, 아래는 원문의 섹션 구조를 그대로 따라가며 내용을 상세히 재서술한 학습 노트다. 코드 예제는 원문 그대로 수록했고, 인용은 소량만 담았다.

## 도입: OOP식 설계의 풍자

글은 전형적인 객체지향 설계 과정을 풍자하며 시작한다. 급여 시스템을 만든다고 하자. OOP 교과서식 접근은 이렇게 흘러간다.

1. 현실 문제에서 명사를 찾는다: "직원(employee)", "관리자(manager)". 각각 클래스로 만든다.
2. 둘 다 사람이니 `Person` 기반 클래스를 만들어 상속시킨다.
3. 그런데 관리자도 직원이니 `Manager`가 `Employee`를 상속하고, `Employee`가 `Person`을 상속하도록 바꾼다. 코드는 한 줄도 안 썼지만 "객체를 모델링했으니 코드는 저절로 써질 것"이라 믿는다.
4. 계약직(contractor)이 등장한다. 계약직은 직원이 아니므로 `Contractor` 클래스가 `Person`을 상속한다.
5. 그러면 `Manager`는 무엇을 상속해야 하나? `Employee`를 상속하면 계약직 관리자를 표현할 수 없고, `Contractor`를 상속하면 정규직 관리자를 표현할 수 없다.
6. 다중 상속은 타입 안전하지 않으니, 결국 `Manager`를 기반 클래스에 대해 템플릿화하고, 관리자를 다루는 모든 코드도 템플릿화한다. 이제 UML 다이어그램을 그리러 간다.

저자는 이것이 과장이 아니라, 유명한 강연자와 저술가를 포함해 실제로 이렇게 사고하는 프로그래머가 많다고 지적한다. 본인도 18세에 OOP를 접해 24세가 되어서야 그것이 전부 헛소리임을 깨달았다고 고백한다. 그 깨달음에는 OOP를 받아들이지 않았던 RAD Game Tools 입사가 크게 작용했다고 한다.

## 프로그래머가 프로그래밍에 관해 글을 써야 하는 이유

많은 숙련 프로그래머가 이런 나쁜 시기를 거쳐 좋은 코드를 쓰는 법을 스스로 터득하지만, 세상의 교육 자료는 여전히 압도적으로 "객관적으로 나쁜" 쪽에 몰려 있다. 저자의 추측으로는, 좋은 프로그래밍은 일단 할 줄 알면 너무 당연해 보여서 — 화려한 수학 기법과 달리 — 굳이 글로 쓸 만한 것이 아니라고 느끼기 때문이다.

하지만 써야 한다. 좋은 프로그래머들이 자기 방식을 공유하지 않으면, 모두가 6년씩 끔찍한 객체지향 프로그램을 쓰며 시간을 낭비한 뒤에야 깨닫는 상황이 반복된다. 그래서 저자는 이 연재에서 화려한 알고리즘이나 수학이 아닌, 순수하게 "코드를 컴퓨터에 넣는 기계적 과정" — 배관 작업 — 만을 다루겠다고 선언한다. 첫 소재는 The Witness 에디터 코드에 실제로 적용한 일련의 코드 변환이다.

## Jon의 출발점: 좋은 원본 코드

The Witness 내장 에디터에는 "Movement Panel"이라는 UI가 있다. 엔티티에 "90도 회전" 같은 조작을 하는 버튼들이 달린 떠 있는 창이다. 저자가 여기에 기능을 대량 추가해야 했는데, UI에 요소를 추가해 본 적이 없어 기존 코드를 살펴봤다. 원래 코드는 이랬다.

```cpp
int num_categories = 4;
int category_height = ypad + 1.2 * body_font->character_height;
float x0 = x;
float y0 = y;
float title_height = draw_title(x0, y0, title);
float height = title_height + num_categories * category_height + ypad;
my_height = height;
y0 -= title_height;

{
    y0 -= category_height;
    char *string = "Auto Snap";
    bool pressed = draw_big_text_button(x0, y0, my_width, category_height, string);
    if (pressed) do_auto_snap(this);
}

{
    y0 -= category_height;
    char *string = "Reset Orientation";
    bool pressed = draw_big_text_button(x0, y0, my_width, category_height, string);
    if (pressed) {
        // ...
    }
}
// ...
```

저자는 원작자 Jon(Jonathan Blow)이 일을 아주 잘 해놨다고 평가한다. 흔히 이런 코드를 열어 보면 불필요한 구조와 간접 참조가 뒤엉켜 있기 마련인데, 여기는 사람에게 UI 그리는 법을 설명하듯 일이 일어나는 순서 그대로 읽힌다. "먼저 제목 표시줄 위치를 계산하고, 제목을 그리고, 그 아래에 Auto Snap 버튼을 그리고, 눌렸으면 자동 스냅을 실행하고…" 누구든 이 발췌만 읽고도 버튼을 추가하는 법을 직관적으로 알 수 있다. 프로그래밍은 이래야 한다는 것이다.

다만 레이아웃 계산을 전부 손으로 인라인으로 하고 있어서, UI가 많아지면 부담이 커진다. 한 줄에 버튼 4개가 놓이는 다음 코드를 보면 문제가 더 분명해진다.

```cpp
{
    y0 -= category_height;

    float w = my_width / 4.0f;
    float x1 = x0 + w;
    float x2 = x1 + w;
    float x3 = x2 + w;

    unsigned long button_color;
    unsigned long button_color_bright;
    unsigned long text_color;

    get_button_properties(this, motion_mask_x, &button_color, &button_color_bright, &text_color);
    bool x_pressed = draw_big_text_button(x0, y0, w, category_height, "X", button_color, button_color_bright, text_color);

    get_button_properties(this, motion_mask_y, &button_color, &button_color_bright, &text_color);
    bool y_pressed = draw_big_text_button(x1, y0, w, category_height, "Y", button_color, button_color_bright, text_color);

    get_button_properties(this, motion_mask_z, &button_color, &button_color_bright, &text_color);
    bool z_pressed = draw_big_text_button(x2, y0, w, category_height, "Z", button_color, button_color_bright, text_color);

    get_button_properties(this, motion_local, &button_color, &button_color_bright, &text_color);
    bool local_pressed = draw_big_text_button(x3, y0, w, category_height, "Local", button_color, button_color_bright, text_color);

    if (x_pressed) motion_mask_x = !motion_mask_x;
    if (y_pressed) motion_mask_y = !motion_mask_y;
    if (z_pressed) motion_mask_z = !motion_mask_z;
    if (local_pressed) motion_local = !motion_local;
}
```

버튼을 대량으로 추가하기 전에, 새 것을 추가하기 "더 간단하게" 만들 필요가 있었다. 그런데 여기서 "간단하다"는 게 무슨 뜻이고, 그걸 어떻게 판단하는가?

## 시맨틱 압축이란

저자는 프로그래밍을 본질적으로 두 부분으로 본다. 첫째, 프로세서가 실제로 무엇을 해야 하는지 알아내기. 둘째, 그것을 사용 언어로 가장 효율적으로 표현하는 방법 찾기. 프로그래머의 시간 대부분은 갈수록 후자 — 알고리즘과 수학을 자기 무게에 무너지지 않는 일관된 전체로 엮어내는 일 — 에 쓰인다.

여기서 "효율적"이란 코드가 최적화됐다는 뜻이 아니라 **개발이 최적화됐다**는 뜻이다. 코드를 타이핑하고, 동작시키고, 수정하고, 출시 가능할 만큼 디버깅하는 데 드는 인간의 노력을 최소화하도록 코드가 구조화되어야 한다. 효율은 최대한 전체론적으로 봐야 한다. 코드가 처음 만들어진 순간부터 누군가 마지막으로 사용하는 순간까지 — 타이핑 시간, 디버깅 시간, 수정 시간, 다른 용도로 적응시키는 시간, 이 코드와 맞물리게 하려고 다른 코드에 들인 작업까지 — 전 생애에 걸친 인간 노력의 총합을 최소화하는 것이 목표다.

이렇게 볼 때, 저자의 경험이 이끈 결론은 이렇다. 가장 효율적인 프로그래밍 방법은 **자기 자신을 사전(dictionary) 압축기라고 생각하고 코드에 접근하는 것**이다. 문자 그대로, 아주 뛰어난 PKZip이 코드 위에서 계속 돌면서 코드를 (시맨틱하게) 더 작게 만들 방법을 찾는다고 상상하라. 여기서 "시맨틱하게 작다"는 것은 텍스트 분량이 아니라 중복되거나 유사한 코드가 적다는 뜻이다(둘은 자주 함께 가긴 한다).

이것은 철저히 상향식(bottom-up) 방법론이다. 유사 변종이 최근 "리팩터링"이라는 이름을 얻었지만, 저자는 그 용어도 우스꽝스럽고 공식적인 "리팩터링" 담론은 핵심을 놓쳤다고 본다.

압축 지향 프로그래밍의 원칙은 다음과 같다.

- **좋은 압축기처럼, 무언가가 두 번 나타나기 전에는 재사용하지 않는다.** 처음부터 "재사용 가능한" 코드를 쓰려는 것은 가장 큰 실수 중 하나다. 저자의 신조는 원문 그대로 옮기면 "make your code usable before you try to make it reusable" — 재사용 가능하게 만들기 전에 먼저 사용 가능하게 만들라는 것이다.
- 항상 각 구체적 사례에서 일어나야 할 일을 "정확성"이나 "추상화" 같은 유행어를 신경 쓰지 않고 그대로 타이핑해서 일단 동작시킨다. 그러다 다른 곳에서 같은 일을 두 번째로 하게 될 때, 그때 재사용 가능한 부분을 뽑아내 공유한다. 이것이 코드를 "압축"하는 것이다. "추상화"라는 흔한 표현보다 "압축"이 더 나은 비유인 이유는, 압축은 유용한 무언가를 의미하기 때문이다. 코드가 추상적이든 말든 누가 신경 쓰는가?
- 최소 두 개의 실제 사례가 생길 때까지 기다리면, 정말 필요해질 때까지 재사용을 고민하는 시간을 아낄 뿐 아니라, 재사용 코드가 감당해야 할 실제 예시를 항상 두 개 이상 확보하게 된다. 예시가 하나뿐이거나, 더 나쁘게는 (선제적으로 짠 코드처럼) 하나도 없으면 잘못 만들 가능성이 크고, 나중에 쓰려 할 때 불편하거나 다시 만들어야 해서 시간이 더 낭비된다. Knuth를 빗대어, 코드를 "조급하게 재사용 가능하게(prematurely reusable)" 만들지 않으려고 애쓴다.
- 전역 최적화를 하는 마법의 압축기처럼, 이미 재사용 중인 코드를 또 쓸 수 있는 새 자리가 나타나면 판단한다. 그대로 맞으면 그냥 쓰고, 안 맞으면 동작을 수정할지, 위나 아래에 새 계층을 추가할지 결정한다. (다중 해상도 진입점(multiresolution entry points)은 별도 주제라 후속 글로 미룬다.)

이 모든 것의 밑바탕 가정: 코드를 잘 압축해 컴팩트하게 만들면 양이 최소이므로 읽기 쉽고, 자주 표현되는 것들이 자기 이름을 얻어 일관되게 쓰이므로 시맨틱이 문제의 실제 "언어"를 닮게 된다. 같은 일을 하는 곳은 모두 같은 경로를 지나므로 유지보수가 쉽고, 필요한 코드가 재조합하기 좋은 형태로 갖춰져 있으므로 확장도 쉽다.

UML 다이어그램, 클래스 계층, 객체 시스템 같은 방법론들도 전부 이런 것을 해주겠다고 추상적으로 주장하지만 늘 실패한다. 코드의 어려운 부분은 디테일을 맞추는 것인데, 디테일이 없는 지점에서 출발하면 필연적으로 무언가를 빠뜨려 계획이 무너지거나 차선의 결과에 이른다. 반대로 디테일에서 출발해 반복적으로 압축하며 아키텍처에 도달하면, 아키텍처를 미리 구상하려 할 때의 함정을 전부 피할 수 있다.

## 공유 스택 프레임 (Shared Stack Frames)

첫 번째 압축은 저자가 가장 좋아하는 종류다. 하기는 쉬운데 만족감이 크다.

C++에서 함수는 이기적이라, 지역 변수를 저 혼자 끌어안고 있고 그걸 어찌할 방법이 없다. 그래서 이런 코드를 보면,

```cpp
int category_height = ypad + 1.2 * body_font->character_height;
float y0 = y;
// ...
y0 -= category_height;
// ...
y0 -= category_height;
// ...
y0 -= category_height;
// ...
```

"공유 스택 프레임"을 만들 때가 됐다고 판단한다. The Witness에서 패널 UI가 있는 곳이면 어디든 이런 일이 벌어질 것이고, 실제로 에디터의 다른 패널들도 사실상 똑같은 시작 코드와 버튼 계산 코드를 갖고 있었다. 각 일이 한 곳에서만 일어나고 나머지는 그걸 쓰게 압축하고 싶다.

그런데 순수하게 함수로만 감싸기는 어렵다. 서로 상호작용하는 변수들의 시스템이 있고, 그 상호작용이 서로 연결되어야 하는 여러 지점에서 일어나기 때문이다. 그래서 첫 단계로, 이 변수들을 구조체로 뽑아냈다. 나중에 이 작업들을 별도 함수로 나누고 싶을 때 일종의 공유 스택 프레임 역할을 할 수 있다.

```cpp
struct Panel_Layout
{
    float width;      // "my_width"에서 이름 변경
    float row_height; // "category_height"에서 이름 변경
    float at_x;       // "x0"에서 이름 변경
    float at_y;       // "y0"에서 이름 변경
};
```

반복적으로 쓰이는 변수들을 골라 구조체에 넣으면 끝이다. (저자는 평소 변수에 InterCaps, 타입에 lowercase_with_underscores를 쓰지만, Witness 코드베이스의 관례인 타입 Uppercase_With_Underscores, 변수 lowercase_with_underscores를 따랐다.)

구조체를 지역 변수 자리에 대입하고 나면 코드는 이렇다.

```cpp
Panel_Layout layout;
int num_categories = 4;
layout.row_height = ypad + 1.2 * body_font->character_height;
layout.at_x = x;
layout.at_y = y;
float title_height = draw_title(layout.at_x, layout.at_y, title);
float height = title_height + num_categories * layout.row_height + ypad;
my_height = height;
layout.at_y -= title_height;

{
    layout.at_y -= layout.row_height;
    char *string = "Auto Snap";
    bool pressed = draw_big_text_button(layout.at_x, layout.at_y, layout.width, layout.row_height, string);
    if (pressed) do_auto_snap(this);
}

{
    layout.at_y -= category_height;
    char *string = "Reset Orientation";
    bool pressed = draw_big_text_button(layout.at_x, layout.at_y, layout.width, layout.row_height, string);
    if (pressed) {
        // ...
    }
}
// ...
```

아직 개선은 아니지만 필수적인 첫 단계다. 다음으로 중복 코드를 함수로 뽑았다. 시작 시점에 하나, UI에 새 행이 생길 때마다 하나. (저자라면 보통 멤버 함수로 만들지 않겠지만, Witness가 자기 코드보다 C++스러운 코드베이스라 스타일 일관성을 위해 멤버 함수로 했다. 어느 쪽이든 강한 선호는 없다.)

```cpp
Panel_Layout::Panel_Layout(Panel *panel, float left_x, float top_y, float width)
{
    row_height = panel->ypad + 1.2 * panel->body_font->character_height;
    at_y = top_y;
    at_x = left_x;
}

void Panel_Layout::row()
{
    at_y -= row_height;
}
```

구조체가 생기고 나니 원본의 이 두 줄을

```cpp
float title_height = draw_title(x0, y0, title);
y0 -= title_height;
```

감싸는 것도 간단했다.

```cpp
void Panel_Layout::window_title(char *title)
{
    float title_height = draw_title(at_x, at_y, title);
    at_y -= title_height;
}
```

그 결과 코드는 이렇게 됐다.

```cpp
Panel_Layout layout(this, x, y, my_width);
layout.window_title(title);

int num_categories = 4;
float height = title_height + num_categories * layout.row_height + ypad;
my_height = height;

{
    layout.row();
    char *string = "Auto Snap";
    bool pressed = draw_big_text_button(layout.at_x, layout.at_y, layout.width, layout.row_height, string);
    if (pressed) do_auto_snap(this);
}

{
    layout.row();
    char *string = "Reset Orientation";
    bool pressed = draw_big_text_button(layout.at_x, layout.at_y, layout.width, layout.row_height, string);
    if (pressed) {
        // ...
    }
}
// ...
```

패널이 이것 하나뿐이었다면 굳이 뽑을 필요가 없었겠지만(코드가 한 번만 나오므로), Witness의 모든 UI 패널이 같은 일을 하고 있었기에, 뽑아 두면 그 코드들도 전부 압축할 수 있었다(실제로 했지만 이 글에서는 다루지 않는다).

다음은 어색한 `num_categories`와 높이 사전 계산 제거. 이 코드가 하는 일은 결국 모든 행을 배치한 뒤 패널이 얼마나 높아질지를 미리 세어 두는 것뿐이었다. 굳이 앞에서 선언할 이유가 없으니, 행이 다 만들어진 뒤에 실제로 몇 개 추가됐는지 세면 된다. 그러면 두 값이 어긋날 수 없으므로 오류 가능성도 줄어든다. 그래서 패널 레이아웃 끝에 실행하는 `complete` 함수를 추가했다.

```cpp
void Panel_Layout::complete(Panel *panel)
{
    panel->my_height = top_y - at_y;
}
```

생성자에서 시작 y를 `top_y`로 저장해 두게 하면, 두 값을 빼기만 하면 된다. 사전 계산이 사라졌다.

```cpp
Panel_Layout layout(this, x, y, my_width);
layout.window_title(title);

{
    layout.row();
    char *string = "Auto Snap";
    bool pressed = draw_big_text_button(layout.at_x, layout.at_y, layout.my_width, layout.row_height, string);
    if (pressed) do_auto_snap(this);
}

{
    layout.row();
    char *string = "Reset Orientation";
    bool pressed = draw_big_text_button(layout.at_x, layout.at_y, layout.my_width, layout.row_height, string);
    if (pressed) {
        // ...
    }
}

// ...
layout.complete(this);
```

코드가 꽤 간결해졌지만, 반복되는 `draw_big_text_button` 호출에서 압축 여지가 더 보인다. 그것도 뽑아냈다.

```cpp
bool Panel_Layout::push_button(char *text)
{
    bool result = panel->draw_big_text_button(at_x, at_y, width, row_height, text);
    return(result);
}
```

이제 코드가 상당히 컴팩트해졌다.

```cpp
Panel_Layout layout(this, x, y, my_width);
layout.window_title(title);

{
    layout.row();
    char *string = "Auto Snap";
    bool pressed = layout.push_button(string);
    if (pressed) do_auto_snap(this);
}

{
    layout.row();
    char *string = "Reset Orientation";
    bool pressed = layout.push_button(string);
    if (pressed) {
        // ...
    }
}

// ...
layout.complete(this);
```

마지막으로 불필요한 장황함을 걷어내 다듬었다.

```cpp
Panel_Layout layout(this, x, y, my_width);
layout.window_title(title);

layout.row();
if(layout.push_button("Auto Snap")) {do_auto_snap(this);}

layout.row();
if(layout.push_button("Reset Orientation"))
{
    // ...
}

// ...
layout.complete(this);
```

원본과 비교하면 신선한 공기 같다. Movement Panel 고유의 UI를 정의하는 데 실제로 필요한 최소한의 정보에 가까워지고 있고, 이것이 압축이 잘 되고 있다는 신호다. 버튼 추가도 아주 단순해졌다. 인라인 수학 없이, 행 하나 만드는 호출과 버튼 하나 만드는 호출이면 된다.

### 이것이 "객체"가 태어나는 올바른 방식이다

여기서 저자가 정말 강조하고 싶은 지점. 지금까지의 과정이 꽤 뻔해 보였을 것이다. "대체 어떻게 한 거지??" 싶은 단계는 하나도 없었을 것이고, 공통 코드를 함수로 뽑으라는 과제를 받았다면 누구나 비슷하게 했을 것이다.

바로 그렇기 때문에, **이것이 "객체"를 낳는 올바른 방법이다.** 실제로 사용 가능한 코드와 데이터의 묶음 — `Panel_Layout` 구조체와 그 멤버 함수들 — 이 만들어졌다. 원하는 일을 정확히 하고, 완벽하게 들어맞고, 쓰기 쉽고, 설계는 하나도 어렵지 않았다.

이를 인덱스 카드에 뭔가를 적으라는 "class responsibility collaborators" 방법론이나, Visio로 상자와 선을 그려 "상호작용"을 표현하라는 객체지향 "방법론"들의 부조리와 대조해 보라. 그런 방법론에 몇 시간을 쓰고 나면 시작할 때보다 문제가 더 헷갈리게 된다. 그런 것을 전부 잊고 단순한 코드를 쓰면, 객체는 언제나 사후에 만들어낼 수 있고, 그렇게 만든 객체는 정확히 원하던 그 모습이다.

저자는 "객체"가 무엇이고 무엇이 어디에 들어가야 하는지 생각하는 데 정확히 0의 시간을 쓴다고 말한다. "객체지향 프로그래밍"의 오류는 바로 코드가 조금이라도 "객체지향적"이라는 그 전제다. 코드는 절차 지향적이며, "객체"란 절차의 재사용을 가능하게 하려고 자연스럽게 생겨나는 구조물일 뿐이다. 모든 것을 거꾸로 강제하는 대신 그것이 저절로 일어나게 두면, 프로그래밍은 훨씬 즐거워진다.

## 더 압축하고, 그다음 확장으로

압축 지향 프로그래밍 개념을 소개하고 OOP를 실컷 비판하느라, Witness UI 코드에 한 변환의 일부만 보여줬는데도 글이 이미 길어졌다. 다음 주 글에서는 앞서 보여준 다중 버튼 코드를 다루고, 새로 압축된 UI 시맨틱으로 UI 자체의 기능을 확장하기 시작한 과정을 이야기하겠다고 예고하며 글을 맺는다.

(저자의 근황과 현재 프로젝트는 computerenhance.com에서 볼 수 있다.)
