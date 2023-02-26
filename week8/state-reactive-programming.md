## 반응형 프로그래밍

[프로그래밍 패러다임과 반응형 프로그래밍 그리고 Rx | 요즘IT](https://yozm.wishket.com/magazine/detail/1334/)

반응형 프로그래밍은 객체지향 프로그래밍, 함수형 프로그래밍과 같은 프로그래밍 패러다임 중 하나다. 내가 이해한 바로는 데이터가 변경되면 데이터를 구독하는 곳에서 변화에 따라 알아서 처리하는 방식인 것 같다. 패러다임인 만큼 개념이 방대하므로 더 자세하게 알아보고 싶다면 위 글을 읽어보자.

### 반응형 모델

아래 모델은 반응형 상태 관리의 예시이며, 이런 방식으로 도메인 로직에만 집중하고 아키텍처 부분은 별도의 라이브러리로 떠넘길 수 있다.

```jsx
import observableFactory from './observable.js'

const cloneDeep = x => {
  return JSON.parse(JSON.stringify(x))
}

const INITIAL_STATE = {
  todos: [],
  currentFilter: 'All'
}

export default (initalState = INITIAL_STATE) => {
  const state = cloneDeep(initalState)

  const addItem = text => {
    if (!text) {
      return
    }

    state.todos.push({
      text,
      completed: false
    })
  }

  const updateItem = (index, text) => {
    if (!text) {
      return
    }

    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos[index].text = text
  }

  const deleteItem = index => {
    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos.splice(index, 1)
  }

  const toggleItemCompleted = index => {
    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos[index].completed = !state.todos[index].completed
  }

  const completeAll = () => {
    state.todos.forEach(t => {
      t.completed = true
    })
  }

  const clearCompleted = () => {
    state.todos = state.todos.filter(t => !t.completed)
  }

  const changeFilter = filter => {
    state.currentFilter = filter
  }

  const model = {
    addItem,
    updateItem,
    deleteItem,
    toggleItemCompleted,
    completeAll,
    clearCompleted,
    changeFilter
  }

  return observableFactory(model, () => state)
}
```

아래는 옵저버블 팩토리를 기반으로 하는 새 버전의 모델 객체다.

```jsx
export default (model, stateGetter) => {
	let listeners = []

	const addChangeListener = cb => {
		listeners.push(cb)
		cb(freeze(stateGetter()))
		return () => {
			listeners = listeners.filter(element => element !== cb)
		}
	}

	const invokeListeners = () => {
		const data = freeze(stateGetter())
		listeners.forEach(l => l(data))
	}

	const wrapAction = originalAction => {
		return (...args) => {
			const value = originalAction(...args)
			invokeListeners()
			return value
		}
	}

	const baseProxy = {
		addChangeListener
	}

	return Object.keys(model).filter(key => {
		return typeof model[key] === 'function'
	}).reduce((proxy, key) => {
		const action = model[key]
		return {
			...proxy,
			[key]: wrapAction(action)
		}
	}, baseProxy)
}
```

옵저버블 팩토리의 코드는 이해하기 어려울 수 있으니 책의 설명을 읽고 이해해보자. `Model` 객체의 프록시를 생성하면 이 원본 모델의 모든 메서드는 `wrapAction`에 의해 래핑되며 `invokeListeners()` 를 호출하는 동일한 이름과 기능의 메서드가 생성된다. 프록시로 상태를 전달하기 위해 `stateGetter` 함수를 통해 모델에서 변경이 이루어질 때마다 현재 상태를 가져온다.

<aside>
💡 `Proxy`는 특정 객체를 감싸 프로퍼티 읽기, 쓰기와 같은 객체에 가해지는 작업을 중간에서 가로채는 객체로, 가로채진 작업은 `Proxy` 자체에서 처리되기도 하고, 원래 객체가 처리하도록 그대로 전달되기도 한다.

</aside>

### 네이티브 프록시

자바스크립트가 제공하는 `Proxy` 객체를 사용하면 객체의 디폴트 동작을 사용자 정의 코드로 쉽게 래핑할 수 있다. 아래는 기본 객체의 속성을 가져오거나 설정할 때마다 로그를 기록하는 간단한 프록시를 생성하는 코드다.

```jsx
const base = {
	foo: 'bar'
}

const handler = {
	get: (target, name) => {
		console.log(`Getting ${name}`)
		return target[name]
	},
	set: (target, name, value) => {
		console.log(`Setting ${name} to ${value}`)
		target[name] = value
		return true
	}
}

const proxy = new Proxy(base, handler)

proxy.foo = 'baz'
console.log(`Logging ${proxy.foo}`)
```

![https://i.ibb.co/1vwR5Tb/2023-02-26-5-06-53.png](https://i.ibb.co/1vwR5Tb/2023-02-26-5-06-53.png)

기본 객체를 래핑하는 프록시를 생성하려면 트랩 집합으로 구성된 핸들러가 필요하다. 트랩은 기본 객체의 기본 작업을 래핑하는 방법으로, 예제의 경우 모든 속성의 getter, setter를 덮어썼다. `set` 핸들러는 작업 성공을 나타내는 boolean 값을 리턴해야 한다.

아래 코드에서 `Proxy` 객체는 옵저버블 팩토리를 생성한다.

```jsx
const cloneDeep = x => {
  return JSON.parse(JSON.stringify(x))
}

const freeze = state => Object.freeze(cloneDeep(state))

export default (initialState) => {
	let listeners = []

	const proxy = new Proxy(cloneDeep(initialState), {
		set: (target, name, value) => {
			target[name] = value
			listeners.forEach(l => l(freeze(proxy)))
			return true
		}
	})

	proxy.addChangeListener = cb => {
		listeners.push(cb)
		cb(freeze(proxy))
		return () => {
			listeners = listeners.filter(l => l !== cb)
		}
	}

	return proxy
}
```

`Proxy`를 사용하는 옵저버블 팩토리는 전에 만들었던 옵저버블 팩토리와 사용법이 조금 다르다. 아래는 `Proxy` 버전 옵저버블 팩토리로 작성된 모델의 새 버전이다.

```jsx
import observableFactory from './observable.js'

const INITIAL_STATE = {
  todos: [],
  currentFilter: 'All'
}

export default (initialState = INITIAL_STATE) => {
  const state = observableFactory(initialState)

  const addItem = text => {
    if (!text) {
      return
    }

    state.todos = [...state.todos, {
      text,
      completed: false
    }]
  }

  const updateItem = (index, text) => {
    if (!text) {
      return
    }

    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos = state.todos.map((todo, i) => {
      if (i === index) {
        todo.text = text
      }
      return todo
    })
  }

  const deleteItem = index => {
    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos = state.todos.filter((todo, i) => i !== index)
  }

  const toggleItemCompleted = index => {
    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos = state.todos.map((todo, i) => {
      if (i === index) {
        todo.completed = !todo.completed
      }
      return todo
    })
  }

  const completeAll = () => {
    state.todos = state.todos.map((todo, i) => {
      todo.completed = true
      return todo
    })
  }

  const clearCompleted = () => {
    state.todos = state.todos.filter(t => !t.completed)
  }

  const changeFilter = filter => {
    state.currentFilter = filter
  }

  return {
    addChangeListener: state.addChangeListener,
    addItem,
    updateItem,
    deleteItem,
    toggleItemCompleted,
    completeAll,
    clearCompleted,
    changeFilter
  }
}
```

프록시를 사용하는 버전의 경우 `todos` 배열을 매번 덮어쓴다. 첫 번째 버전에서는 새로운 속성을 추가할 때 배열에 `push`하는 방법을 사용했지만, 프록시 버전의 경우 `set` 트랩을 호출하기 위해 배열을 덮어쓰는 방법을 사용한다.
