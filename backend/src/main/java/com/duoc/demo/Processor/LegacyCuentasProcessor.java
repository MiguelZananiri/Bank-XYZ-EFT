package com.duoc.demo.Processor;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.duoc.demo.Exception.CampoObligatorioException;
import com.duoc.demo.Exception.FechaInvalidaException;
import com.duoc.demo.Exception.MontoInvalidoException;
import com.duoc.demo.Model.LegacyCuentas;
import com.duoc.demo.Model.ModernCuentas;

public class LegacyCuentasProcessor
        implements ItemProcessor<LegacyCuentas, ModernCuentas> {

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

    private static final String DEPOSITO = "deposito";
    private static final String RETIRO = "retiro";
    private static final String COMPRA = "compra";

    private static final String ESTADO_VALIDA = "VALIDA";
    private static final String ESTADO_ANOMALIA = "ANOMALIA";

    @Override
    public ModernCuentas process(LegacyCuentas item) {

        Integer cuentaId = Integer.valueOf(item.cuentaId().trim());

        LocalDate fecha = parsearFecha(item.fecha());

        String transaccion = normalizarTexto(item.transaccion());

        BigDecimal monto;

        if (item.monto() == null || item.monto().isBlank()) {
            throw new CampoObligatorioException(
                    "El campo monto es obligatorio");
        }

        try {
            monto = new BigDecimal(item.monto().trim());
        } catch (NumberFormatException e) {
            throw new MontoInvalidoException(
                    "El monto no es numérico: " + item.monto());
        }

        String estado = ESTADO_VALIDA;
        String detalleValidacion = null;

        String descripcion;

        if (item.descripcion() == null || item.descripcion().isBlank()) {

            descripcion = "SIN DESCRIPCION";
            estado = ESTADO_ANOMALIA;
            detalleValidacion = agregarDetalle(
                    detalleValidacion,
                    "Descripcion ausente");

        } else {
            descripcion = item.descripcion().trim();
        }

        switch (transaccion) {

            case DEPOSITO -> {
                if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                    estado = ESTADO_ANOMALIA;
                    detalleValidacion = agregarDetalle(
                            detalleValidacion,
                            "Deposito con monto menor o igual a cero");
                }
            }

            case RETIRO -> {
                if (monto.compareTo(BigDecimal.ZERO) >= 0) {
                    estado = ESTADO_ANOMALIA;
                    detalleValidacion = agregarDetalle(
                            detalleValidacion,
                            "Retiro con monto mayor o igual a cero");
                }
            }

            case COMPRA -> {
                if (monto.compareTo(BigDecimal.ZERO) >= 0) {
                    estado = ESTADO_ANOMALIA;
                    detalleValidacion = agregarDetalle(
                            detalleValidacion,
                            "Compra con monto mayor o igual a cero");
                }
            }

            default -> {
                estado = ESTADO_ANOMALIA;
                detalleValidacion = agregarDetalle(
                        detalleValidacion,
                        "Tipo de transaccion no soportado");
            }
        }

        return new ModernCuentas(
                cuentaId,
                fecha,
                transaccion,
                monto,
                descripcion,
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
                // Se intenta con el siguiente formato legacy.
            }
        }

        throw new FechaInvalidaException(
                "Formato de fecha no reconocido: " + fecha);
    }

    private String normalizarTexto(String valor) {

        if (valor == null) {
            return "";
        }

        String normalizado = Normalizer.normalize(
                valor.trim().toLowerCase(),
                Normalizer.Form.NFD);

        return normalizado.replaceAll("\\p{M}", "");
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