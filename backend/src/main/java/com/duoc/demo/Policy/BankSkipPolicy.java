package com.duoc.demo.Policy;

import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.duoc.demo.Exception.CampoObligatorioException;
import com.duoc.demo.Exception.FechaInvalidaException;
import com.duoc.demo.Exception.MontoInvalidoException;

@Component
public class BankSkipPolicy implements SkipPolicy {

    private static final Logger log =
            LoggerFactory.getLogger(BankSkipPolicy.class);

    private final long maxSkipCount;

    public BankSkipPolicy(
            @Value("${app.maxSkipCount}") long maxSkipCount) {

        this.maxSkipCount = maxSkipCount;
    }

    @Override
    public boolean shouldSkip(
            Throwable throwable,
            long skipCount)
            throws SkipLimitExceededException {

        boolean excepcionOmitible =
                throwable instanceof FlatFileParseException
                || throwable instanceof NumberFormatException
                || throwable instanceof DateTimeParseException
                || throwable instanceof MontoInvalidoException
                || throwable instanceof FechaInvalidaException
                || throwable instanceof CampoObligatorioException;

        /*
         * Una excepción no contemplada por la política
         * nunca debe ser ignorada.
         */
        if (!excepcionOmitible) {
            return false;
        }

        /*
         * Detiene la partición si supera el máximo
         * de errores de datos permitido.
         */
        if (skipCount >= maxSkipCount) {

            log.error(
                    "Limite de omisiones alcanzado: {}. Excepcion: {}",
                    maxSkipCount,
                    throwable.getClass().getSimpleName());

            return false;
        }

        return true;
    }
}