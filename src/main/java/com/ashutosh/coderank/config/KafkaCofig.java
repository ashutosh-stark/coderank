package com.ashutosh.coderank.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.ashutosh.coderank.constant.UtilConstant;

@Configuration
public class KafkaCofig {

    @Bean
    public NewTopic codeSubmissionTopic() {
        return TopicBuilder
                .name(UtilConstant.TOPIC_STRING)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
