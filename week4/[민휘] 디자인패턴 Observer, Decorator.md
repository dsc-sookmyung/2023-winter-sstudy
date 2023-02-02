
# Observer

## 핵심 의도

**한 객체의 상태가 바뀌면 그 객체에 의존하는 다른 객체에게 연락이 가고 자동으로 내용이 갱신되는 방식**으로 일대다 의존성을 정의하는 패턴이다.

> 신문 구독 매커니즘과 유사하다! 신문사는 사업을 시작하고 신문을 찍어낸다. 독자가 구독 신청을 하면 새로운 신문이 나올 때마다 배달을 받을 수 있다. 신문을 더 이상 보고 싶지 않으면 구독 해지 신청을 한다. 옵저버 패턴에서는 신문사를 Subject, 구독자를 Observer라고 부른다.
> 

## 적용 상황

특정 상태의 쓰기와 읽기 책임이 명확하게 분리되는 경우 사용할 수 있다. 특히 쓰기는 한 곳에서 발생하는데 읽기는 여러 곳에서 발생하면 의존 관계를 더 유연하게 구성할 수 있다.

## 솔루션의 구조와 각 요소의 역할

### 객체에게 책임을 분할하기

상태를 저장하고 제어(write)하는 객체는 Subject 뿐이며, Observer는 상태 변화를 기다리고 사용하는(read) 의존 객체이다. 그래서 ***Subject는 상태를 관리하는 책임, 옵저버 목록을 관리하고 연락하는 책임을 가진다. 옵저버는 Subject로부터 연락을 받는 책임과 변경된 상태를 사용하는 책임을 가진다.***

### 구현 포인트

**느슨한 결합**이 핵심이다. 느슨한 결합은 객체들이 상호작용할 수는 있지만, 서로를 잘 모르는 관계를 의미한다. 객체가 상호작용할 때 서로의 책임만 알고 있으므로, 내부 구현이 바뀌어도 잘 동작하는 유연한 설계를 가진다. 옵저버 패턴에서는 새로운 유형의 옵저버를 추가하거나 기존 옵저버를 삭제해도 상관 없고 재사용도 가능하다. 다만 두 객체는 공통 상태를 공유하고 있으므로, 이 상태가 변경되면 기존 구조를 유지할 수는 없다.

## 적용 예시

### 요구사항

기상 스테이션에서 실시간으로 날씨 정보를 측정할 때마다 WeatherData의 상태가 변경된다. 변경이 발생할 때마다 measurementsChanged()가 호출된다. 우리는 변경된 상태를 디스플레이에 반영해야한다.

### 설계

공유하고자 하는 상태는 날씨 정보이다. 이 정보를 관리하고 받아오는 책임은 WeatherData에게 있으므로 WeatherData가 Subject이고, 상태에 관심이 있는 디스플레이 객체들이 Object가 된다.

WeatherData는 Subject 인터페이스를 구현하고, 디스플레이들은 Observer 인터페이스를 구현한다. 인터페이스에는 공통 책임을 추상 메소드로 명시한다. 구체적인 날씨 정보를 관리하는 책임은 Subject로부터 분리하여 WeatherData에 할당한다.

Subject는 옵저버 목록을 관리하고 연락을 돌리는 메소드를, Oberver에는 연락을 받는 메소드를 넣는다. 이때 연락을 받아 바뀐 날씨 정보를 사용하는 부분은 디스플레이 타입의 객체들만 가지는 책임이므로 별도의 DisplayElement 인터페이스로 분리한다. 

이 예제에서는 날씨 정보 외에 다른 상태에 대해서도 옵저버 패턴을 활용할 수 있도록 공통 책임을 가진 Subject, Observer과 구체적인 책임을 가진 WeatherData와 Display로 구분하였다.

## 코드

Subject

```java
// 옵저버 관리와 연락 돌리는 책임
public interface Subject {
	public void registerObserver(Observer o);
	public void removeObserver(Observer o);
	public void notifyObservers();
}
```

Observer

```java
// 연락을 받아 변경된 상태를 받아오는 책임
public interface Observer {
	public void update(float temp, float humidity, float pressure);
}
```

DisplayElement

```java
// 디스플레이에서 날씨 정보를 보여주는 책임
public interface DisplayElement {
	public void display();
}
```

WeatherData

```java
// (인터페이스) 옵저버 목록을 관리하고 연락 돌리는 책임
// 날씨 정보를 저장하는 책임
public class WeatherData implements Subject {
	private List<Observer> observers;
	private float temperature;
	private float humidity;
	private float pressure;
	
	public WeatherData() {
		observers = new ArrayList<Observer>();
	}
	
	public void registerObserver(Observer o) {
		observers.add(o);
	}
	
	public void removeObserver(Observer o) {
		observers.remove(o);
	}
	
	public void notifyObservers() {
		for (Observer observer : observers) {
			observer.update(temperature, humidity, pressure);
		}
	}
	
	public void measurementsChanged() {
		notifyObservers();
	}
	
	public void setMeasurements(float temperature, float humidity, float pressure) {
		this.temperature = temperature;
		this.humidity = humidity;
		this.pressure = pressure;
		measurementsChanged();
	}

	public float getTemperature() {
		return temperature;
	}
	
	public float getHumidity() {
		return humidity;
	}
	
	public float getPressure() {
		return pressure;
	}

}
```

CurrentConditionDisplay

```java
public class CurrentConditionsDisplay implements Observer, DisplayElement {
	private float temperature;
	private float humidity;
	private WeatherData weatherData;

	// 생성 시에 옵저버로 등록
	public CurrentConditionsDisplay(WeatherData weatherData) {
		this.weatherData = weatherData;
		weatherData.registerObserver(this);
	}
	
	public void update(float temperature, float humidity, float pressure) {
		this.temperature = temperature;
		this.humidity = humidity;
		display();
	}
	
	public void display() {
		System.out.println("Current conditions: " + temperature 
			+ "F degrees and " + humidity + "% humidity");
	}
}
```

WeatherSation (엔트리)

```java
public class WeatherStation {

	public static void main(String[] args) {
		WeatherData weatherData = new WeatherData();
	
		CurrentConditionsDisplay currentDisplay = 
			new CurrentConditionsDisplay(weatherData);
		StatisticsDisplay statisticsDisplay = new StatisticsDisplay(weatherData);
		ForecastDisplay forecastDisplay = new ForecastDisplay(weatherData);

		weatherData.setMeasurements(80, 65, 30.4f);
		weatherData.setMeasurements(82, 70, 29.2f);
		weatherData.setMeasurements(78, 90, 29.2f);
		
		weatherData.removeObserver(forecastDisplay);
		weatherData.setMeasurements(62, 90, 28.1f);
	}
}
```

## 연락 방식의 차이 : Push 방식과 Pull 방식

위의 코드는 Push 방식을 따르고 있다. Subject에서 변경 사항이 발생할 때마다 옵저버가 변경된 상태를 수신하는 방법이다. 이 방법은 Subject 중심으로 연락을 돌리기 때문에, 옵저버가 필요할 시점에 필요한 정보만 불러올 수 없다는 단점이 있다.

기존 코드를 보면 다음과 같이 모든 날씨 정보를 담아 연락을 돌린다.

```java
public interface Observer {
	public void update(float temp, float humidity, float pressure);
}
```

옵저버가 필요한 시점에 필요한 정보를 가져오기 위해 pull 방식을 사용한다. update()로 상태가 변경되었다는 알림을 받으면 그때 필요한 정보를 직접 가져온다. Subject가 알려주는 정보의 일부만을 옵저버가 사용한다면 pull 방식이 더 효과적이다.

```java
public interface Observer {
	public void update();
}

public void notifyObservers() {
	for (Observer observer : observers) {
		observer.update();
	}
}

public void update() {
	this.temperature = weatherData.getTemperature;
	this.humidity = weatherData.getHumidity;
	display();
}
```

# Decorator

## 핵심 의도

데코레이터 패턴으로 객체에 추가 요소를 실행 중에 동적으로 더할 수 있다. 데코레이터를 사용하면 서브클래스를 만들 때보다 훨씬 유연하게 기능을 확장할 수 있다.

## 적용 상황

감싸고 있는 객체에 행동을 추가하거나 위임하는 용도로 만들어진다. 예를 들어 문자열을 다양한 방법으로 출력하고 싶을 때 사용할 수 있다. 문장 앞에 줄번호를 붙이거나, 소문자로 바꾸어 출력하거나, 박스로 감싸서 출력할 수 있다. 이때 문자열은 감싸지는 객체이고 추가하는 행동은 데코레이터의 책임이다.

## 솔루션의 구조와 각 요소의 역할

### 객체에게 책임을 분할하기

어떤 객체와 상호작용하는 다른 객체들은 그 객체의 장식의 유무와 상관없이 그 객체에게 메시지를 보낸다. 데코레이터에게도 원래 객체와 동일한 메시지 요청이 오므로, 이 메시지를 수신하려면 데코레이터와 감싸지는 객체는 같은 타입이어야 한다.

감싸지는 객체의 책임은 ConcreateComponent에, 장식의 책임은 Decorator에 할당한다. 이 둘은 같은 타입이어야 하므로 Component를 구현하거나 상속한다. 장식 기능의 구체적인 구현은 ConcreteDecorator에서 담당한다.

### 구현 포인트

**데코레이터 패턴은 OCP 규칙을 효과적으로 지킬 수 있는 패턴이다.** OCP는 기존 코드를 변경하지 않고 확장으로 새로운 행동을 추가하는 기법이다. 요구사항이 변경이 발생하는 상황에서 유연하게 코드를 관리할 수 있다.

## 적용 예시

### 요구사항

커피의 주문 시스템 구축하기. 현재 커피의 종류는 HouseBlend, DarkRoast, Decaf, Espresso가 있고 추가할 수 있는 메뉴는 Milk, Soy, Mocha, Whip가 있다.

### 설계

Beverage (감싸지는 객체와 데코레이터의 추상 클래스)

```java
public abstract class Beverage {
	String description = "Unknown Beverage";
  
	public String getDescription() {
		return description;
	}
 
	public abstract double cost();
}
```

CondimentDecorator (데코레이터 추상 클래스)

```java
public abstract class CondimentDecorator extends Beverage {
	Beverage beverage;
	public abstract String getDescription();
}
```

DarkRoast (감싸지는 객체)

```java
public class DarkRoast extends Beverage {
	public DarkRoast() {
		description = "Dark Roast Coffee";
	}
 
	public double cost() {
		return .99;
	}
}
```

Milk (데코레이터 객체)

```java
public class Milk extends CondimentDecorator {
	public Milk(Beverage beverage) {
		this.beverage = beverage;
	}

	public String getDescription() {
		return beverage.getDescription() + ", Milk";
	}

	public double cost() {
		return .10 + beverage.cost();
	}
}
```

Entry Point

```java
public static void main(String args[]) {
	Beverage beverage = new Espresso();
	System.out.println(beverage.getDescription() 
			+ " $" + beverage.cost());
 
	Beverage beverage2 = new DarkRoast();
	beverage2 = new Mocha(beverage2);
	beverage2 = new Mocha(beverage2);
	beverage2 = new Whip(beverage2);
	System.out.println(beverage2.getDescription() 
			+ " $" + beverage2.cost());
 
	Beverage beverage3 = new HouseBlend();
	beverage3 = new Soy(beverage3);
	beverage3 = new Mocha(beverage3);
	beverage3 = new Whip(beverage3);
	System.out.println(beverage3.getDescription() 
			+ " $" + beverage3.cost());
}
```

데코레이터 패턴을 적용한 객체와 데코레이터들은 외부에서 받은 메시지 요청을 내부에서 연쇄적으로 처리한다.
