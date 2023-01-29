# 4장. 웹 구성 요소

참고 - 여기서 말하는 ‘컴포넌트’는 ‘구성 요소’와 같은 뜻입니다.

# 웹 컴포넌트 API

웹 컴포넌트에는 세 가지 중요 기술이 있다. 

- **HTML 템플릿**: `<template>` 태그는 컨텐츠가 렌더링되지는 않지만, 자바스크립트 코드에서 동적인 컨텐츠를 생성하는 데 ‘스탬프’로 사용되도록 하려는 경우에 유용하다.
- **사용자 정의 요소**: 이 API를 통해 개발자는 완전한 기능을 갖춘 자신만의 DOM 요소를 작성할 수 있다 - **사용자 정의 HTML 태그를 만들 수 있다.**
- **Shadow DOM**: 이 기술은 웹 컴포넌트가 컴포넌트 외부의 DOM에 영향을 받지 않아야 하는 경우에 유용하다. 다른 사람들과 공유할 수 있도록 구성 요소 라이브러리나 위젯을 작성하려는 경우에 유용하다. 가상 DOM과는 완전히 다른 개념이며, 섀도우 DOM은 캡슐화와 관련되고, 가상 DOM은 성능과 관련된다.

HTML 템플릿은 3장에서 알아봤고, 섀도우 DOM은 다음 번에 다룰 예정이다. 그럼 사용자 정의 요소에 대해서 알아보도록 하자.

## 사용자 정의 요소

사용자 정의 요소 API는 웹 컴포넌트의 핵심 요소이며, `HTMLElement` 를 상속받는 자바스크립트 클래스를 통해 다음과 같이 사용자 정의 HTML 태그를 작성할 수 있다.

```html
<app-calendar/>
```

사용자 정의 요소 API를 사용해 태그를 작성할 때에는 대시로 구분된 두 단어 이상의 태그를 사용해야 한다. 한 단어 태그는 W3C에서만 단독으로 사용 가능하다.

```jsx
export default class HelloWorld extends HTMLElement {
	connectedCallback() {
		window.requestAnimationFrame(() => {
			this.innerHTML = '<div>Hello World!</div>'
		})
	}
}
```

`connectedCallback`은 사용자 정의 요소의 라이프사이클 메서드 중 하나로, 컴포넌트가 DOM에 연결될 때 호출된다. 리액트의 `componentDidMount` 메서드와 매우 유사하며, 컴포넌트의 컨텐츠를 렌더링하거나, 타이머를 시작하거나, 네트워크에서 데이터를 가져오기에 좋은 장소다. 반대로 컴포넌트가 DOM에서 삭제될 때에는 `disconnectedCallback`이 호출된다.

**새로 생성한 사용자 정의 요소를 사용하기 위해선** **브라우저 구성 요소 레지스트리에 추가**해야 한다. 이 작업을 통해 HTML 속 태그 이름과 사용자 정의 요소 클래스를 연결한다.

```jsx
import HelloWorld from './components/HelloWorld.js'

window.customElements.define('hello-world', HelloWorld);
```

### 사용자 정의 요소의 속성 관리

웹 컴포넌트의 가장 중요한 기능은 개발자가 어떤 프레임워크(리액트, 앵귤러 포함 모든 웹 애플리케이션)와도 호환되는 새로운 구성 요소를 만들 수 있다는 것이다. 그러기 위해선 우리가 만든 사용자 구성 요소에 다른 표준 HTML 요소와 동일한 공용 API를 구현해야 한다. 다른 속성과 동일한 방식으로 이 속성을 관리할 수 있어야 하기 때문이다.

표준 HTML 요소들은 아래 3가지의 방법으로 속성을 설정할 수 있다.

```
// 1) HTML에서
<input type="text" value="Frameworkless">
// 2) javascript에서
input.value = 'Frameworkless';
// 3) javascript에서
input.setAttribute('value', 'Frameworkless');
```

속성을 관리하는 API는 `HTMLElement`를 상속한 자바스크립트 클래스에 속성에 대한 setter와 getter를 정의하여 구현할 수 있다.

```jsx
export default class HelloWorld extends HTMLElement {
	get color() {
		return this.getAttribute('color') || DEFAULT_COLOR
	}

	set color() {
		this.setAttribute('color', value);
	}

	connectedCallback() {
		window.requestAnimationFrame(() => {
			const div = document.createElement('div');
			div.textContent = 'Hello World!';
			div.style.color = this.color;

			this.appendChild(div)
		})
	}
}
```

API를 구현하면 아래처럼 속성을 사용할 수 있다.

```jsx
<hello-world></hello-world>
<hello-world color='blue'></hello-world>
<hello-world color='palevioletred'></hello-world>
```

### attributeChangedCallback

방금 전의 예제는 `connectedCallback` 메서드에서 `color` 속성을 바꿔 DOM에 적용했는데, 다음의 예제처럼 초기 렌더링이 된 후에 속성을 클릭 이벤트 핸들러로 변경하면 어떻게 될까?

```jsx
const changeColorTo = color => {
	document.querySelectorAll('hello-world').foreach(helloWorld => {
		helloWorld.color = color;
	})
}

document.querySelector('button').addEventListener('click', () => {
	changeColorTo('blue');
})
```

버튼을 클릭하면 핸들러는 모든 `<hello-world>` 요소의 color 속성을 파란색으로 변경하나 화면에는 아무런 변화가 일어나지 않는다. DOM이 다시 그려지지 않았기 때문이다. 사용자 구성 요소의 `setter`에 새로운 색상으로 DOM을 업데이트하는 코드를 넣는 방법을 생각해볼 수 있겠으나, `setter`를 사용하는 대신 `setAttribute`를 사용해 속성을 변경할 경우에는 DOM이 업데이트되지 않는다는 단점이 있다. 속성의 변경에 따라 DOM을 업데이트하는 올바른 방법은 `attributeChangedCallback` 메서드를 사용하는 것이다. 이 메서드는 속성이 변경될 때마다 호출되므로 속성이 변경됨에 따른 후속 조치를 취하기 딱 적합한 장소다. 이제 `attributeChangedCallback`을 사용해 새로운 `color` 속성이 제공될 때마다 DOM이 업데이트되도록 코드를 고쳐보자.

```jsx
const DEFAULT_COLOR = 'black'

export default class HelloWorld extends HTMLElement {
	// 추가된 부분 1
	static get observedAttributes() {
		return ['color'];
	}

	get color() {
		return this.getAttribute('color') || DEFAULT_COLOR
	}

	set color() {
		this.setAttribute('color', value);
	}
	// 추가된 부분 2
	attributeChangedCallback(name, oldValue, newValue) {
		if (!this.div) {
			return;
		}

		if (name === 'color') {
			this.div.style.color = newValue;
		}
	}

	connectedCallback() {
		window.requestAnimationFrame(() => {
			const div = document.createElement('div');
			div.textContent = 'Hello World!';
			div.style.color = this.color;

			this.appendChild(div)
		})
	}
}
```

`attributeChangedCallback` 메서드는 **변경된 속성의 이름**, **속성의 이전 값**, **속성의 새로운 값**의 3가지 매개변수를 받는다. 모든 속성이 `attributeChangedCallback`을 트리거하지는 않으며, `observedAttributes` 배열에 나열된 속성만 트리거한다.

### 사용자 정의 이벤트

이번에는 좀 더 복잡한 컴포넌트를 분석해보자. `github-avatar`의 목적은 깃허브 사용자의 아바타를 보여주는 것이다. 이 컴포넌트를 사용하려면 `user` 속성을 설정해야 한다.

```html
<github-avatar user='mori8'></github-avatar>
```

이 컴포넌트가 동작하는 방식은 다음과 같다:

1. 컴포넌트가 DOM에 연결되면 ‘loading’이라는 placeholder를 표시한다.
2. 깃허브 REST API를 사용해 아바타 이미지 URL을 가져온다.
3. 요청이 성공하면 아바타를 표시하고, 실패하면 ‘error’ placeholder를 표시한다.

```jsx
const ERROR_IMAGE = 'https://files-82ee7vgzc.now.sh';
const LOADING_IMAGE = 'https://files-8bga2nnt0.now.sh';

const getGithubAvaterUrl = async user => {
	if (!user) {
		return;
	}

	const url = `https://api.github.com/users/${user}`

	const response = await fetch(url)
	if (!response.ok) {
		throw new Error(response.statusText);
	}
	const data = await response.json();
	
	return data.avatar_url;
}

export default class GithubAvatar extends HTMLElement {
	constructor() {
		super();
		this.url = LOADING_IMAGE;
	}

	get user() {
		return this.getAttribute('user');
	}

	set user() {
		this.setAttribute('user', value);
	}

	render() {
		window.requestAnimationFrame(() => {
			this.innerHTML = '';
			const img = document.createElement('img');
			img.src = this.url;
			this.appendChild(img);
		})
	}

	async loadNewAvatar() {
		const { user } = this;
		if (!user) {
			return;
		}

		try {
			this.url = await getGithubAvaterUrl(user);
		} catch (e) {
			this.url = ERROR_IMAGE;
		}

		this.render();
	}

	connectedCallback() {
		this.render();
		this.loadNewAvatar();
	}
}
```

컴포넌트 외부에서 `github-avatar` 의 HTTP 요청 결과에 반응하려면, 다른 표준 구성 요소에서 정보를 얻는 방법과 동일하게 DOM 이벤트를 사용해야 한다. 이 때 3장에서 배운 사용자 정의 이벤트 API를 사용한다. 아래 코드의 `github-avatar` 컴포넌트는 아바타가 로드되었을 때와 오류가 발생했을 때의 두 가지 이벤트를 발생시킨다. `github-avatar`의 HTTP 요청 결과를 감지하고자 하는 외부 컴포넌트는 이벤트 핸들러를 통해 이 이벤트를 감지하여 원하는 행동을 취하면 된다.

```jsx
const ERROR_IMAGE = 'https://files-82ee7vgzc.now.sh';
const LOADING_IMAGE = 'https://files-8bga2nnt0.now.sh';

const AVATAR_LOAD_COMPLETE = 'AVATAR_LOAD_COMPLAETE';
const AVATAR_LOAD_ERROR = 'AVATAR_LOAD_ERROR';

export const EVENTS = {
	AVATAR_LOAD_COMPLETE,
	AVATAR_LOAD_ERROR
}

const getGithubAvaterUrl = async user => {
	if (!user) {
		return;
	}

	const url = `https://api.github.com/users/${user}`

	const response = await fetch(url)
	if (!response.ok) {
		throw new Error(response.statusText);
	}
	const data = await response.json();
	
	return data.avatar_url;
}

export default class GithubAvatar extends HTMLElement {
	constructor() {
		super();
		this.url = LOADING_IMAGE;
	}

	get user() {
		return this.getAttribute('user');
	}

	set user() {
		this.setAttribute('user', value);
	}

	onLoadAvatarComplete() {
		const event = new CustomEvent(AVATAR_LOAD_COMPLETE, {
			detail: {
				avatar: this.url
			}
		})
	}

	onLoadAvatarError(error) {
		const event = new CustomEvent(AVATAR_LOAD_ERROR {
			detail: {
				error
			}
		})
		this.dispatchEvent(event);
	}

	render() {
		window.requestAnimationFrame(() => {
			this.innerHTML = '';
			const img = document.createElement('img');
			img.src = this.url;
			this.appendChild(img);
		})
	}

	async loadNewAvatar() {
		const { user } = this;
		if (!user) {
			return;
		}

		try {
			this.url = await getGithubAvaterUrl(user);
			this.onLoadAvatarComplete();
		} catch (e) {
			this.url = ERROR_IMAGE;
			this.onLoadAvatarError(e);
		}

		this.render();
	}

	connectedCallback() {
		this.render();
		this.loadNewAvatar();
	}
}
```

새로 생성한 두 종류의 사용자 정의 이벤트에 아래처럼 이벤트 헨들러를 연결할 수 있다.
```javascript
import { EVENTS } from './components/GitHubAvatar.js';

document.querySelectorAll('github-avatar').forEach(avatar => {
	avatar.addEventListener(EVENTS.AVATAR_LOAD_COMPLETE, e => {
		console.log('Avatar Loaded', e.detail.avatar);
	})

	avatar.addEventListener(EVENTS.AVATAR_LOAD_ERROR, e => {
		console.log('Avatar Loading Error', e.detail.error);
	})
})
```
