# 6장. 라우팅

# 단일 페이지 애플리케이션

단일 페이지 애플리케이션(SPA)는 하나의 HTML 페이지로 실행되는 웹 애플리케이션으로, 표준 다중 페이지 애플리케이션에서 페이지 간 탐색 시 사용자가 경험할 수 있는 지연을 제거해 더 나은 사용자 경험을 제공한다. 앵귤러JS와 Ember 같은 프레임워크 덕분에 SPA가 웹 애플리케이션을 구축하는 주류 방식으로 떠올랐으며, 이런 프레임워크들은 **라우팅 시스템**을 통해 경로를 정의할 수 있는 시스템을 기본으로 제공한다.

아키텍쳐 관점에서, 모든 라우팅 시스템은 **레지스트리**와 **URL 리스너**라는 두 가지 핵심 요소를 갖는다. 레지스트리는 애플리케이션의 경로 목록을 수집한다. 리스너는 현재 URL이 변경되는지 지켜보며, URL이 변경되면 본문의 내용을 현재 URL과 일치하는 경로에 바인딩된 컴포넌트로 교체한다.

![https://i.ibb.co/rHK21DV/Frame-80.png](https://i.ibb.co/rHK21DV/Frame-80.png)

# 코드 예제

이제 라우팅 시스템을 세 가지 버전으로 작성해 보자. 먼저 프레임워크를 사용하지 않는 두 가지 방식을 소개한다. 첫번째는 **프래그먼트 식별자**를 기반으로 하고, 다른 하나는 **History API**를 기반으로 한다.

## 프래그먼트 식별자

모든 URL은 프래그먼트 식별자라고 불리는 해시(#)로 시작하는 선택적 부분을 포함할 수 있다. 프래그먼트 식별자의 목적은 웹 페이지의 특정 섹션을 식별하는 것으로, 예를 들어 `www.domain.org/foo.html#bar`에서 `bar`는 프래그먼트 식별자로 `id="bar"`로 HTML 요소를 식별한다. 프래그먼트 식별자가 포함된 URL을 탐색할 때 브라우저는 프래그먼트로 식별된 엘레먼트가 뷰포트의 맨 위에 오도록 페이지를 식별한다.

### 첫 번째 방식

프래그먼트 식별자의 특성을 이용해 첫 번째 `Router` 객체를 구현해 보자. 이 예제에서는 링크와 `main` 컨테이너를 가진 정말 간단한 SPA를 구현한다.

```html
<body>
	<header>
		<a href="#/">Go To Index</a>
		<a href="#/list">Go To List</a>
		<a href="#/dummy">Dummy Page</a>
	</header>
	<main>
	</main>
</body>
```

앵커를 누르면 URL이 `localhost:8080#/`에서 `localhost:8080#/list` 등으로 변경된다. 아래의 코드는 URL이 변경될 때 main 컨테이너 내부에 URL에 알맞는 컴포넌트를 넣는다.

```jsx
export default container => {
	const home = () => {
		container.textContent = 'This is Home page'
	}

	const list = () => {
		container.textContent = 'This is List Page'
	}

	const notFound = () => {
		container.textContent = 'Page Not Found!'
	}

	return { home, list, notFound }
}
```

라우터가 동작하게 하려면 라우터를 구성하고 컴포넌트를 올바른 프래그먼트에 연결해야 한다. 다음 코드는 라우터 구성 방법을 보여준다.

```jsx
import createRouter from './router.js'
import createPages from './Pages.js'

const container = document.querySelector('main')

const pages = createPages(container)

const router = createRouter()

router
	.addRoute('#/', pages.home)
	.addRoute('#/list', pages.list)
	.setNotFound(pages.notFound)
	.start()
```

라우터는 3개의 퍼블릭 메서드를 갖는다. `addRoute`는 새 라우터와 프래그먼트로 구성된 구성 객체, 구성 요소를 정의한다. `setNotFound`는 레지스트리에 없는 모든 프래그먼트에 대한 제네릭 구성 요소를 설정한다. `start` 는 라우터를 초기화하고 URL 변경을 청취하기 시작한다.

```jsx
export default () => {
	const routes = []
	let notFound = () => {}

	const router = {}

	const checkRoutes = () => {
		const currentRoute = routes.find(route => {
			return route.fragment === window.location.hash
		})

		if (!currentRoute) {
			notFound()
			return
		}

		currentRoute.component()
	}

	router.addRoute = (fragment, component) => {
		routes.push({
			fragment, component
		})

		return router
	}

	router.setNotFound = cb => {
		notFound = cb
		return router
	}

	router.start = () => {
		window.addEventListener('hashchange', checkRoutes)

		if (!window.location.hash) {
			window.location.hash = '#/'
		}

		checkRoutes()
	}

	return router
}
```

현재 프래그먼트 식별자는 `location` 객체의 `hash` 속성에 저장된다. 또한 프래그먼트가 변경될 때마다 발생하는 `hashchange` 이벤트도 적절히 활용했다.

`checkRoutes` 메서드는 라우터의 핵심 메서드로, 현재 프래그먼트와 일치하는 경로를 찾는다. 경로를 발견하면 해당 경로와 맞는 컴포넌트가 메인 컨테이너에 있는 컴포넌트를 새롭게 대체한다. 경로가 발견되지 않는 경우 `notFound` 함수가 호출된다. 이 메서드는 라우터가 처음 시작될 때와 `hashchange` 이벤트가 발생할 때마다 실행된다.

### 프로그래밍 방식으로 탐색

위의 예제는 앵커를 클릭하는 행위를 통해 경로 탐색을 트리거했다. 하지만 때로는 프로그래밍 방식으로 뷰의 변경이 필요할 때도 있다. 이를 위해 헤더의 링크를 버튼으로 바꿔 애플리케이션을 살짝 변경해보자.

```html
<body>
	<header>
		<button data-navigate="/">Go To Index</button>
		<button data-navigate="/list">Go To List</button>
		<button data-navigate="/dummy">Dummy Page</button>
	</header>
	<main>
	</main>
</body>
```

이제 아래와 같이 컨트롤러의 버튼에 이벤트 핸들러를 추가한다.

```jsx
const NAV_BTN_SELECTOR = 'button[data-navigate]'

document.body.addEventListener('click', e => {
	const { target } = e
	if (target.matches(NAV_BTN_SELECTOR)) {
		const { navigate } = target.dataset
		router.navigate(navigate)
	}
})
```

프로그래밍 방식으로 다른 뷰로 이동할 수 있도록 라우터에 새로운 퍼블릭 메서드인 `navigate`를 생성했다. 이 메서드는 새 프래그먼트를 가져와 `location` 객체에서 대체한다. `navigate` 메서드는 다음과 같이 사용하여 프로그래밍 방식으로 다른 뷰로 이동할 수 있게 해준다.

```jsx
router.navigate = fragment => {
	window.location.hash = fragment
}
```

## History API

History API를 통해 개발자는 사용자 탐색 히스토리를 조작할 수 있다. 다음은 History API의 치트 시트이며, 자세한 내용은 [Mozilla 개발자 네트워크 - History API](https://developer.mozilla.org/en-US/docs/Web/API/History_API)를 참고하자.

| 서명 | 설명 |
| --- | --- |
| back() | 히스토리에서 이전 페이지로 이동한다. |
| forward() | 히스토리에서 다음 페이지로 이동한다. |
| go(index) | 히스토리에서 특정 페이지로 이동한다. |
| pushState(state, title, URL) | 히스토리 스택의 데이터를 푸시하고 제공된 URL로 이동한다. |
| replaceState(state, title, URL) | 히스토리 스택에서 가장 최근 데이터를 바꾸고 제공된 URL로 이동한다. |

History API를 통해 라우팅하는 경우 프래그먼트 식별자를 기반으로 경로를 지정할 필요가 없다. 다음 코드는 History API를 기반으로 하는 라우터다.

```jsx
const ROUTE_PARAMETER_REGEXP = /:(\w+)/g
const URL_FRAGMENT_REGEXP = '([^\\/]+)'
const TICKTIME = 250

const extractUrlParams = (route, pathname) => {
  const params = {}

  if (route.params.length === 0) {
    return params
  }

  const matches = pathname
    .match(route.testRegExp)

  matches.shift()

  matches.forEach((paramValue, index) => {
    const paramName = route.params[index]
    params[paramName] = paramValue
  })

  return params
}

export default () => {
  const routes = []
  let notFound = () => {}
  let lastPathname

  const router = {}

  const checkRoutes = () => {
    const { pathname } = window.location
    if (lastPathname === pathname) {
      return
    }

    lastPathname = pathname

    const currentRoute = routes.find(route => {
      const { testRegExp } = route
      return testRegExp.test(pathname)
    })

    if (!currentRoute) {
      notFound()
      return
    }

    const urlParams = extractUrlParams(currentRoute, pathname)

    currentRoute.callback(urlParams)
  }

  router.addRoute = (path, callback) => {
    const params = []

    const parsedPath = path
      .replace(
        ROUTE_PARAMETER_REGEXP,
        (match, paramName) => {
          params.push(paramName)
          return URL_FRAGMENT_REGEXP
        })
      .replace(/\//g, '\\/')

    routes.push({
      testRegExp: new RegExp(`^${parsedPath}$`),
      callback,
      params
    })

    return router
  }

  router.setNotFound = cb => {
    notFound = cb
    return router
  }

  router.navigate = path => {
    window
      .history
      .pushState(null, null, path)
  }

  router.start = () => {
    checkRoutes()
    window.setInterval(checkRoutes, TICKTIME)
  }

  return router
}
```

프래그먼트 식별자를 사용하는 방법은 해시가 변경되었을 때 트리거되는 `hashchange`라는 이벤트를 사용해 DOM에 변화를 주었던 반면, URL이 변경되었을 때 트리거되는 이벤트는 존재하지 않기 때문에 `setInterval`로 경로가 변경되었는지 직접 확인해야 한다. 이 라우터는 기존에 우리가 만들었던 퍼블릭 API에서 상대 경로에 있는 해시를 제거해주기만 하면 완벽하게 호환된다.

### 링크 사용

History API를 사용한 라우터가 완벽하게 동작하려면 템플릿에 있는 링크를 업데이트해야 한다. 다음 코드는 샘플 코드의 초기 템플릿에서 업데이트된 버전이나, 우리가 의도한 대로 동작하지는 않는다. 예를 들어 ‘Go To List’를 클릭하면 `http://localhost:8080/list/index.html`로 이동하기 때문에 404 에러가 발생한다.

```html
<body>
	<header>
		<a href="/">Go To Index</a>
		<a href="/list">Go To List</a>
		<a href="/list/1">Go To Detail With Id 1</a>
		<a href="/list/2">Go To Detail With Id 2</a>
		<a href="/list/1/2">Go To Another Detail</a>
		<a href="/dummy">Dummy Page</a>
	</header>
</body>
```

이 링크가 동작하게 하려면 디폴트 동작을 변경해야 한다. 표준 탐색을 비활성화하고 라우터의 `navigate` 메서드를 사용할 수 있도록 코드를 짜보자.

```html
<body>
	<header>
		<a data-navigate href="/">Go To Index</a>
		<a data-navigate href="/list">Go To List</a>
		<a data-navigate href="/list/1">Go To Detail With Id 1</a>
		<a data-navigate href="/list/2">Go To Detail With Id 2</a>
		<a data-navigate href="/list/1/2">Go To Another Detail</a>
		<a data-navigate href="/dummy">Dummy Page</a>
	</header>
</body>
```

```jsx
const NAV_BTN_SELECTOR = 'button[data-navigate]'

router.start = () => {
    checkRoutes()
    window.setInterval(checkRoutes, TICKTIME)
  }

	// 추가된 코드, 이벤트 위임(3장 참고)
	document.body.addEventListener('click', e => {
		const { target } = e
		if (target.matches(NAV_BTN_SELECTOR)) {
			e.preventDefault()
			router.navigate(target.href)
		}
	})

  return router
}
```

## Navigo

[https://github.com/krasimir/navigo](https://github.com/krasimir/navigo)

Navigo는 아주 작은 바닐라 자바스크립트 라우팅 라이브러리다. 여기서는 Navigo를 사용해서 라우터를 구현한다. 우리가 앞서 만든 API 구조를 유지하면서 라우터 자체의 내부 코드만 변경할 것이다.

```jsx
export default () => {
  const navigoRouter = new window.Navigo()
  const router = {}

  router.addRoute = (path, callback) => {
    navigoRouter.on(path, callback)
    return router
  }

  router.setNotFound = cb => {
    navigoRouter.notFound(cb)
    return router
  }

  router.navigate = path => {
    navigoRouter.navigate(path)
  }

  router.start = () => {
    navigoRouter.resolve()
    return router
  }

  return router
}
```

다음으로, 내부 탐색 링크 관리를 위해 넣어뒀던 `data-navigation`을 `data-navigo`로 변경한다.
