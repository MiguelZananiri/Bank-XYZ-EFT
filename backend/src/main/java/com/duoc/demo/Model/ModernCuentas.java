package com.duoc.demo.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ModernCuentas(
        Integer cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        String descripcion,
        String estado,
        String detalleValidacion
) {
}