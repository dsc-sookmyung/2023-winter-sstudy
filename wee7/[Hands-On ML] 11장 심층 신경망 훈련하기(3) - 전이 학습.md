"핸즈온 머신러닝"의 "11장 심층 신경망 훈련하기" 파트를 읽고 공부한 내용을 정리해보았다. 사전훈련된 층을 재사용하는 전이 학습에 대해 다뤄볼 것이다.<br>

## 11.2 사전훈련된 층 재사용하기  

**전이 학습(transfer learning)**  

- 기존에 존재하는 신경망의 하위층을 재사용하는 것이다.

- 장점: 훈련 속도를 크게 높일 수 있고, 필요한 훈련 데이터도 크게 줄여준다.  



<br>



**방법**  

- 보통 원본 모델의 출력층을 바꿔야한다.

- 작업이 비슷할 수록 더 많은 층을 재사용하자!  

![image](https://user-images.githubusercontent.com/89712324/219865918-f9e14f2c-0fac-4797-8414-4297d722ec40.png)

1. 재사용하는 층을 모두 동결한다.

  - 경사 하강법으로 가중치가 바뀌지 않도록 훈련되지 않는 가중치로 만든다.

2. 모델을 훈련하고 성능을 평가한다.

  - 맨 위에 있는 한두개의 은닉층의 동결을 해제하고 역전파를 통해 가중치를 조정하여 성능이 향상되는지 확인한다.

  - 훈련 데이터가 많을 수록 많은 층의 동결을 해제할 수 있다.

  - 동결을 해제할 때는 학습률을 줄여 세밀하게 튜닝한다.

3. 그래도 성능이 안 나오고, 훈련 데이터가 적다면, 상위 은닉층들을 제거하고 남은 은닉층을 동결하고, 훈련 데이터가 많다면 은닉층을 제거하는 대신 다른 것으로 바꾸거나 더 많은 은닉층을 추가할 수 있다.

<br>

### 11.2.1 케라스를 사용한 전이 학습

8개의 클래스를 분류하는 모델을 2개의  클래스를 분류하는 모델로 만들어보자.  


#### my_model_A 만들기



```python
import keras
import tensorflow as tf
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split

fashion = tf.keras.datasets.fashion_mnist
(X_train_full, y_train_full), (X_test, y_test) = fashion.load_data()
X_train, X_valid, y_train, y_valid = train_test_split(X_train_full, y_train_full)
class_names = ['T-shirt/top', 'Trouser', 'Pullover', 'Dress', 'Coat', 'Sandal', 'Shirt', 'Sneaker', 'Bag', 'Ankle boot']

plt.figure()
plt.imshow(X_train[3])
plt.colorbar()
plt.grid(False)
```

<br>

```python
# 이미지 정규화
train_images = X_train / 255.0
test_images = X_test / 255.0

plt.figure(figsize=(10, 10))
for i in range(25):
  plt.subplot(5, 5, i+1)
  plt.xticks([])
  plt.yticks([])
  plt.grid(False)
  plt.imshow(X_train[i], cmap=plt.cm.binary)
  plt.xlabel(class_names[y_train[i]])
```

<br>


```python
model = keras.models.Sequential([
    keras.layers.Flatten(input_shape=X_train.shape[1:]),
    keras.layers.Dense(128, activation='relu'),
    keras.layers.Dense(10, activation='softmax')
])
```


<br>

```python
model.compile(optimizer='adam',
              loss='sparse_categorical_crossentropy',
              metrics=['accuracy'])
model.fit(X_train, y_train, epochs=20, validation_data = (X_valid, y_valid))
```
<br>


```python
model.save('my_model_A.h5')
```


<br>

```python
model_A = keras.models.load_model('my_model_A.h5')
model_B_on_A = keras.models.Sequential(model_A.layers[:-1]) # [:-1] -> 마지막 문자를 제외하고 모두 출력
model_B_on_A.add(keras.layers.Dense(1, activation='sigmoid'))
```

**코드 설명**  

model_B_on_A를 훈련할 때 model_A도 영향을 받는다.  

model_A에게 영향 주지 않으려면 층을 재사용하기 전에 model_A를 클론해야한다.

- clone_model(): 모델 A의 구조를 복제한 후

- set_weights(): 가중치를 복사한다.

<br>



```python
# model_A 클론하기
model_A_clone = keras.models.clone_model(model_A)
model_A_clone.set_weights(model_A.get_weights())
model_B_on_A = keras.models.Sequential(model_A_clone.layers[:-1]) # [:-1] -> 마지막 문자를 제외하고 모두 출력
model_B_on_A.add(keras.layers.Dense(1, activation='sigmoid'))
```

재사용된 층의 가중치가 망가지지 않도록 하기 위해, 초기 에포크 동안 재사용된 층을 동결하고 새로운 층의 가중치를 학습하자.


<br>


```python
# 재사용된 층 동결
for layer in model_B_on_A.layers[:-1]:
  layer.trainable = False

model_B_on_A.compile(loss='binary_crossentropy', optimizer='sgd',
                     metrics=['accuracy'])
```

동결 해제한 후에도 모델을 다시 컴파일해야하며, 학습률을 낮추는 것이 좋다.

<br>



#### 모델B를 위한 데이터를 만들자.  

모델B는 샌들인지 구분하는 모델이다.



```python
class_names.index('Sandal')
```

<pre>
5
</pre>

```python
y_train_B = (y_train == 5)
y_test_B = (y_test == 5)
y_valid_B = (y_valid == 5)
X_train.shape, y_train_B.shape
```

<pre>
((45000, 28, 28), (45000,))
</pre>

```python
# 4번의 에포크 후에 재사용된 레이어 동결 해제하기 & 학습률 낮추기
history = model_B_on_A.fit(X_train, y_train_B, epochs=4, validation_data = (X_valid, y_valid_B))

for layer in model_B_on_A.layers[:-1]:
  layer.trainable = True

optimizer = keras.optimizers.SGD(learning_rate=1e-4) # 기본 학습률은 1e-2
model_B_on_A.compile(loss='binary_crossentropy', optimizer=optimizer,
                     metrics=['accuracy'])
history = model_B_on_A.fit(X_train, y_train_B, epochs=16,
                           validation_data=(X_valid, y_valid_B))
```

<br>

```python
model_B_on_A.evaluate(X_test, y_test_B)
```

<pre>
313/313 [==============================] - 2s 5ms/step - loss: 0.6444 - accuracy: 0.9882
</pre>
<pre>
[0.6444016098976135, 0.9882000088691711]
</pre>
- 정확도가 98.82%로 꽤 높게 나왔다.


#### 결과에 속임수가 있다?

- 전이 학습은 작은 DNN에서는 잘 동작하지 않는다. 타깃 클래스나 랜덤 초깃값을 바꾸면 성능이 떨어지거나 더 나빠질 것이다.

  - 이유: 패턴 수를 적게 학습하고 DNN에서는 특정 패턴을 학습하기 때문이다. 그렇기 때문에 다른 작업에 적용할 때는 유용하지 않다. 

- 일반적인 특징을 감지하는 경향이 있는 심층 CNN에서 잘 동작한다.


### 11.2.2 비지도 사전 훈련  

레이블된 훈련 데이터가 많지 않고, 재사용할 수 있는 비슷한 모델이 없을 때 어떻게 해야할까?  

1. 레이블되지 않은 훈련 데이터로 오토인코더나 GAN과 같은 비지도 학습 모델을 훈련할 수 있다.

2. 그 다음 오토인코더나 GAN 판별자의 하위층을 재사용하고 그 위에 작업에 맞는 출력층을 추가할 수 있다.

3. 그 다음 레이블된 훈련데이터로 지도 학습을 하여 최종 네트워크를 세밀하게 튜닝한다.  

![image](https://user-images.githubusercontent.com/89712324/219943903-5b4072e9-01ea-4118-a491-2a1da0310685.png)


### 11.2.3 보조 작업에서 사전 훈련

레이블된 훈련 데이터가 많지 않을 때 사용할 수 있는 또 다른 방법  

- 레이블된 훈련 데이터를 쉽게 얻거나 생성할 수 있는 보조 작업에서 첫 번째 신경망을 훈련한다.

- 이 신경망의 하위층을 실제 작업을 위해 재사용한다.

- 첫 번째 신경망의 하위층은 두 번째 신경망에 재사용될 수 있는 특성 추출기를 학습하게 된다.  

ex) 얼굴 인식하는 시스템을 만들 때, 각 개인의 얼굴 사진을 수백개씩 수집할 수 없으므로 랜덤하게 많은 사람의 얼굴 사진을 수집해서 두 개의 다른 이미지가 같은 사람의 것인지 감지하는 첫 번째 신경망을 훈련할 수 있다.  

이 신경망은 얼굴의 특성을 잘 감지하도록 학습된 신경망이다.  

=> 이 신경망의 하위층을 재사용해서 적은 양의 훈련 데이터에 얼굴을 잘 구분하는 분류기를 훈련할 수 있을 것이다.
