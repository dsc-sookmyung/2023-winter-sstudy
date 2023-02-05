# 5장. HTTP 요청

## 간단한 역사: AJAX의 탄생

[Ajax 시작하기 - 웹 개발자 안내서 | MDN](https://developer.mozilla.org/ko/docs/Web/Guide/AJAX/Getting_Started)

1999년 이전에는 서버에서 데이터를 가져올 때 전체 페이지를 다시 로드해야 했다. 하지만 AJAX(Asynchronous JavaScript and XML)의 등장 이후로 서버에서 데이터를 가져올 때 페이지를 완전히 다시 로드하지 않고 필요한 데이터만 서버에서 가져와 화면을 채우는 식으로 개발이 가능해졌다. AJAX 애플리케이션의 핵심은 `XMLHttpRequest` 객체로, 이것을 통해 HTTP 요청으로 서버에서 데이터를 가져올 수 있다.

요즘은 자바스크립트를 통한 비동기 통신을 통칭하는 단어로 더 많이 쓰인다고 한다.

## todo-list REST 서버

앞선 장에서 투두 리스트를 만들어 보며 프레임워크 없는 프론트엔드 개발을 경험해 봤었다. 이 장에서는 프레임워크 없는 HTTP 통신 방법을 설명하기 위해 Express.js로 간단한 서버를 구축해 테스트를 해볼 것이다. 여기서는 실제 데이터베이스 대신 임시 배열을 사용해 투두 리스트와 관련된 데이터를 저장한다.

```jsx
const express = require('express');
const bodyParser = require('body-parser');
const uuidv4 = require('uuid/v4');
const findIndex = require('lodash.findindex');

const PORT = 8080

const app = express()
let todos = []

app.use(bodyParser.json())

app.get('/api/todos', (req, res) => {
	res.send(todos);
})

app.post('/api/todos', (req, res) => {
	const newTodo = {
		completed: false,
		...req.body,
		id: uuidv4()
	};

	todos.push(newTodo);

	res.status(201);
	res.send(newTodo);
})

app.patch('/api/todos/:id', (req, res) => {
	const updateIndex = findIndex(
		todos,
		t => t.id === req.params.findIndex
	);

	const oldTodo = todos[updateIndex];

	const newTodo = {
		...oldTodo,
		...req.body
	};

	todos[updateIndex] = newTodo;

	res.send(newTodo);
})

app.delete('/api/todos/:id', (req, res) => {
	todos = todos.filter(
		t => t.id !== req.params.id
	);

	res.status(204);
	res.send();
})

app.listen(PORT);
```

## 코드 예제

`HMLHttpRequest`, `Fetch`, `axios`를 사용해 각각의 HTTP 클라이언트 버전을 작성해보고, 강점과 약점을 분석해보자.

### 기본 구조

HTTP 클라이언트의 동작 방식을 보여주기 위해 다음과 같은 구조를 가진 간단한 애플리케이션을 사용한다.

```html
<html>
	<body>
		<button data-list>Read Todos List</button>
		<button data-add>Add Todo</button>
		<button data-update>Update Todo</button>
		<button data-delete>Delete Todo</button>
		<div></div>
	</body>
</html>
```

```jsx
// controller
import todos from './todos.js'

const NEW_TODO_TEXT = 'A simple todo Element'

const printResult = (action, result) => {
	const time = (new Date()).toTimeString()
	const node = document.createElement('p')
	node.textContent = `${action.toUpperCase()}: ${JSON.stringify(result)} (${time})`
	
	document.querySelector('div').appendChild(node)
}

const onListClick = async () => {
	const result await todos.list()
	printResult('list todos', result)
}

const onAddClick = async () => {
	const result = await todos.create(NEW_TODO_TEXT)
	printResult('add todo', result)
}

const onUpdateClick = async () => {
	const list = await todos.list()
	
	const { id } = list[0]
	const newTodo = {
		id,
		completed: true
	}
	const result = await todos.update(newTodo)
	printResult('update todo', result)
}

const onDeleteClick = async () => {
	const list = await todos.list()
	const { id } = list[0]
	
	const result = await todos.delete(id)
	print('delete todo', result)
}

document.querySelector('button[data-list]').addEventListener('click', onListClick)
document.querySelector('button[data-add]').addEventListener('click', onAddClick)
document.querySelector('button[data-update]').addEventListener('click', onUpdateClick)
document.querySelector('button[data-delete]').addEventListener('click', onDeleteClick)
```

이 컨트롤러에서는 HTTP 클라이언트를 직접 사용하는 대신 HTTP 요청을 `todos` 모델에 래핑했는데, HTTP 클라이언트를 모델 객체에서 캡슐화하여 사용했을 때의 첫 번째 장점으로는 **테스트 가능성**이 있다. `todos` 객체를 정적 데이터 세트를 반환하는 모의(mock) 데이터로 바꿀 수 있다. 이런 식으로 컨트롤러를 독립적으로 테스트할 수 있다. 또 다른 장점은 **가독성**이다. 모델 객체는 코드를 좀 더 명확하게 만든다. 이러한 이유로 보통 컨트롤러에서 직접 HTTP 클라이언트를 사용하지 안혹, 모델 객체에서 캡슐화하여 사용한다.

다음은 `todo` 모델 객체 코드이다.

```jsx
import http from './http.js'

const HEADERS = {
	'Content-Type': 'application/json'
}

const BASE_URL = '/api/todos'

const list = () => http.get(BASE_URL)

const create = text => {
	const todo = {
		text,
		completed: false
	}

	return http.post(BASE_URL, todo, HEADERS)
}

const update = newTodo => {
	const url = `{BASE_URL}/${newTodo.id}`
	
	return http.patch(url, newTodo, HEADERS)
}

const deleteTodo = id => {
	const url = `{BASE_URL}/${newTodo.id}`
	
	return http.delete(url, HEADERS)
}

export default { list, create, update, delete: deleteTodo }
```

`http.{verb}` 식으로 사용하는 대신 `http(url, verb, body, config)` 처럼 동사를 매개변수로 사용해 http를 객체가 아닌 함수로 사용하는 방법도 있다. 중요한 것은 방법이 아닌 꺾이지 않는 일관성

### XMLHttpRequest

다음 코드의 구현은 `XMLHttpRequest`를 기반으로 한다. `XMLHttpRequest`는 W3C가 비동기 HTTP 요청의 표준 방법을 정의한 첫 번째 시도였다.

```jsx
const setHeaders = (xhr, headers) => {
	Object.entries(headers).forEach(entry => {
		const { name, value } = entry
		xhr.setRequestHeader(name, value)
	}

const parseResponse = xhr => {
	const { status, responseText } = xhr
	
	let data
	if (status !== 204) {
		data = JSON.parse(responseText)
	}

	return { status, data }
}

const request = params => {
	return newe Promise((resolve, reject) => {
		const xhr = new XMLHttpRequest()
		
		const {
			method = 'GET',
			url,
			headers = {},
			body
		} = params

		xhr.open(method, url)
		setHeaders(xhr, headers)
		xhr.send(JSON.stringify(body))
	
		xhr.onerror = () => {
			reject(new Error('HTTP Error'))
		}
		
		xhr.ontimeout = () => {
			reject(new Error('Timeout Error'))
		}

		xhr.onload = () => resolve(parseResponse(xhr))
	})
}

const get = async (url, headers) => {
	const response = await request({url, headers, method: 'GET'})
	return response.data
}

const post = async (url, body, headers) => {
	const response = await request({url, headers, method: 'POST', body})
	return response.data
}

const put = async (url, body, headers) => {
	const response = await request({url, headers, method: 'PUT', body})
	return response.data
}

const patch = async (url, body, headers) => {
	const response = await request({url, headers, method: 'PATCH', body})
	return response.data
}

const deleteRequest = async (url, headers) => {
	const response = await request({url, headers, method: 'DELETE'})
	return response.data
}

export default { get, post, put, patch, delete: deleteRequest }
```

HTTP 클라이언트의 공개 API는 Promise를 기반으로 한다. 따라서 `request` 메서드는 표준 `XMLHttpRequest` 요청을 새로운 Promise 객체로 묶어 사용한다. `get`, `post`, `put`, `patch`, `delete` 메서드로 `request` 메서드를 좀 더 쉽게 사용할 수 있다.

다음은 `XMLHttpRequest`를 사용한 HTTP 요청의 흐름을 보여준다.

1. 새로운 `XMLHttpRequest` 객체 생성(`new XMLHttpRequest`)
2. 특정 URL로 요청을 초기화(`xhr.open(method, url)`)
3. Request 구성
4. 요청 전송(`xhr.send(JSON.stringify(body))`)
5. 요청이 끝날 때까지 대기
    1. 요청이 성공적으로 끝나면 `onload` 콜백 호출
    2. 요청이 오류로 끝나면 `onerror` 콜백 호출
    3. 요청이 타임아웃으로 끝나면 `ontimeout` 콜백 호출

### Fetch

Fetch는 원격 리소스에 접근하고자 만들어진 API다. Fetch는 `Request`나 `Response`등의 네트워크 객체에 대한 표준 정의를 제공한다. 덕분에 `ServiceWorker`나 `Cache`같은 다른 API와 상호 운용이 가능하다.

요청을 생성하려면 다음 코드처럼 Fetch API로 구현된 HTTP 클라이언트의 구현인 `window.fetch` 메서드를 사용해야 한다. 

```jsx
const parseResponse = async response => {
	const { status } = response
	let data
	if (status !== 204) {
		data = await response.json()
	}

	return { status, data }

const reqeust = async params => {
	const { method = 'GET', url, headers = {}, body } = params
	
	const config = { method, headers: new window.Headers(headers) }

	if (body) {
		config.body = JSON.stringify(body)
	}

	const response = await window.fetch(url, config)

	return parseResponse(response)
}

const get = async (url, headers) => {
	const response = await request({url, headers, method: 'GET'})
	return response.data
}

const post = async (url, body, headers) => {
	const response = await request({url, headers, method: 'POST', body})
	return response.data
}

const put = async (url, body, headers) => {
	const response = await request({url, headers, method: 'PUT', body})
	return response.data
}

const patch = async (url, body, headers) => {
	const response = await request({url, headers, method: 'PATCH', body})
	return response.data
}

const deleteRequest = async (url, headers) => {
	const response = await request({url, headers, method: 'DELETE'})
	return response.data
}

export default { get, post, put, patch, delete: deleteRequest }
```

이 HTTP 클라이언트는 `XMLHttpRequest`와 동일한 공용 API(사용하려는 각 HTTP 메서드로 래핑된 요청 함수)를 가진다. Fetch 코드는 `window.fetch`가 Promise 객체를 반환하기 때문에 `XMLHttpRequest`보다 읽기가 더 쉽다. 따라서 전통적인 콜백 기반의 `XMLHttpRequest`의 접근 방식을 최신의 프로미스 기반으로 변환하기 위한 보일러플레이트 코드가 필요하지 않다.

`window.fetch`가 반환한 프로미스는 `Response` 객체를 resolve한다. 이 객체를 통해 서버가 보낸 response body를 추출할 수 있으며, 수신된 데이터의 형식에 따라 `text()`, `blob()`, `json()`같은 메서드를 사용한다. 실제 서비스에서는 `Content-Type` 헤더와 함께 적절한 메서드를 함께 사용해야 한다.

### Axios

마지막으로 사용해 볼 라이브러리는 `axios`다. `axios`는 브라우저와 Node.js에서 바로 사용할 수 있다. `axios`의 API는 프로미스 기반으로 Fetch API와 매우 유사하다.

```jsx
const reqeust = async params => {
	const { method = 'GET', url, headers = {}, body } = params
	
	const config = { url, method, headers, data: body }

	if (body) {
		config.body = JSON.stringify(body)
	}

	return axios(config)
}

const get = async (url, headers) => {
	const response = await request({url, headers, method: 'GET'})
	return response.data
}

const post = async (url, body, headers) => {
	const response = await request({url, headers, method: 'POST', body})
	return response.data
}

const put = async (url, body, headers) => {
	const response = await request({url, headers, method: 'PUT', body})
	return response.data
}

const patch = async (url, body, headers) => {
	const response = await request({url, headers, method: 'PATCH', body})
	return response.data
}

const deleteRequest = async (url, headers) => {
	const response = await request({url, headers, method: 'DELETE'})
	return response.data
}

export default { get, post, put, patch, delete: deleteRequest }
```

### 아키텍처 검토

세 가지 라이브러리로 구현된 HTTP 클라이언트는 모두 동일한 공용 API를 가진다. 이런 특성 덕분에 최소한의 노력으로 HTTP 요청에 사용되는 라이브러리를 변경할 수 있다. 이것은 소프트웨어 디자인 원칙 중 하나인 ‘**구현이 아닌 인터페이스로 프로그래밍하라.**’ 라는 원칙을 적용한 것이다. 실제 서비스에서 HTTP 클라이언트 인터페이스를 사용하지 않고 직접 특정 라이브러리로 구현한 경우에는 라이브러리를 변경하고자 할 때 매우 큰 비용이 들 것이다. **라이브러리를 사용할 때 인터페이스를 생성해 사용하면 필요시 새로운 라이브러리로 쉽게 변경할 수 있다.**

## 적합한 HTTP API를 선택하는 방법

‘딱 맞는’ 프레임워크란 존재하지 않으며, ‘적합한’ 컨택스트에서 유효한 ‘적합한’ 프레임워크가 있을 뿐이다. 따라서 여기서는 `XMLHttpRequest`, `Fetch` API, `axios`의 특성을 각각 다른 관점에서 알아본다.

### 호환성

`Fetch`는 최신 브라우저에서만 동작하기 때문에, 인터넷 익스플로러를 꼭 지원해야 한다면 `axios`나 `XMLHttpRequest`를 사용해야 한다. 인터넷 익스플로러 11 미만의 (극악의) 환경에서도 동작해야 하는 경우에는 `XMLHttpRequest`만 사용할 수 있다.

### 휴대성

`Fetch`와 `XMLHttpRequest`는 모두 브라우저 위에서만 동작하기 때문에 Node.js나 React Native 등 다른 자바스크립트 환경에서 코드를 실행해야 하는 경우 `axios`를 사용하자.

### 발전성

`Fetch`의 가장 중요한 기능 중 하나인 `Request`나 `Response`같은 네트워크 관련 객체의 표준 정의를 제공하는 것이다. 이 특성은 `ServiceWorker`나 `Cache` API와 잘 맞기 때문에 코드베이스를 빠르게 발전시키고자 하는 경우 `Fetch`가 아주 유용할 것이다.

### 보안

`axios`에는 cross-site request 위조나 XSRF에 대한 보호 시스템이 내장되어 있다.

### 학습 곡선

`XMLHttpRequest`는 콜백 작업 때문에 주니어 개발자에게는 어렵게 느껴질 수 있다. 이 경우 내부 API를 프로미스로 래핑하면 되나, 그냥 `axios`나 `Fetch`를 사용하는 방법도 있다.
