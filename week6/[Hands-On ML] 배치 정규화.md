## 11.1.3 배치 정규화

- 배치 정규화: 그레디언트 소실과 폭주 문제 해결 방법 중 하나이다.

  - 장점

    - 수렴성 활성화 함수 사용 가능 (하이퍼볼릭 탄젠트나 로지스틱 활성화 함수)

    - 훨씬 큰 학습률을 사용하여 학습 과정의 속도를 크게 높일 수 있다.

    - 규제와 같은 역할을 하여 다른 규제 기법의 필요성을 줄여준다.

  - 단점

    - 모델의 복잡도를 키운다.

    - 훈련 실행 속도가 느리다. => but 훈련이 끝난 후에 이전 층과 배치 정규화 층을 합쳐 실행 속도 저하를 피할 수 있다. 수렴이 빨라 상쇄 가능!

- 각 층에서 활성화 함수 통과 전이나 후에 1. 입력을 정규화한 다음, 2. 스케일을 조정하고 3. 이동시킨다.

  - 파라미터 => 하나는 스케일 조정, 하나는 이동시킨다.

  - 결과적으로 4개의 파라미터를 추가한다.

    - 입력의 평균 벡터 μ

    - 입력의 표준편차 벡터 σ

    - 층의 출력 스케일 벡터 γ

    - 층의 출력 이동 벡터 β  

      ![image](https://user-images.githubusercontent.com/89712324/218304785-7d8f490a-118f-40c4-b7dd-ff788a8a8bba.png)

  - 지수 이동 평균을 사용해 추정

    - μ, σ

  - 역전파를 통해 학습

    - γ, β

- 구현: BatchNormalization 층을 은닉층의 활성화 함수 전이나 후에 추가하면 된다.


### 케라스로 배치 정규화 구현하기

- 아래 코드는 은닉층 2개를 가진 작은 예제라 배치 정규화가 큰 도움이 되지 않을 수 있다. 깊은 네트워크에서는 엄청난 차이를 만들 수 있다.



```python
import keras
model = keras.models.Sequential([
    keras.layers.Flatten(input_shape=[28, 28]),
    keras.layers.BatchNormalization(),
    keras.layers.Dense(300, activation='elu', kernel_initializer='he_normal'),
    keras.layers.BatchNormalization(),
    keras.layers.Dense(100, activation='elu', kernel_initializer='he_normal'),
    keras.layers.BatchNormalization(),
    keras.layers.Dense(10, activation='softmax')
])
```


```python
model.summary()
```

<pre>
Model: "sequential_1"
_________________________________________________________________
 Layer (type)                Output Shape              Param #   
=================================================================
 flatten_1 (Flatten)         (None, 784)               0         
                                                                 
 batch_normalization_3 (Batc  (None, 784)              3136      
 hNormalization)                                                 
                                                                 
 dense_3 (Dense)             (None, 300)               235500    
                                                                 
 batch_normalization_4 (Batc  (None, 300)              1200      
 hNormalization)                                                 
                                                                 
 dense_4 (Dense)             (None, 100)               30100     
                                                                 
 batch_normalization_5 (Batc  (None, 100)              400       
 hNormalization)                                                 
                                                                 
 dense_5 (Dense)             (None, 10)                1010      
                                                                 
=================================================================
Total params: 271,346
Trainable params: 268,978
Non-trainable params: 2,368
_________________________________________________________________
</pre>
- 첫 번째 배치 정규화 층은 4x784=3136개의 파라미터로 구성되었고, 두 번째 정규화 층은 4x300=1200개의 파라미터로 구성되었다. 4는 배치 정규화에서 추가된 파라미터이다.

- 배치 정규화 파라미터의 전체 개수 = 3136+1200+400


첫 번째 배치 정규화 층의 파라미터를 살펴보자



```python
# 첫 번째 배치 정규화 층의 파라미터를 살펴보기
[(var.name, var.trainable) for var in model.layers[1].variables]
```

<pre>
[('batch_normalization_3/gamma:0', True),
 ('batch_normalization_3/beta:0', True),
 ('batch_normalization_3/moving_mean:0', False),
 ('batch_normalization_3/moving_variance:0', False)]
</pre>
- γ, β는 역전파로 훈련되고, μ, σ는 역전파로 훈련되지 않는다.

- 배치 정규화 층 훈련 시 매 반복마다 2개의 연산이 함께 생성되며, 이동 평균을 업데이트한다.


**TIP**

- 일반적으로 활성화 함수 이후보다 활성화 함수 이전에 배치 정규화 층을 추가하는 것이 좋다. 그러나 두 가지 방법 모두 해보고 데이터셋에 더 잘 맞는 방법을 채택하자.

- 활성화 함수 이전에 배치 정규화층을 추가하려면 활성화함수를 별도의 층으로 추가해야한다.

- 배치 정규화 층은 입력마다 이동 파라미터를 포함하기 때문에 이전 층에서 편향을 뺼 수 있다.



```python
model = keras.models.Sequential([
    keras.layers.Flatten(input_shape=[28, 28]),
    keras.layers.BatchNormalization(),
    keras.layers.Dense(300, kernel_initializer='he_normal', use_bias=False),
    keras.layers.BatchNormalization(),
    keras.layers.Activation('elu'),
    keras.layers.Dense(100, kernel_initializer='he_normal', use_bias=False),
    keras.layers.BatchNormalization(),
    keras.layers.Activation('elu'),
    keras.layers.Dense(10, activation='softmax')
])
```

**TIP**

- BatchNormalization의 하이퍼파라미터 

  - 가끔 momentum 매개변수를 변경해야할 수 있다.

    - BatchNormalization층이 지수 이동 평균을 업데이트할 때 momentum 하이퍼파라미터를 사용한다.

  - axis 하이퍼파라미터: 이 매개변수는 정규화할 축을 결정한다. 기본값은 -1이다.

- 심층 신경망에서 널리 사용하는 층이라 신경망 그림에서 빠져있을 수 있다.

- 하지만 새로운 Fixup 가중치 초기화 기법을 사용하여 배치 정규화 없이 매우 깊은 심층 신경망을 훈련해 복잡한 이미지 분류 작업에서 최고의 성능을 달성한 사례가 있었다. => but 뒷받침할 추가 연구가 나온 뒤에 배치 정규화를 제거하는 것이 좋다.
