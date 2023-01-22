# 3장. DOM 이벤트 관리

지난 장에서는 데이터에 따른 DOM 엘레먼트를 어떻게 그리는지에 대해 알아봤다면, 이번 장에서는 화면을 동적으로 바꾸는 **이벤트**에 대해 알아본다.

## YAGNI 원칙

YANGI는 ‘You aren’t gonna need it(정말 필요할 때까지 기능을 추가하지 마라)’의 약자로, 익스트림 프로그래밍의 원칙 중 하나다. 프레임워크 없이 개발할 때에는 이 원칙이 절대적으로 중요하다. 아키텍처를 과도하게 엔지니어링하는 경우를 막기 위해서다.

## DOM 이벤트 API

이벤트는 웹 애플리케이션에서 발생하는 동작으로, 브라우저는 이벤트가 발생하면 사용자에게 알려주고 사용자는 이벤트에 대해 어떤 방식으로든 반응할 수 있다. 이벤트에는 다양한 종류가 있다. 전체 이벤트 리스트는 [여기](https://developer.mozilla.org/en-US/docs/Web/Events)서 확인 가능하다.

[Event reference | MDN](https://developer.mozilla.org/en-US/docs/Web/Events)

이벤트에 반응하기 위해선 이벤트를 트리거한 DOM 요소에 연결해야 한다. (뷰나 시스템 이벤트의 경우 이벤트 핸들러를 `window` 객체에 연결해야 한다.

## 속성에 핸들러 연결

### 비추: on* 속성으로 연결

```jsx
const button = document.querySelector('button');

button.onclick = () => {
	cnsole.log('Click managed using onclick property');
}
```

가장 빠른 핸들러 연결 방법은 익히 알고 있는 `onclick` 등의 `on*` 속성을 사용하는 방법이다. 이 방법은 굉장히 빠르고 편리하긴 하나 분명한 단점도 존재한다. `on*` 속성을 사용하면 한 번에 하나의 핸들러만 연결할 수 있고, 코드 내에서 `on*` 핸들러를 덮어쓰면 기존 핸들러는 없어진다.

### 권장: addEventListener로 연결

```jsx
const button = document.querySelector('button');

button.addEventListener('click', () => {
	console.log('First handler');
});

button.addEventListener('click', () => {
	console.log('Second Handler');
});
```

모든 DOM 노드는 `EventTarget`을 상속하기 때문에 `EventTarget`의 `addEventListener` 인터페이스를 구현할 수 있다. `addEventListener`는 `on*` 속성을 통한 핸들러 연결 방법과는 다르게 기존 핸들러를 삭제하지 않으면서 새로운 핸들러를 추가한다. 첫 번째 매개변수로 이벤트 타입을 넘기고, 두 번째 매개변수는 이벤트가 트리거될 때 호출될 함수다.

### 핸들러 삭제

DOM에 엘레먼트가 더 이상 존재하지 않으면 메모리 누수 방지를 위해 이벤트 리스너들도 삭제해줘야 한다.

```jsx
const button = document.querySelector('button');

const firstHandler = () => {
	console.log('First handler');
});

const secondHandler () => {
	console.log('Second Handler');
});

button.addEventListener('click', firstHandler);
button.addEventListener('click', secondHandler);

window.setTimeout(() => {
	button.removeEventListener('click', firstHandler);
	button.removeEventListener('click', secondHandler);
}, 1000);

```

### 이벤트 객체

이벤트 핸들러의 서명은 DOM 노드나 시스템에서 생성한 이벤트를 나타내는 매개변수(흔히 `e`로 표현하는)를 포함할 수 있다. 이 이벤트 객체에는 포인터 좌표, 이벤트 타입, 이벤트를 트리거한 요소 등의 정보가 담겨 있다.

![https://i.ibb.co/kxmQR3R/2023-01-22-8-15-24.png](https://i.ibb.co/kxmQR3R/2023-01-22-8-15-24.png)

웹 애플리케이션에 전달된 모든 이벤트는 `Event` 인터페이스를 구현한다. 이벤트 타입에 따라 Event 인터페이스를 확장하는 좀 더 구체적인 Event 인터페이스를 구현할 수 있다. 예를 들어 `click` 이벤트는 `MouseEvent` 인터페이스를 구현하며, 이 인터페이스에는 이벤트 중 포인터의 좌표나 이동에 대한 정보 등등 마우스를 사용했기 때문에 얻을 수 있는 이벤트 정보들을 담고 있다.

### DOM Event Lifecycle

앞서 소개한 `addEventListener` 메서드 예제에서는 2개의 매개변수만 사용했으나, 사실 이 메서드는 3개의 매개변수를 가진다.

```jsx
button.addEventListener('click', handler, false);
```

세 번째 매개변수는 `useCapture`라고 불리며 기본값은 `false`다. 이 매개변수는 옵셔널이긴 하지만, 폭넓은 브라우저 호환성을 얻으려면 포함시켜야 한다. 매개변수가 말하는 ‘이벤트를 캡쳐’한다는 것이 무엇인지 예시를 통해 알아보자.

```html
<body>
	<div>
		This is a container
		<button>Click Here</button>
	</div>
</body>
```

```jsx
const button = document.querySelector('button');
const div = document.querySelector('div');

div.addEventListener('click', () => {
	console.log('Div Clicked');
})

button.addEventListener('click', () => {
	console.log('Button Clicked');
})
```

div 안에 button이 있는 구조고, div와 button에 각각 클릭 이벤트 핸들러가 붙어있다. 이 때 버튼을 클릭했을 때 어떤 일이 일어날까? 이벤트 객체는 이벤트를 트리거한 DOM 노드에서부터 시작해 모든 조상 노드로 올라간다. 따라서 두 핸들러가 모두 실행되며 이 현상을 ‘버블 단계’나 ‘이벤트 버블링’이라고 한다. 버블 체인은 `Event` 인터페이스의 `stopPropagation` 메서드를 사용해서 멈출 수 있다.

```jsx
const button = document.querySelector('button');
const div = document.querySelector('div');

div.addEventListener('click', () => {
	console.log('Div Clicked');
})

button.addEventListener('click', (e) => {
	e.stopPropagation();
	console.log('Button Clicked');
})
```

이 코드에서 div 핸들러는 호출되지 않는다. 하지만 이 방법은 핸들러의 순서에 의존하기 때문에 코드의 유지보수가 어려워질 수 있다. 이런 경우 이벤트 위임(event delegation) 패턴이 유용할 수 있다. 이벤트 위임에 관해서는 뒤에서 다루겠다.

`useCapture` 매개변수를 사용해 핸들러의 실행 순서를 반대로 할 수 있다. 아래 예제를 실행하면 div 핸들러가 button 핸들러보다 먼저 실행된다. `**addEventListener`를 호출할 때 `useCapture` 값으로 `true`를 주면 버블 단계 대신 캡쳐 단계에 이벤트 핸들러를 추가하기 때문이다.** 

```jsx
const button = document.querySelector('button');
const div = document.querySelector('div');

div.addEventListener('click', () => {
	console.log('Div Clicked');
}, true)

button.addEventListener('click', (e) => {
	e.stopPropagation();
	console.log('Button Clicked');
}, true)
```

### 캡쳐링과 버블링

표준 DOM 이벤트에서 정의한 이벤트 흐름엔 다음 3단계가 있다:

1. 캡처링 단계 – 이벤트가 하위 요소로 전파되는 단계
2. 타깃 단계 – 이벤트가 실제 타깃 요소에 전달되는 단계
3. 버블링 단계 – 이벤트가 상위 요소로 전파되는 단계

예시로 테이블 안의 `<td>`를 클릭하면 어떤 일이 일어나는지 알아보며 이해해보자(원문 - [버블링과 캡쳐링](https://ko.javascript.info/bubbling-and-capturing)).

![https://i.ibb.co/DD2gtGn/2023-01-22-9-49-54.png](https://i.ibb.co/DD2gtGn/2023-01-22-9-49-54.png)

`<td>`를 클릭하면 이벤트가 최상위 조상에서 시작해 아래로 전파되고(캡처링 단계), 이벤트가 타깃 요소에 도착해 실행된 후(타깃 단계), 다시 위로 전파된다(버블링 단계). 이런 과정을 통해 요소에 할당된 이벤트 핸들러가 호출된다.

버블 단계에서는 핸들러가 bottom-up(상향식)으로 처리되는 반면 캡쳐 단계에서는 반대로 처리된다. 시스템은 `<html>` 태그에서 핸들러 관리를 시작하고 이벤트를 트리거한 요소를 만날 때까지 내려간다. 생성된 모든 DOM 이벤트에 대해 브라우저는 캡쳐 단계(하향식)를 실행한 다음 버블 단계(상향식)를 실행한다는 걸 명심하자.

## 이벤트 위임

이벤트 위임은 `event.target`을 통해 실제 어디서 이벤트가 발생했는지 알 수 있다는 점을 이용해 부모 엘레먼트에 이벤트 핸들러를 하나만 할당하면서 자식 엘레먼트를 한꺼번에 다룰 수 있는 방법이다. 여기서는 책에 있는 코드 대신 자바스크립트 튜토리얼에 있는 예제 코드를 사용하겠다. 아래와 같은 팔괘도 테이블 코드가 있고, 우리가 하고 싶은 것은 `<td>` 를 클릭했을 때 그 칸을 강조하는 것이다.

```jsx
<table>
  <tr>
    <th colspan="3"><em>Bagua</em> Chart: Direction, Element, Color, Meaning</th>
  </tr>
  <tr>
    <td class="nw"><strong>Northwest</strong><br>Metal<br>Silver<br>Elders</td>
    <td class="n">...</td>
    <td class="ne">...</td>
  </tr>
  <tr>...2 more lines of this kind...</tr>
  <tr>...2 more lines of this kind...</tr>
</table>
```

각 `<td>`마다 이벤트 핸들러를 할당하는 대신, 모든 이벤트를 잡아내는 핸들러를 `<table>`에 할당해 보자.

```jsx
let selectedTd;

table.onclick = function(event) {
  let target = event.target; // 클릭이 어디서 발생했을까요?
  if (target.tagName != 'TD') return; // TD에서 발생한 게 아니라면 아무 작업도 하지 않습니다,
  highlight(target); // 강조 함
};

function highlight(td) {
  if (selectedTd) { // 이미 강조되어있는 칸이 있다면 원상태로 바꿔줌
    selectedTd.classList.remove('highlight');
  }
  selectedTd = td;
  selectedTd.classList.add('highlight'); // 새로운 td를 강조 함
}
```

이렇게 코드를 작성하면 테이블 내 `<td>`의 개수를 신경쓰지 않으면서 원하는 기능을 구현할 수 있다. 하지만 이 코드는 클릭 이벤트가 `<td>`가 아닌 `<td>`의 내부에서 발생하는 경우를 잡아내지 못한다. `<td>` 안의 `<strong>` 을 클릭하게 되면 `target`에 td가 아닌 strong이 저장되어 우리가 의도한 대로 동작하지 않는다. 모든 경우에서 작동이 잘 되도록 하기 위해서는 코드를 아래처럼 수정해 보자.

```jsx
table.onclick = function(event) {
  let td = event.target.closest('td'); // (1)
  if (!td) return; // (2)
  if (!table.contains(td)) return; // (3)
  highlight(td); // (4)
};
```

설명:

1. 현재 이벤트가 발생한 엘레먼트로부터 가장 가까운 조상 `<td>` 엘레먼트를 찾는다.
2. 이벤트가 발생한 곳이 `<td>` 안이 아닌 경우 아무 일도 일어나지 않는다.
3. 중첩 테이블인 경우 이벤트가 발생한 곳이 팔괘도 테이블 바깥에 있는 `<td>`일 수 있다. 이 경우 아무 일도 일어나지 않는다.
4. `<td>`를 강조한다.

이벤트 위임을 활용하는 방법 등 더 자세한 정보를 알고 싶다면 [자바스크립트 튜토리얼의 이벤트 위임 전문](https://ko.javascript.info/event-delegation)을 읽어보자.
