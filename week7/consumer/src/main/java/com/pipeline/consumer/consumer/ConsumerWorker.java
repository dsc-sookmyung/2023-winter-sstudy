package com.pipeline.consumer.consumer;

import org.apache.commons.io.filefilter.ConditionalFileFilter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConsumerWorker implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);
    private static Map<Integer, List<String>> bufferString = new ConcurrentHashMap<>(); // 전달받은 데이터를 임시 저장할 버퍼
    private static Map<Integer, Long> currentFileOffset = new ConcurrentHashMap<>();

    private final static int FLUSH_RECORD_COUNT = 10;
    private Properties prop;
    private String topic;
    private String threadName;
    private KafkaConsumer<String, String> consumer;

    public ConsumerWorker(Properties prop, String topic, int number) {
        this.prop = prop;
        this.topic = topic;
        this.threadName = "consumer-thread-" + number;              // 스레드에 번호 부여
    }

    @Override
    public void run() {
        Thread.currentThread().setName(threadName);

        consumer = new KafkaConsumer<>(prop);
        consumer.subscribe(Arrays.asList(topic));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, String> record : records) {
                    addHdfsFileBuffer(record);                      // 가져온 데이터를 버퍼에 저장
                }
                saveBufferToHdfsFile(consumer.assignment());        // 가득 찬 버퍼를 HDFS에 저장
            }
        } catch (WakeupException e) {
            logger.warn("Wakeup consumer");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            consumer.close();
        }
    }

    private void saveBufferToHdfsFile(Set<TopicPartition> partitions) {
        partitions.forEach(p -> checkFlushCount(p.partition()));    // 버퍼 데이터 개수가 충족되었는지 확인
    }

    private void addHdfsFileBuffer(ConsumerRecord<String, String> record) {
         List<String> buffer = bufferString.getOrDefault(record.partition(), new ArrayList<>());
         buffer.add(record.value());
         bufferString.put(record.partition(), buffer);

         if (buffer.size() == 1)                                    // 가장 처음 오프셋인 경우
             currentFileOffset.put(record.partition(), record.offset());
    }

    private void checkFlushCount(int partitionNo) {
        if (bufferString.get(partitionNo) != null) {
            if (bufferString.get(partitionNo).size() > FLUSH_RECORD_COUNT - 1) {
                save(partitionNo);                                  // 실질적인 적재 수행
            }
        }
    }

    private void save(int partitionNo) {
        if (bufferString.get(partitionNo).size() > 0)
            try {
                String fileName = "/data/color-" + partitionNo + "-" +
                    currentFileOffset.get(partitionNo) + ".log";

                Configuration configuration = new Configuration();
                configuration.set("fs.defaultFS", "hdfs://localhost:9000");
                FileSystem hdfsFileSystem = FileSystem.get(configuration);

                FSDataOutputStream fileOutputStream = hdfsFileSystem.create(new Path(fileName));
                fileOutputStream.writeBytes(StringUtils.join("\n", bufferString.get(partitionNo)));
                fileOutputStream.close();

                bufferString.put(partitionNo, new ArrayList<>());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
    }

    private void saveRemainBufferToHdfsFile() {
        bufferString.forEach((partitionNo, v) -> this.save(partitionNo));
    }

    public void stopAndWakeup() {
        logger.info("stopAndWakeup");
        consumer.wakeup();
        saveRemainBufferToHdfsFile();
    }
}
