package com.duoc.demo.Processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.demo.Exception.FechaInvalidaException;
import com.duoc.demo.Exception.MontoInvalidoException;
import com.duoc.demo.Model.LegacyTransacciones;
import com.duoc.demo.Model.ModernTransacciones;

public class LegacyTransaccionesProcessor
        implements ItemProcessor<LegacyTransacciones, ModernTransacciones> {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd-MM-uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/MM/dd")
                    .withResolverStyle(ResolverStyle.STRICT)
    );

    private static final String TIPO_DEBITO = "debito";
    private static final String TIPO_CREDITO = "credito";
    private static final String TIPO_DESCONOCIDO = "desconocido";

    private static final String ESTADO_VALIDA = "VALIDA";
    private static final String ESTADO_ANOMALIA = "ANOMALIA";

    /*
     * ConcurrentHashMap.newKeySet() se utiliza porque el Processor
     * es ejecutado por varias particiones en paralelo.
     */
    private final Set<String> transaccionesProcesadas =
            ConcurrentHashMap.newKeySet();

    @Override
    public ModernTransacciones process(LegacyTransacciones item) {

        Integer id = Integer.valueOf(item.id().trim());

        LocalDate fecha = parsearFecha(item.fecha());

        if (item.monto() == null || item.monto().isBlank()) {
            throw new MontoInvalidoException(
                    "El campo monto es obligatorio");
        }

        BigDecimal monto;

        try {
            monto = new BigDecimal(item.monto().trim());
        } catch (NumberFormatException e) {
            throw new MontoInvalidoException(
                    "Formato de monto incorrecto: " + item.monto());
        }

        String tipoOriginal = normalizarTexto(item.tipo());
        String tipo;

        String estado = ESTADO_VALIDA;
        String detalleValidacion = null;

        /*
         * Normalización del tipo de transacción.
         * Oracle permite debito, credito y desconocido.
         */
        if (TIPO_DEBITO.equals(tipoOriginal)
                || TIPO_CREDITO.equals(tipoOriginal)) {

            tipo = tipoOriginal;

        } else {

            tipo = TIPO_DESCONOCIDO;
            estado = ESTADO_ANOMALIA;

            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Tipo de transaccion no reconocido: "
                            + tipoOriginal);
        }

        /*
         * Validación del monto.
         */
        if (monto.compareTo(BigDecimal.ZERO) < 0) {

            estado = ESTADO_ANOMALIA;

            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Monto negativo");

        } else if (monto.compareTo(BigDecimal.ZERO) == 0) {

            estado = ESTADO_ANOMALIA;

            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Monto igual a cero");
        }

        /*
         * Detección de posibles duplicados.
         */
        String claveTransaccion =
                fecha
                + "|"
                + monto.stripTrailingZeros().toPlainString()
                + "|"
                + tipo;

        if (!transaccionesProcesadas.add(claveTransaccion)) {

            estado = ESTADO_ANOMALIA;

            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Posible transaccion duplicada");
        }

        return new ModernTransacciones(
                id,
                fecha,
                monto,
                tipo,
                estado,
                detalleValidacion
        );
    }

    private LocalDate parsearFecha(String valor) {

        if (valor == null || valor.isBlank()) {
            throw new FechaInvalidaException(
                    "El campo fecha es obligatorio");
        }

        String fecha = valor.trim();

        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                return LocalDate.parse(fecha, formato);
            } catch (DateTimeParseException ignored) {
                // Se prueba el siguiente formato legacy.
            }
        }

        throw new FechaInvalidaException(
                "Formato de fecha no reconocido: " + fecha);
    }

    private String normalizarTexto(String valor) {

        if (valor == null) {
            return "";
        }

        return valor.trim().toLowerCase();
    }

    private String agregarDetalle(
            String detalleActual,
            String nuevoDetalle) {

        if (detalleActual == null || detalleActual.isBlank()) {
            return nuevoDetalle;
        }

        return detalleActual + "; " + nuevoDetalle;
    }

    public void reset() {
        transaccionesProcesadas.clear();
    }
}