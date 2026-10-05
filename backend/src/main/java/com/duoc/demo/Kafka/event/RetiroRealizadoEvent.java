package com.duoc.demo.Kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RetiroRealizadoEvent(
        Integer cuentaId,
        BigDecimal monto,
        BigDecimal saldoDisponible,
        LocalDateTime fechaHora
) {
}