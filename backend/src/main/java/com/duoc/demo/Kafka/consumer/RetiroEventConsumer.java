package com.duoc.demo.Kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.duoc.demo.Kafka.TopicNames;

@Service
public class RetiroEventConsumer {

        private static final Logger log =
                LoggerFactory.getLogger(RetiroEventConsumer.class);

        @KafkaListener(
                topics = TopicNames.RETIRO_REALIZADO,
                groupId = "auditoria-bankxyz",
                concurrency = "3"
        )
        public void procesarRetiro(
                ConsumerRecord<String, String> consumerRecord) {

                log.info(
                        "AUDITORIA KAFKA | key={} | particion={} | offset={} | evento={}",
                        consumerRecord.key(),
                        consumerRecord.partition(),
                        consumerRecord.offset(),
                        consumerRecord.value()
                );
        }
}