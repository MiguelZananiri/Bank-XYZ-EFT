package com.duoc.demo.Kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.duoc.demo.Kafka.TopicNames;
import com.duoc.demo.Kafka.event.RetiroRealizadoEvent;

@Service
public class RetiroEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(RetiroEventProducer.class);

    private final KafkaTemplate<String, RetiroRealizadoEvent> kafkaTemplate;

    public RetiroEventProducer(
            KafkaTemplate<String, RetiroRealizadoEvent> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(RetiroRealizadoEvent evento) {

        String key = evento.cuentaId().toString();

        kafkaTemplate
                .send(TopicNames.RETIRO_REALIZADO, key, evento)
                .whenComplete((resultado, error) -> {

                    if (error != null) {
                        log.error(
                                "Error al publicar evento de retiro para cuenta {}",
                                evento.cuentaId(),
                                error
                        );
                        return;
                    }

                    log.info(
                            "Evento retiro-realizado publicado. cuentaId={}, particion={}, offset={}",
                            evento.cuentaId(),
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset()
                    );
                });
    }
}