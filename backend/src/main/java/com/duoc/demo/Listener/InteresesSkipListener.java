package com.duoc.demo.Listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.duoc.demo.Model.LegacyIntereses;
import com.duoc.demo.Model.ModernIntereses;

@Component
public class InteresesSkipListener implements SkipListener<LegacyIntereses, ModernIntereses> {

    private static final Logger log = LoggerFactory.getLogger(InteresesSkipListener.class);

    private final JdbcTemplate jdbcTemplate;

    public InteresesSkipListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onSkipInRead(Throwable t) {

        log.error("REGISTRO OMITIDO DURANTE LECTURA | MOTIVO {}", t.getMessage());

        guardarError("READ", null, t);
    }

    @Override
    public void onSkipInWrite(ModernIntereses item, Throwable t) {

        log.error("REGISTRO OMITIDO DURANTE ESCRITURA | MOTIVO {}", item, t.getMessage());

        guardarError("WRITE", item.toString(), t);
    }

    @Override
    public void onSkipInProcess(LegacyIntereses item, Throwable t) {

        log.error(
        "REGISTRO OMITIDO {} | MOTIVO {}",
        item,
        t.getMessage());
    }

    private void guardarError(String tipo, String item, Throwable t) {

        String sql = "INSERT INTO ERRORES_BATCH (tipo, registro, detalle) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, tipo, item, t.getMessage());
    }
}
