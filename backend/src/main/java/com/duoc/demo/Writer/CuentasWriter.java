package com.duoc.demo.Writer;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;

import com.duoc.demo.Model.ModernCuentas;

public class CuentasWriter implements ItemWriter<ModernCuentas> {
    
    private final JdbcTemplate jdbcTemplate;
    
    public CuentasWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends ModernCuentas> items) throws Exception {
        for (ModernCuentas item : items) {
            jdbcTemplate.update(
                "INSERT INTO MOVIMIENTOS_ANUALES_PROCESADOS (" +
                "CUENTA_ID, FECHA, TRANSACCION, MONTO, DESCRIPCION, " +
                "ESTADO, DETALLE_VALIDACION) VALUES (?, ?, ?, ?, ?, ?, ?)",
                item.cuentaId(),
                item.fecha(),
                item.transaccion(),
                item.monto(),
                item.descripcion(),
                item.estado(),
                item.detalleValidacion()
            );
        }
    }
}
