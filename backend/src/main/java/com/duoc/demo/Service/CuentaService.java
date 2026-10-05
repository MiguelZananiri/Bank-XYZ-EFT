package com.duoc.demo.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.duoc.demo.Dto.CuentaResponse;
import com.duoc.demo.Dto.RetiroResponse;
import com.duoc.demo.Kafka.event.RetiroRealizadoEvent;
import com.duoc.demo.Kafka.producer.RetiroEventProducer;
import com.duoc.demo.Repository.CuentaRepository;

@Service
public class CuentaService {

        private final CuentaRepository cuentaRepository;
        private final RetiroEventProducer retiroEventProducer;

        public CuentaService(
                CuentaRepository cuentaRepository,
                RetiroEventProducer retiroEventProducer) {

        this.cuentaRepository = cuentaRepository;
        this.retiroEventProducer = retiroEventProducer;
        }

        public List<CuentaResponse> obtenerTodas() {
                return cuentaRepository.findAll();
        }

        public Optional<CuentaResponse> obtenerPorId(Integer cuentaId) {
                return cuentaRepository.findById(cuentaId);
        }

        @Transactional
        public RetiroResponse retirar(
                Integer cuentaId,
                BigDecimal monto) {

                if (monto == null ||
                        monto.compareTo(BigDecimal.ZERO) <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El monto debe ser mayor que cero"
                );
                }

                CuentaResponse cuenta = cuentaRepository
                        .findById(cuentaId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Cuenta no encontrada"
                                )
                        );

                if (!"VALIDA".equalsIgnoreCase(cuenta.estado())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cuenta no se encuentra habilitada"
                );
                }

                if (!"ahorro".equalsIgnoreCase(cuenta.tipo())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La cuenta no admite retiros"
                );
                }

                if (cuenta.saldoFinal().compareTo(monto) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Saldo insuficiente"
                );
                }

                int filasActualizadas =
                        cuentaRepository.retirar(cuentaId, monto);

                if (filasActualizadas == 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No fue posible realizar el retiro"
                );
                }

                CuentaResponse cuentaActualizada =
                        cuentaRepository.findById(cuentaId)
                                .orElseThrow();

                RetiroRealizadoEvent evento =
                        new RetiroRealizadoEvent(
                                cuentaId,
                                monto,
                                cuentaActualizada.saldoFinal(),
                                LocalDateTime.now()
                        );

                retiroEventProducer.publicar(evento);

                return new RetiroResponse(
                        cuentaId,
                        monto,
                        cuentaActualizada.saldoFinal(),
                        "Retiro realizado correctamente"
                );
        }
}