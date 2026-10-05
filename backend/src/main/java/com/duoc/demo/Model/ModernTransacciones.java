package com.duoc.demo.Model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ModernTransacciones(
    Integer id,
    LocalDate fecha,
    BigDecimal monto,
    String tipo,
    String estado,
    String detalleValidacion
) {
}