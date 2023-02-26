package com.pipeline.elasticconnector;

import com.google.gson.Gson;
import com.pipeline.elasticconnector.config.*;
import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * 실질적인 엘라스틱서치 적재 로직
 */
public class ElasticSearchSinkTask extends SinkTask {
    private Logger logger = LoggerFactory.getLogger(ElasticSearchSinkTask.class);

    private ElasticSearchSinkConnectorConfig config;
    private RestHighLevelClient esClient;

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        try {
            config = new ElasticSearchSinkConnectorConfig(props);
        } catch (ConfigException e) {
            throw new ConnectException(e.getMessage(), e);
        }

        // esClient: 엘라스틱서치에 적재하기 위해 사용할 인스턴스
        esClient = new RestHighLevelClient(
                        RestClient.builder(new HttpHost(config.getString(config.ES_CLUSTER_HOST),
                        config.getInt(config.ES_CLUSTER_PORT))));
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        if (records.size() > 0) {
            BulkRequest bulkRequest = new BulkRequest();                // BulkRequest: 데이터를 묶어 엘라스틱서치로 전송할 때 사용
            for (SinkRecord record : records) {
                Gson gson = new Gson();
                Map map = gson.fromJson(record.value().toString(), Map.class);
                bulkRequest.add(new IndexRequest(config.getString(config.ES_INDEX)).source(map, XContentType.JSON));
                logger.info("record : {}", record.value());
            }

            // bulk 전송
            esClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
                @Override
                public void onResponse(BulkResponse bulkResponse) {
                    if (bulkResponse.hasFailures()) {
                        logger.error(bulkResponse.buildFailureMessage());
                    } else {
                        logger.info("bulk save success");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    logger.error(e.getMessage(), e);
                }
            });
        }
    }

    /**
     * 일정 주기마다 호출되는 메서드
     */
    @Override
    public void flush(Map<TopicPartition, OffsetAndMetadata> currentOffsets) {
        logger.info("flush");
    }

    @Override
    public void stop() {
        try {
            esClient.close();
        } catch (IOException e) {
            logger.info(e.getMessage(), e);
        }
    }
}
