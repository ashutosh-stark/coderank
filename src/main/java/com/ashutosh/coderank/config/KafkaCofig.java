package com.ashutosh.coderank.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.TopicBuilder;

import com.ashutosh.coderank.constant.UtilConstant;

@Configuration
public class KafkaCofig {

    Environment environment;

    public KafkaCofig(Environment environment) {
        this.environment = environment;
    }
    
    public NewTopic codeSubmissionTopic() {
        return TopicBuilder
        .name(environment.getProperty(UtilConstant.TOPIC_STRING))
        .partitions(3)
        .replicas(1)
        .build();
        
    }
}
