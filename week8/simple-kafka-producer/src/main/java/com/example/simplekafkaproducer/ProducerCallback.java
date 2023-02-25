package com.example.simplekafkaproducer;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 프로듀서로 보낸 데이터 결과를 비동기적으로 확인하는 콜백
 * 사용 시 producer.send(record, new ProducerCallback()); 코드 추가
 */
public class ProducerCallback implements Callback {
    private final Logger logger = LoggerFactory.getLogger(ProducerCallback.class);

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            logger.error(exception.getMessage(), exception);
        } else {
            logger.error(metadata.toString());
        }
    }
}
