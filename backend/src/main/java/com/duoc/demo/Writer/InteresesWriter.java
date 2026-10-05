package com.duoc.demo.Writer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.duoc.demo.Model.ModernIntereses;

public class InteresesWriter implements ItemWriter<ModernIntereses> {

    private final JdbcTemplate jdbcTemplate;

    public InteresesWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends ModernIntereses> items) throws Exception {

        for (ModernIntereses item : items) {

            jdbcTemplate.update(
                """
                MERGE INTO INTERESES_PROCESADOS destino
                USING (
                    SELECT
                        ? AS CUENTA_ID,
                        ? AS NOMBRE,
                        ? AS SALDO_INICIAL,
                        ? AS EDAD,
                        ? AS TIPO,
                        ? AS TASA_INTERES,
                        ? AS INTERES_CALCULADO,
                        ? AS SALDO_FINAL,
                        ? AS ESTADO,
                        ? AS DETALLE_VALIDACION,
                        ? AS LINEA_ORIGEN
                    FROM DUAL
                ) origen

                ON (destino.CUENTA_ID = origen.CUENTA_ID)

                WHEN MATCHED THEN
                    UPDATE SET
                        destino.NOMBRE = origen.NOMBRE,
                        destino.SALDO_INICIAL = origen.SALDO_INICIAL,
                        destino.EDAD = origen.EDAD,
                        destino.TIPO = origen.TIPO,
                        destino.TASA_INTERES = origen.TASA_INTERES,
                        destino.INTERES_CALCULADO = origen.INTERES_CALCULADO,
                        destino.SALDO_FINAL = origen.SALDO_FINAL,
                        destino.ESTADO = origen.ESTADO,
                        destino.DETALLE_VALIDACION =
                            origen.DETALLE_VALIDACION,
                        destino.LINEA_ORIGEN =
                            origen.LINEA_ORIGEN,
                        destino.FECHA_PROCESAMIENTO =
                            SYSTIMESTAMP
                    WHERE
                        destino.LINEA_ORIGEN IS NULL

                        OR (
                            LOWER(origen.TIPO) IN ('ahorro', 'prestamo')
                            AND LOWER(destino.TIPO) NOT IN ('ahorro', 'prestamo')
                        )

                        OR (
                            (
                                (
                                    LOWER(origen.TIPO) IN ('ahorro', 'prestamo')
                                    AND LOWER(destino.TIPO) IN ('ahorro', 'prestamo')
                                )
                                OR
                                (
                                    LOWER(origen.TIPO) NOT IN ('ahorro', 'prestamo')
                                    AND LOWER(destino.TIPO) NOT IN ('ahorro', 'prestamo')
                                )
                            )
                            AND origen.LINEA_ORIGEN < destino.LINEA_ORIGEN
                        )

                WHEN NOT MATCHED THEN
                    INSERT (
                        CUENTA_ID,
                        NOMBRE,
                        SALDO_INICIAL,
                        EDAD,
                        TIPO,
                        TASA_INTERES,
                        INTERES_CALCULADO,
                        SALDO_FINAL,
                        ESTADO,
                        DETALLE_VALIDACION,
                        LINEA_ORIGEN
                    )
                    VALUES (
                        origen.CUENTA_ID,
                        origen.NOMBRE,
                        origen.SALDO_INICIAL,
                        origen.EDAD,
                        origen.TIPO,
                        origen.TASA_INTERES,
                        origen.INTERES_CALCULADO,
                        origen.SALDO_FINAL,
                        origen.ESTADO,
                        origen.DETALLE_VALIDACION,
                        origen.LINEA_ORIGEN
                    )
                """,
                item.cuentaId(),
                item.nombre(),
                item.saldoInicial(),
                item.edad(),
                item.tipo(),
                item.tasaInteres(),
                item.interesCalculado(),
                item.saldoFinal(),
                item.estado(),
                item.detalleValidacion(),
                item.lineaOrigen()
            );
        }
    }
}