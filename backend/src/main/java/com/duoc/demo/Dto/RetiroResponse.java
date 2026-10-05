package com.duoc.demo.Dto;

import java.math.BigDecimal;

public record RetiroResponse(
        Integer cuentaId,
        BigDecimal montoRetirado,
        BigDecimal saldoDisponible,
        String mensaje
) {
}