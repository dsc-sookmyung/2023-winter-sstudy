package com.pipeline.consumer;

import com.pipeline.consumer.consumer.ConsumerWorker;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.codehaus.jackson.map.deser.std.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class HdfsSinkApplication {
	private final static Logger logger = LoggerFactory.getLogger(HdfsSinkApplication.class);

	private final static String BOOTSTRAP_SERVERS = "my-kafka:9092";
	private final static String TOPIC_NAME = "select-color";
	private final static String GROUP_ID = "color-hdfs-save-consumer-group";
	private final static int CONSUMER_COUNT = 3;
	private final static List<ConsumerWorker> workers = new ArrayList<>();

	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new ShutdownThread());

		Properties configs = new Properties();
		configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
		configs.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
		configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

		ExecutorService executorService = Executors.newCachedThreadPool();		// 컨슈머 스레드를 스레드 풀로 관리
		for (int i = 0; i < CONSUMER_COUNT; i++) {
			workers.add(new ConsumerWorker(configs, TOPIC_NAME, i));			// CONSUMER_COUNT 개만큼 컨슈머 생성
		}

		workers.forEach(executorService::execute);

		SpringApplication.run(HdfsSinkApplication.class, args);
	}

	static class ShutdownThread extends Thread {
		public void run() {
			logger.info("Shutdown hook");
			workers.forEach(ConsumerWorker::stopAndWakeup);
		}
	}

}
