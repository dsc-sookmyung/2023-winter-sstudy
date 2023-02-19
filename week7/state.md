프론트엔드 애플리케이션이나 좀 더 일반적으로는 모든 종류의 클라이언트 애플리케이션의 효과적인 데이터 관리 방법을 상태 관리라고 한다. 요즘에는 프론트엔드 상태 관리 전용 라이브러리도 등장했으며, MobX와 Redux가 대표적이다. 이번 장에서는 3가지 상태 관리 전략을 직접 구축하고 비교하면서 장단점을 분석해 볼 것이다.

## TodoMVC 애플리케이션 리뷰

베이스 코드로는 이전 장에서 계속 사용해 왔던 TodoMVC 코드를 사용한다. 아래 코드에서 todo와 필터를 조작하는 모든 이벤트와 함께 컨트롤러의 코드를 볼 수 있다. 상태를 관리하는 함수들은 `events` 객체에 정의되어 있으며, 이 객체는 레지스트리를 거쳐 `View`에 전달되어 DOM 핸들러에 연결된다.

```jsx
import todosView from './view/todos.js'
import counterView from './view/counter.js'
import filtersView from './view/filters.js'
import appView from './view/app.js'
import applyDiff from './applyDiff.js'

import registry from './registry.js'

registry.add('app', appView)
registry.add('todos', todosView)
registry.add('counter', counterView)
registry.add('filters', filtersView)

const state = {
	todos: [],
	currentFilter: 'All'
}

const events = {
  addItem: text => {
    state.todos.push({
			text,
			completed: false	
		})
		render()
  },
  updateItem: (index, text) => {
    state.todos[index].text = text
		render()
  },
  deleteItem: (index) => {
    state.todos.splice(index, 1)
		render()
  },
  toggleItemCompleted: (index) => {
    const {
			completed
		} = state.todos[index]
		state.todos[index].completed = !completed
		render()
  },
  completeAll: () => {
    state.totos.forEach(t => {
			t.completed = true
		})
		render()
  },
  clearCompleted: () => {
    state.todos = state.todos.filter(
			t => !t.completed
		)
		render()
  },
  changeFilter: filter => {
    state.currentFilter = filter
		render()
  }
}

const render = (state) => {
  window.requestAnimationFrame(() => {
    const main = document.querySelector('#root')

    const newMain = registry.renderRoot(
      main,
      state,
      events)

    applyDiff(document.body, main, newMain)
  })
}

render()
```

## 모델-뷰-컨트롤러

하지만 상태를 컨트롤러에서 유지하는 것은 좋은 방법이 아니다. 상태를 컨트롤러에서 유지하는 대신에, 이 코드를 별도의 파일로 옮겨 디자인을 향상시킬 수 있다. 다음은 애플리케이션의 상태를 외부 모델에서 관리하는 컨트롤러다.

```jsx
import todosView from './view/todos.js'
import counterView from './view/counter.js'
import filtersView from './view/filters.js'
import appView from './view/app.js'
import applyDiff from './applyDiff.js'

import registry from './registry.js'

import modelFactory from './model/model.js'

registry.add('app', appView)
registry.add('todos', todosView)
registry.add('counter', counterView)
registry.add('filters', filtersView)

const model = modelFactory()

const events = {
  addItem: text => {
    model.addItem(text)
    render(model.getState())
  },
  updateItem: (index, text) => {
    model.updateItem(index, text)
    render(model.getState())
  },
  deleteItem: (index) => {
    model.deleteItem(index)
    render(model.getState())
  },
  toggleItemCompleted: (index) => {
    model.toggleItemCompleted(index)
    render(model.getState())
  },
  completeAll: () => {
    model.completeAll()
    render(model.getState())
  },
  clearCompleted: () => {
    model.clearCompleted()
    render(model.getState())
  },
  changeFilter: filter => {
    model.changeFilter(filter)
    render(model.getState())
  }
}

const render = (state) => {
  window.requestAnimationFrame(() => {
    const main = document.querySelector('#root')

    const newMain = registry.renderRoot(
      main,
      state,
      events)

    applyDiff(document.body, main, newMain)
  })
}

render(model.getState())
```

렌더링에 사용되는 데이터는 `model` 객체의 `getState` 메서드에서 반환된다. 아래에서 코드를 확인할 수 있다.

```jsx
const cloneDeep = x => {
  return JSON.parse(JSON.stringify(x))
}

const INITIAL_STATE = {
  todos: [],
  currentFilter: 'All'
}

export default (initalState = INITIAL_STATE) => {
  const state = cloneDeep(initalState)

  const getState = () => {
    return Object.freeze(cloneDeep(state))
  }

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

  return {
    addItem,
    updateItem,
    deleteItem,
    toggleItemCompleted,
    completeAll,
    clearCompleted,
    changeFilter,
    getState
  }
}
```

`model` 객체에서 `getState`로 받아온 값은 `Object.freeze`로 인해 불변성을 가진다. 여기서는 객체를 복제하기 위해 `JSON` 객체의 메서드들을 사용했는데, 이 방법은 단순하지만 느리기 때문에 실제 애플리케이션은 보통 Lodash의 `cloneDeep` 함수를 사용한다.

불변성을 사용해 데이터를 전송하는 경우 이 API의 사용자는 상태를 조작하는 데에 퍼블릭 메서드를 사용해야 한다. 이런 로직이 `Model` 객체에 완전히 포함되어 있으면 애플리케이션의 다른 부분에 상태를 관리하는 코드가 흩어지는 걸 막을 수 있기 때문에 높은 수준의 테스트 가능성을 유지할 수 있다.

```jsx
import modelFactory from './model.js'

describe('TodoMVC Model', () => {
  test('data should be immutable', () => {
    const model = modelFactory()

    expect(() => {
      model.getState().currentFilter = 'WRONG'
    }).toThrow()
  })

  test('should add an item', () => {
    const model = modelFactory()

    model.addItem('dummy')

    const { todos } = model.getState()

    expect(todos.length).toBe(1)
    expect(todos[0]).toEqual({
      text: 'dummy',
      completed: false
    })
  })

  test('should not add an item when a falsy text is provided', () => {
    const model = modelFactory()

    model.addItem('')
    model.addItem(undefined)
    model.addItem(0)
    model.addItem()
    model.addItem(false)

    const { todos } = model.getState()

    expect(todos.length).toBe(0)
  })

  test('should update an item', () => {
    const model = modelFactory({
      todos: [{
        text: 'dummy',
        completed: false
      }]
    })

    model.updateItem(0, 'new-dummy')

    const { todos } = model.getState()

    expect(todos[0].text).toBe('new-dummy')
  })

  test('should not update an item when an invalid index is provided', () => {
    const model = modelFactory({
      todos: [{
        text: 'dummy',
        completed: false
      }]
    })

    model.updateItem(1, 'new-dummy')

    const { todos } = model.getState()

    expect(todos[0].text).toBe('dummy')
  })
})
```

TodoMVC 애플리케이션을 위한 상태 관리 라이브러리의 첫 번째 버전은 고전적인 MVC 구현이다. 역사적으로 MVC는 클라이언트 애플리케이션의 상태 관리에 사용된 첫 번째 패턴 중 하나다. 우리가 구현한 MVC 패턴의 스키마는 다음과 같다.

![https://i.ibb.co/c8H5t8T/Frame-82.png](https://i.ibb.co/c8H5t8T/Frame-82.png)

이 모델 객체는 다른 구현들의 기반이 되므로, 애플리케이션의 워크플로우와 각 부분들 간의 관계를 살펴보자.

1. 컨트롤러는 모델에서 초기 상태를 가져온다.
2. 컨트롤러는 뷰를 호출해 초기 상태를 렌더링한다.
3. 시스템이 사용자 입력을 받을 준비를 한다.
4. 사용자가 어떤 동작을 수행한다(예 - 항목 추가).
5. 컨트롤러는 올바른 `Model` 메서드(`model.addItem`)로 사용자의 동작과 매핑한다.
6. 모델이 상태를 업데이트한다.
7. 컨트롤러는 모델에서 새로운 상태를 얻는다.
8. 컨트롤러는 뷰를 호출해 새로운 상태를 렌더링한다.
9. 시스템이 사용자 입력을 받을 준비가 되었다.

### 옵저버블 모델

MVC를 기반으로 작성한 첫 번째 상태 관리 코드는 잘 동작하나, 사용자가 동작을 수행할 때마다 `Render` 메서드를 수동으로 호출해야 하기 때문에 모델과 컨트롤러 간의 통합이 완벽하지 않다. 또한 상태 변경 후에 렌더링을 수동으로 호출하는 방법은 오류가 발생하기 쉬운 접근 방식이다. 마지막으로 동작이 상태를 변경하지 않을 때(예 - 빈 항목을 리스트에 추가)에도 `render` 메서드가 호출된다. 이런 문제는 옵저버 패턴을 기반으로 하는 모델을 통해 해결할 수 있다. 

```jsx
// model.js
const cloneDeep = x => {
  return JSON.parse(JSON.stringify(x))
}

const freeze = x => Object.freeze(cloneDeep(x))

const INITIAL_STATE = {
  todos: [],
  currentFilter: 'All'
}

export default (initalState = INITIAL_STATE) => {
  const state = cloneDeep(initalState)
  let listeners = []

  const addChangeListener = listener => {
    listeners.push(listener)

    listener(freeze(state))

    return () => {
      listeners = listeners.filter(l => l !== listener)
    }
  }

  const invokeListeners = () => {
    const data = freeze(state)
    listeners.forEach(l => l(data))
  }

  const addItem = text => {
    if (!text) {
      return
    }

    state.todos.push({
      text,
      completed: false
    })

    invokeListeners()
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

    invokeListeners()
  }

  const deleteItem = index => {
    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos.splice(index, 1)

    invokeListeners()
  }

  const toggleItemCompleted = index => {
    if (index < 0) {
      return
    }

    if (!state.todos[index]) {
      return
    }

    state.todos[index].completed = !state.todos[index].completed

    invokeListeners()
  }

  const completeAll = () => {
    state.todos.forEach(t => {
      t.completed = true
    })

    invokeListeners()
  }

  const clearCompleted = () => {
    state.todos = state.todos.filter(t => !t.completed)
    invokeListeners()
  }

  const changeFilter = filter => {
    state.currentFilter = filter
    invokeListeners()
  }

  return {
    addItem,
    updateItem,
    deleteItem,
    toggleItemCompleted,
    completeAll,
    clearCompleted,
    changeFilter,
    addChangeListener
  }
}
```

```jsx
// model.test.js
import modelFactory from './model.js'
let model

describe('observable model', () => {
  beforeEach(() => {
    model = modelFactory()
  })

  test('listeners should be invoked immediatly', () => {
    let counter = 0
    model.addChangeListener(data => {
      counter++
    })
    expect(counter).toBe(1)
  })

  test('listeners should be invoked when changing data', () => {
    let counter = 0
    model.addChangeListener(data => {
      counter++
    })
    model.addItem('dummy')
    expect(counter).toBe(2)
  })

  test('listeners should be removed when unsubscribing', () => {
    let counter = 0
    const unsubscribe = model.addChangeListener(data => {
      counter++
    })
    unsubscribe()
    model.addItem('dummy')
    expect(counter).toBe(1)
  })

  test('state should be immutable', () => {
    model.addChangeListener(data => {
      expect(() => {
        data.currentFilter = 'WRONG'
      }).toThrow()
    })
  })
})
```

테스트 코드를 읽고 나면 `Model` 객체에서 상태를 얻는 유일한 방법은 리스너 콜백 추가라는 걸 확실히 알 수 있다. 이 콜백은 가입할 때와 내부 상태가 변경될 때마다 호출된다. 이런 방식을 적용하면 컨트롤러를 단순화할 수 있다.
