package com.duoc.demo.Processor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.demo.Exception.CampoObligatorioException;
import com.duoc.demo.Model.LegacyIntereses;
import com.duoc.demo.Model.ModernIntereses;

public class LegacyInteresesProcessor
        implements ItemProcessor<LegacyIntereses, ModernIntereses> {

    private static final String TIPO_AHORRO = "ahorro";
    private static final String TIPO_PRESTAMO = "prestamo";

    private static final String ESTADO_VALIDA = "VALIDA";
    private static final String ESTADO_ANOMALIA = "ANOMALIA";

    private static final BigDecimal TASA_AHORRO =
            new BigDecimal("0.01");

    private static final BigDecimal TASA_PRESTAMO =
            new BigDecimal("0.02");

    @Override
    public ModernIntereses process(LegacyIntereses item) throws Exception {

        Integer cuentaId = Integer.valueOf(item.cuentaId().trim());
        String nombre = item.nombre().trim();
        BigDecimal saldoInicial;

        try {
            saldoInicial = new BigDecimal(item.saldo().trim());
        } catch (Exception e) {
            throw new CampoObligatorioException("El campo saldo es obligatorio");
        }

        Integer edad;

        try {
            edad = Integer.valueOf(item.edad().trim());
        } catch (Exception e) {
            throw new CampoObligatorioException("El campo edad es obligatorio");
        }

        String tipo = item.tipo().trim().toLowerCase();

        String estado = ESTADO_VALIDA;
        String detalleValidacion = null;

        BigDecimal tasaInteres = null;
        BigDecimal interesCalculado = null;
        BigDecimal saldoFinal = saldoInicial;
        

        // Validación del saldo
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            estado = ESTADO_ANOMALIA;
            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Saldo negativo"
            );

        } else if (saldoInicial.compareTo(BigDecimal.ZERO) == 0) {
            estado = ESTADO_ANOMALIA;
            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Saldo igual a cero"
            );
        }

        // Determinar tasa según tipo de cuenta
        if (TIPO_AHORRO.equals(tipo)) {
            tasaInteres = TASA_AHORRO;

        } else if (TIPO_PRESTAMO.equals(tipo)) {
            tasaInteres = TASA_PRESTAMO;

        } else {
            estado = ESTADO_ANOMALIA;
            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Tipo de cuenta no soportado"
            );
        }

        // Calcular interés únicamente si existe una tasa válida
        if (tasaInteres != null) {
            interesCalculado = saldoInicial
                    .multiply(tasaInteres)
                    .setScale(2, RoundingMode.HALF_UP);

            saldoFinal = saldoInicial
                    .add(interesCalculado)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new ModernIntereses(
                cuentaId,
                nombre,
                saldoInicial,
                edad,
                tipo,
                tasaInteres,
                interesCalculado,
                saldoFinal,
                estado,
                detalleValidacion,
                item.lineaOrigen()
        );
    }

    private String agregarDetalle(
            String detalleActual,
            String nuevoDetalle) {

        if (detalleActual == null || detalleActual.isBlank()) {
            return nuevoDetalle;
        }

        return detalleActual + "; " + nuevoDetalle;
    }
}