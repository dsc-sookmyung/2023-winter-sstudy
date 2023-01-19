# 2장. 렌더링

# 문서 객체 모델(DOM)

문서 객체 모델(DOM)은 프로그래밍 방식으로 엘레먼트를 렌더링하는 방식으로, 우리는 DOM을 통해 웹 애플리케이션을 구성하는 엘레먼트들을 조작할 수 있다. 우리가 보는 HTML은 모두 DOM 트리 형식으로 표현할 수 있는데, 이 트리의 노드에 `querySelector` 등의 메서드로 접근하여 속성을 변경할 수 있다.

# 렌더링 성능 모니터링

여기선 렌더링 엔진의 성능을 모니터링하는 여러 도구를 살펴본다.

## 구글 개발자 도구

![https://i.ibb.co/xXwMddb/2023-01-15-3-30-25.png](https://i.ibb.co/xXwMddb/2023-01-15-3-30-25.png)

개발자 도구를 연 다음 `cmd(ctrl)+shift+P`를 누르고 `Show frame per seconds (FPS) meter` 메뉴 항목을 선택하면 FPS 미터와 함께 GPU에서 사용하는 메모리양을 볼 수 있다.

## stats.js

[https://github.com/mrdoob/stats.js/](https://github.com/mrdoob/stats.js/)

![https://i.ibb.co/0VK1VYS/2023-01-15-3-35-54.png](https://i.ibb.co/0VK1VYS/2023-01-15-3-35-54.png)

stats.js는 자바스크립트 성능 측정 라이브러리로, 몇 줄의 코드를 추가하여 내 웹 애플리케이션의 성능을 측정할 수 있다. 기본적으로 FPS와 렌더링 속도, 사용하는 메모리를 측정할 수 있으며, 사용자 정의 값을 측정할 수도 있다. 아래는 공식 문서에 소개된 stats.js를 사용하는 방법이다.

```jsx
var stats = new Stats();
stats.showPanel( 1 ); // 0: fps, 1: ms, 2: mb, 3+: custom
document.body.appendChild( stats.dom );

function animate() {

	stats.begin();

	// monitored code goes here

	stats.end();

	requestAnimationFrame( animate );

}

requestAnimationFrame( animate );
```

## 사용자 정의 성능 위젯

다른 방법은 FPS를 측정하는 코드를 직접 작성하는 것이다. `requestAnimationFrame` 콜백을 사용해 현재 렌더링 사이클과 다음 사이클 사이의 시간을 측정하고, 이 콜백이 1초에 몇 번 호출되는지를 측정한다.

[window.requestAnimationFrame() - Web API | MDN](https://developer.mozilla.org/ko/docs/Web/API/Window/requestAnimationFrame)

```jsx
let panel
let start
let frames = 0

const create = () => {
	const div = document.createElement('div');

	div.style.position = 'fix';
	div.style.left = '0px';
	div.style.top = '0px';
	// style 관련 코드 생략

	return div;
}

const tick = () => {
	frames++;
	const now = window.performance.now();
	if (now >= start + 1000) {
		panel.innerText = frames;
		frames = 0;
		start = now;
	}
	window.requestAnimationFrame(tick);
}

const init = (parent = document.body) => {
	panel = create()

	window.requestAnimationFrame(() => {
		start = window.performance.now();
		parent.appendChild(panel);
		tick();
	})
}

export default init;
```

# 렌더링 함수

여기서는 순수 함수로 엘레먼트를 DOM에 렌더링하는 다양한 방법을 분석해 본다. 순수 함수로 엘레먼트를 렌더링한다는 것은 DOM이 애플리케이션의 상태에만 의존한다는 것을 의미한다. (DOM의 변화를 트리거하는 것은 오직 애플리케이션 상태의 변화, 즉 $view = f(state)$)

이 예제에서는 TodoMVC의 코드를 사용한다. MVC 구조를 사용하므로 MVC에 익숙하지 않다면 관련 글을 읽고 오자.

## 순수 함수 렌더링

애플리케이션의 index.html부터 살펴보자.

```html
<html>

<head>
    <link rel="shortcut icon" href="../favicon.ico" />
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/todomvc-common@1.0.5/base.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/todomvc-app-css@2.1.2/index.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/Faker/3.1.0/faker.js"></script>
    <title>
        Frameworkless Frontend Development: Rendering
    </title>
</head>

<body>
    <section class="todoapp">
        <header class="header">
            <h1>todos</h1>
            <input class="new-todo" placeholder="What needs to be done?" autofocus>
        </header>
        <section class="main">
            <input id="toggle-all" class="toggle-all" type="checkbox">
            <label for="toggle-all">Mark all as complete</label>
            <ul class="todo-list">
            </ul>
        </section>
        <footer class="footer">
            <span class="todo-count">1 Item Left</span>
            <ul class="filters">
                <li>
                    <a href="#/">All</a>
                </li>
                <li>
                    <a href="#/active">Active</a>
                </li>
                <li>
                    <a href="#/completed">Completed</a>
                </li>
            </ul>
            <button class="clear-completed">Clear completed</button>
        </footer>
    </section>
    <footer class="info">
        <p>Double-click to edit a todo</p>
        <p>Created by <a href="http://twitter.com/thestrazz86">Francesco Strazzullo</a></p>
        <p>Thanks to <a href="http://todomvc.com">TodoMVC</a></p>
    </footer>
    <script type="module" src="index.js"></script>
</body>

</html>
```

이 HTML 뼈대를 **동적**으로 만들기 위해서는 to-do 리스트 데이터를 불러온 다음 1)`<span>` 안의 ‘{} Item Left’ 부분과 2)`<ul>` 안의 필터링 된 to-do 리스트, 그리고 3)`selected` 클래스를 오른쪽에 추가한 필터 유형(현재 유저가 보고 있는 필터 유형)을 업데이트해야 한다.

다음은 순수 함수를 통한 렌더링 코드의 첫 번째 버전이다.

```jsx
// 1)
const getTodoElement = todo => {
  const {
    text,
    completed
  } = todo

  return `
  <li ${completed ? 'class="completed"' : ''}>
    <div class="view">
      <input 
        ${completed ? 'checked' : ''}
        class="toggle" 
        type="checkbox">
      <label>${text}</label>
      <button class="destroy"></button>
    </div>
    <input class="edit" value="${text}">
  </li>`
}

// 2)
const getTodoCount = todos => {
  const notCompleted = todos
    .filter(todo => !todo.completed)

  const { length } = notCompleted
  if (length === 1) {
    return '1 Item left'
  }

  return `${length} Items left`
}

export default (targetElement, state) => {
  const {
    currentFilter,
    todos
  } = state

	// 기존 DOM과 분리
  const element = targetElement.cloneNode(true)

  const list = element.querySelector('.todo-list')
  const counter = element.querySelector('.todo-count')
  const filters = element.querySelector('.filters')

  list.innerHTML = todos.map(getTodoElement).join('')
  counter.textContent = getTodoCount(todos)

	// 3)
  Array
    .from(filters.querySelectorAll('li a'))
    .forEach(a => {
      if (a.textContent === currentFilter) {
        a.classList.add('selected')
      } else {
        a.classList.remove('selected')
      }
    })

  return element
}
```

이 뷰 함수는 DOM 요소를 받아 복제하고, state 매개변수를 사용해 업데이트한다. 그 다음 업데이트한 새 노드를 반환한다. 여기서는 DOM을 복제하여(분리하여) 작업하고 있는데, DOM을 직접 업데이트하는 대신 복제한 DOM을 업데이트하여 반환하는 식으로 작업하면 성능이 향상된다. (진짜?) 이 뷰 함수를 실제 DOM에 연결하고자 간단한 컨트롤러를 사용한다.

```jsx
import getTodos from './getTodos.js'
import view from './view.js'

const state = {
  todos: getTodos(),
  currentFilter: 'All'
}

const main = document.querySelector('.todoapp')

window.requestAnimationFrame(() => {
  const newMain = view(main, state)
  main.replaceWith(newMain)
})
```

이 렌더링 엔진은 `requestAnimationFrame`을 기반으로 하며, 모든 DOM 조작이나 애니메이션은 이 DOM API를 기반으로 해야 한다. 이 API는 메인 스레드를 차단하지 않으며 (성능 저하의 원인이 되지 않는다는 뜻일까?) 다음 리페인트가 이벤트 루프에서 스케줄링되기 직전에 실행된다. 몇 년 전에 DOM과 렌더링에 관해 공부하고 정리해 둔 적이 있는데 이 설명이 잘 와닿지 않는 걸 보니 다시 공부할 때가 된 것 같다.

## 개선된 버전의 코드

앞서 소개된 코드는 DOM의 여러 부분을 조작하는 함수가 단 하나 뿐이므로, 상황을 복잡하게 만들 수 있다. 따라서 이것을 개선한 코드를 링크로 첨부한다. 자세하게 다루지 않는 이유는 이 책을 읽는 목표가 클린 코드를 작성하기 위함이 아니라 프레임워크에 의존하지 않는 프론트엔드 개발법을 공부하기 위해서기 때문이다.

[frameworkless-front-end-development/Chapter02/02 at master · Apress/frameworkless-front-end-development](https://github.com/Apress/frameworkless-front-end-development/tree/master/Chapter02/02)

### 의문

개선 전 코드는 state에 변경사항이 생겼을 때 DOM을 1번 수정하는 반면 개선 후 코드는 DOM을 3번 수정하는데, 그럼 개선 후 코드가 무조건 좋다고 할 수는 없는 것 아닌가?

## 구성 요소 함수

위의 코드는 함수를 수동으로 호출해야 한다는 단점이 있다. 이 파트에서는 수동으로 연결해야 했던 엘레먼트와 함수와의 연결을 자동으로 하는 방법에 대해 알아볼 것이다.

엘레먼트와 함수의 연결을 자동화하는 방법은 레지스트리와 `data-component`를 이용하는 것이다. 레지스트리는 애플리케이션에서 사용할 수 있는 모든 엘레먼트의 인덱스로, 구현 가능한 가장 간단한 레지스트리는 일반 자바스크립트 객체다. 레지스트리의 키는 `data-component` 속성 값과 일치해야 한다.

```jsx
const registry = {
	'todos': todosView.
	'counter': counterView,
	'filters': filtersView
};
```

이제 이 레지스트리를 통해 고차 함수에서 `data-component` 속성을 가진 엘리먼트에 뷰 함수를 매칭한다. index.html은 아래와 같이 수정되어 있는 상태다. (기존 html에 `data-component` 속성이 추가되었으며, 스압방지를 위해 body > section 부분만 발췌했다.)

```html
<section class="todoapp">
        <header class="header">
            <h1>todos</h1>
            <input 
                class="new-todo" 
                placeholder="What needs to be done?" 
                autofocus>
        </header>
        <section class="main">
            <input 
                id="toggle-all" 
                class="toggle-all" 
                type="checkbox">
            <label for="toggle-all">
                Mark all as complete
            </label>
            <ul class="todo-list" data-component="todos">
            </ul>
        </section>
        <footer class="footer">
            <span 
                class="todo-count" 
                data-component="counter">
                    1 Item Left
            </span>
            <ul class="filters" data-component="filters">
                <li>
                    <a href="#/">All</a>
                </li>
                <li>
                    <a href="#/active">Active</a>
                </li>
                <li>
                    <a href="#/completed">Completed</a>
                </li>
            </ul>
            <button class="clear-completed">
                Clear completed
            </button>
        </footer>
    </section>
```

이제 고차함수를 만들어 HTML과 뷰 함수를 연결하는 코드를 작성한다. targetElement(root)에서 `data-component` 속성을 가진 자식 요소들을 모두 가져와 클론한 뒤 상태에 따라 컴포넌트를 바꾸는 함수를 실행한 후 원래 노드를 대체한다. 이해하는 데 굉장히 오랜 시간이 걸린 코드인데, 이 부분만 보지 말고 이 아래의 코드까지 모두 살펴본 뒤 다시 코드를 읽어보면 이해하는 데 큰 도움이 된다.

```jsx
const renderWrapper = component => {
  return (targetElement, state) => {
		// element = component()에 의해 새로 갱신된 컴포넌트
    const element = component(targetElement, state)

    const childComponents = element
      .querySelectorAll('[data-component]')

    Array
      .from(childComponents)
      .forEach(target => {
        const name = target
          .dataset
          .component
				// add 함수에 의해 registry[name]에는 renderWrapper의 리턴값 함수가 담겨 있음
        const child = registry[name]

        if (!child) {
          return
        }
				// child를 실행하면 재귀는 아니나 재귀처럼 동작
        target.replaceWith(child(target, state))
      })

    return element
  }
}
```

재귀를 사용하는 이유는 새로 갱신된 컴포넌트 안에 있는 `data-component`를 건드려야 하는 경우 등 때문이지 않을까 싶다.

레지스트리에 구성 요소를 추가하려면 레지스트리 접근자 메서드를 만들어 사용해야 한다. 이 함수에서 `registry[name]`에 `renderWrapper`의 리턴값 함수를 할당하기 때문에 위 코드의 `child(target, state)`에서 재귀함수처럼 동작한다.

```jsx
const add = (name, component) => {
	registry[name] = renderWrapper(component);
}
```

또한 최초 DOM 요소에서 렌더링을 시작하려면 애플리케이션의 루트를 렌더링하는 메서드가 필요하다. 여기서 이 메서드는 `renderRoot`이며, 코드는 다음과 같다.

```jsx
const renderRoot = (root, state) => {
  const cloneComponent = root => {
    return root.cloneNode(true)
  }

  return renderWrapper(cloneComponent)(root, state)
}
```

마지막으로 해야 할 일은 컨트롤러에서 모든 요소를 혼합하는 것이다.
