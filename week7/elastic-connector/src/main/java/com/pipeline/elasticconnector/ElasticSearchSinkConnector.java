package com.pipeline.elasticconnector;

import com.pipeline.elasticconnector.config.*;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * - 커넥터 생성 시 최초 실행
 * - 설정값을 확인하고 태스크 클래스를 지정하는 역할
 */
public class ElasticSearchSinkConnector extends SinkConnector {
    private final Logger logger = LoggerFactory.getLogger(ElasticSearchSinkConnector.class);
    private Map<String, String> configProperties;

    @Override
    public void start(Map<String, String> props) {
        this.configProperties = props;
        try {
            new ElasticSearchSinkConnectorConfig(props);        // 사용자로부터 설정값을 가져와 Config 인스턴스 생성
        } catch(ConfigException e) {
            throw new ConnectException(e.getMessage(), e);
        }
    }

    @Override
    public Class<? extends Task> taskClass() {
        return ElasticSearchSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>();
        Map<String, String> taskProps = new HashMap<>();

        taskProps.putAll(configProperties);
        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(taskProps);                         // 모든 태스크에 동일한 설정값 설정
        }
        return taskConfigs;
    }

    @Override
    public void stop() {
        logger.info("Stop elasticsearch connector");
    }

    @Override
    public ConfigDef config() {
        return ElasticSearchSinkConnectorConfig.CONFIG;
    }

    @Override
    public String version() {
        return "1.0";
    }
}
