package com.ashutosh.coderank.service;

import java.util.UUID;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.ashutosh.coderank.constant.UtilConstant;

@Service
public class KafkaProducer {

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    Environment environment;



    public void producer(UUID submissionId){
        kafkaTemplate.send(UtilConstant.TOPIC_STRING,submissionId.toString());
        System.out.println("Message sent to Kafka topic: " + submissionId.toString());
    }


    
}
