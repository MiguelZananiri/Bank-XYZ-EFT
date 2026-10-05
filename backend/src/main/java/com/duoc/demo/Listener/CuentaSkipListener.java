package com.duoc.demo.Listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.duoc.demo.Model.LegacyCuentas;
import com.duoc.demo.Model.ModernCuentas;

@Component
public class CuentaSkipListener implements SkipListener<LegacyCuentas, ModernCuentas> {

    private static final Logger log = LoggerFactory.getLogger(CuentaSkipListener.class);

    private final JdbcTemplate jdbcTemplate;

    public CuentaSkipListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onSkipInRead(Throwable t) {

        log.error("REGISTRO OMITIDO DURANTE LECTURA | MOTIVO {}", t.getMessage());

        guardarError("READ", null, t);
    }

    @Override
    public void onSkipInWrite(ModernCuentas item, Throwable t) {

        log.error("REGISTRO OMITIDO DURANTE ESCRITURA | MOTIVO {}", item, t.getMessage());

        guardarError("WRITE", item.toString(), t);
    }

    @Override
    public void onSkipInProcess(LegacyCuentas item, Throwable t) {

        log.error("REGISTRO OMITIDO {} | MOTIVO {}", item, t);

        guardarError("PROCESS", item.toString(), t);
    }

    private void guardarError(String tipo, String item, Throwable t) {

        String sql = "INSERT INTO ERRORES_BATCH (tipo, registro, detalle) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, tipo, item, t.getMessage());
    }
}