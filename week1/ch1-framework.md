# 1장. 프레임워크에 대한 이야기

# 프레임워크의 정의

프레임워크는 **무언가를 만들 수 있는 지지 구조**로, 우리는 프레임워크에서 제공하는 구성 요소와 기본 요소들을 가지고 서비스를 만들 수 있다. 프레임워크는 사용 언어, 의존성 주입 방법, 디렉터리 구조 등을 문법적으로 강제하거나, 프레임워크 커뮤니티 내에서 만든 ‘사실상’ 강제되는 몇 가지 제약 조건을 갖고 있으며, 프레임워크를 사용하려는 개발자는 이 제약 조건에 맞춰 코드를 작성해야 한다. 

## 프레임워크 대 라이브러리 - 비교로 간단하게 알아보기

<aside>
💡 프레임워크는 코드를 호출한다. 코드는 라이브러리를 호출한다.

</aside>

우리가 웹 서비스를 개발할 때 사용하는 프레임워크 중 하나인 **Angular**와 날짜를 다루는 라이브러리인 **Moment.js**를 비교해 보면 프레임워크와 라이브러리의 차이점을 이해할 수 있다. 앵귤러는 프레임워크로서, 따라야 하는 앵귤러만의 규칙이 있다. 아래 앵귤러 코드를 살펴보자.

```jsx
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

const URL = 'http://example.api.com';

@Injectable({
	providedIn: 'root',
})

export class PeopleService {
	constructor(private http: HttpClient) {}
	list() {
		return this.http.get(URL);
	}
}
```

```jsx
import { Component, OnInit } from '@angular/core';
import { PeopleService } from '../people.service';

@Component({
	selector: 'people-list',
	templateUrl: './people-list.component.html'
})

export class PeopleListComponent implements OnInit {
	constructor(private peopleService: PeopleService) { }

	ngOnInit() {
		this.loadList();
	}

	loadList(): void {
		this.peopleService.getHeroese().subscribe(people => this.people = people);
	}
}
```

`PeopleListComponent`이 `PeopleService`와 상호작용할 수 있도록 생성자(constructor)에 `PeopleService` 인스턴스를 넣어야 하고, `PeopelService`의 의존성 주입을 위해 `@Injectable` 이노테이션을 사용해야 한다. 이처럼 프레임워크인 앵귤러는 개발자가 코드로 채울 수 있는 **구조**를 제공하고, 앵귤러 표준 작업에 도움이 되는 유틸리티 세트를 제공한다.

이제 라이브러리인 `Moment.js`의 사용 예시 코드를 살펴보자.

```jsx
import moment from 'moment';

const DATE_FORMAT = 'DD/MM/YYYY';

export const formatDate = date => {
	return moment(date).format(DATE_FORMAT);
}
```

Moment.js는 앵귤러와 다르게 구조에 대한 강요를 하지 않는다. 그저 가져와 사용하는 것이 라이브러리의 전부다. 이 예시로 ‘프레임워크는 코드를 호출하며, 코드는 라이브러리를 호출한다’는 말이 이해가 될 것이다.

<aside>
💡 **프레임워크**는 구조를 제공하며 강요하고, 구조를 따를 수 있는 유틸리티 세트를 제공하는 반면,
**라이브러리**는 구성에 대한 강요가 없으며 그저 코드로 가져와 사용하면 된다.

</aside>

# 리액트는 프레임워크인가?

흔히 프론트엔드 삼대장으로 Angular, Vue, React가 묶여 불린다. 여기서 Angular와 Vue가 프레임워크기 때문에 자연스럽게 React 또한 프레임워크라고 생각하기 쉬우나(나도 리액트가 프레임워크인 줄 알았다) 리액트 공식 문서에서는 리액트를 ‘사용자 인터페이스 구축을 위한 자바스크립트 **라이브러리’**라고 정의하고 있다. 하지만 리액트에는 분명 단순 라이브러리라고 보기에는 어려운 점이 있다.

## 리액트는 선언적 패턴을 사용한다.

리액트는 DOM을 리액트의 방식으로 추상화하고, 사용자는 DOM에 직접 접근하는 대신 컴포넌트의 상태를 수정하고, 리액트는 사용자 대신 DOM을 수정하는 선언적 패러다임을 사용한다. 필자는 리액트가 DOM을 추상화하고 유저가 컴포넌트를 통해 간접적으로 DOM을 수정하는 방식, 그리고 state의 사용 방법에 대한 리액트 커뮤니티의 권장 방식(선언적 패턴)이 리액트의 제약 사항이라고 말한다. 리액트의 선언적 패턴은 리액트가 라이브러리보다 프레임워크에 가깝게 느껴지는 이유라고 생각한다.

### 참고 - 선언형 대 명령형

[명령형 vs 선언형 프로그래밍](https://iborymagic.tistory.com/73)

💡 *명령형(절차적) 프로그래밍은 당신이 어떤 일을 **어떻게** 할 것인가에 관한 것이고, 선언적 프로그래밍은 당신이 **무엇을** 할 것인가에 관한 것입니다.*


**명령형 접근**(**HOW**): "주차장 북쪽 출구로 나와서 좌회전을 해. 12번가 출구에 도착할 때까지I-15 북쪽 도로를 타고 와야 해. 거기서 IKEA에 가는 것처럼 출구에서 우회전을 해. 그리고 거기서직진하다가 첫 번째 신호등에서 우회전을 해. 그 다음에 나오는 신호등을 통과한 후에 좌회전을 하면 돼.우리 집은 #298 이야."

**선언형 접근**(**WHAT**): "우리 집 주소는 298 West Immutable Alley, Eden, Utah 84310 이야."
