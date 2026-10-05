package com.duoc.demo.Writer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import com.duoc.demo.Model.ModernTransacciones;

public class TransaccionesWriter implements ItemWriter<ModernTransacciones> {

    private final JdbcTemplate jdbcTemplate;

    public TransaccionesWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends ModernTransacciones> items) throws Exception {
        for (ModernTransacciones item : items) {

            jdbcTemplate.update(
                """
                MERGE INTO TRANSACCIONES_PROCESADAS destino
                USING (
                    SELECT
                        ? AS ID,
                        ? AS FECHA,
                        ? AS MONTO,
                        ? AS TIPO,
                        ? AS ESTADO,
                        ? AS DETALLE_VALIDACION
                    FROM DUAL
                ) origen
                ON (destino.ID = origen.ID)

                WHEN MATCHED THEN
                    UPDATE SET
                        destino.FECHA = origen.FECHA,
                        destino.MONTO = origen.MONTO,
                        destino.TIPO = origen.TIPO,
                        destino.ESTADO = origen.ESTADO,
                        destino.DETALLE_VALIDACION = origen.DETALLE_VALIDACION,
                        destino.FECHA_PROCESAMIENTO = SYSTIMESTAMP

                WHEN NOT MATCHED THEN
                    INSERT (
                        ID,
                        FECHA,
                        MONTO,
                        TIPO,
                        ESTADO,
                        DETALLE_VALIDACION
                    )
                    VALUES (
                        origen.ID,
                        origen.FECHA,
                        origen.MONTO,
                        origen.TIPO,
                        origen.ESTADO,
                        origen.DETALLE_VALIDACION
                    )
                """,
                item.id(),
                item.fecha(),
                item.monto(),
                item.tipo(),
                item.estado(),
                item.detalleValidacion()
            );
        }
    }
    
}
