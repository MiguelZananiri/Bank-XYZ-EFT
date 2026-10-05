package com.duoc.bffatm.service;

import java.math.BigDecimal;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import com.duoc.bffatm.dto.CuentaBackendResponse;
import com.duoc.bffatm.dto.RetiroAtmRequest;
import com.duoc.bffatm.dto.RetiroAtmResponse;
import com.duoc.bffatm.dto.RetiroBackendResponse;
import com.duoc.bffatm.dto.SaldoAtmResponse;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

@Service
public class CuentaAtmService {

        private final RestClient restClient;
        private final CircuitBreaker circuitBreaker;

        private final Retry retry;
        private final Bulkhead bulkhead;

        public CuentaAtmService(
                        @Value("${bankxyz.backend.url}") String backendUrl,
                        CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                        RetryRegistry retryRegistry,
                        BulkheadRegistry bulkheadRegistry) {

                this.restClient = RestClient.builder()
                                .baseUrl(backendUrl)
                                .build();

                this.circuitBreaker = circuitBreakerFactory.create("circuitBreaker");
                this.retry = retryRegistry.retry("retry");
                this.bulkhead = bulkheadRegistry.bulkhead("bulkhead");
        }

        public SaldoAtmResponse obtenerSaldo(Integer id) {

                Supplier<SaldoAtmResponse> supplier = () -> {

                        CuentaBackendResponse cuenta = restClient.get()
                                        .uri("/api/cuentas/{id}", id)
                                        .retrieve()
                                        .body(CuentaBackendResponse.class);

                        return new SaldoAtmResponse(
                                        cuenta.cuentaId(),
                                        cuenta.saldoFinal(),
                                        cuenta.estado());
                };

                Supplier<SaldoAtmResponse> conBulkhead = Bulkhead.decorateSupplier(bulkhead, supplier);

                Supplier<SaldoAtmResponse> conRetry = Retry.decorateSupplier(retry, conBulkhead);

                return circuitBreaker.run(
                                conRetry,
                                throwable -> obtenerSaldoFallback(id, throwable));
        }

        public SaldoAtmResponse obtenerSaldoFallback(
                        Integer id,
                        Throwable e) {

                System.out.println("CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

                return new SaldoAtmResponse(
                                id,
                                BigDecimal.ZERO,
                                "TEMPORALMENTE_NO_DISPONIBLE");
        }

        public RetiroAtmResponse retirar(
                        Integer id,
                        RetiroAtmRequest request) {

                Supplier<RetiroAtmResponse> supplier = () -> {

                        RetiroBackendResponse respuesta = restClient.post()
                                        .uri("/api/cuentas/{id}/retiro", id)
                                        .body(request)
                                        .retrieve()
                                        .body(RetiroBackendResponse.class);

                        if (respuesta == null) {
                                throw new IllegalStateException(
                                                "El backend devolvió una respuesta vacía");
                        }

                        return new RetiroAtmResponse(
                                        respuesta.cuentaId(),
                                        respuesta.montoRetirado(),
                                        respuesta.saldoDisponible(),
                                        respuesta.mensaje());
                };

                Supplier<RetiroAtmResponse> conBulkhead = Bulkhead.decorateSupplier(bulkhead, supplier);

                return circuitBreaker.run(
                                conBulkhead,
                                throwable -> retirarFallback(id, request, throwable));
        }

        private RetiroAtmResponse retirarFallback(
                        Integer id,
                        RetiroAtmRequest request,
                        Throwable e) {

                System.out.println(
                                "CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

                return new RetiroAtmResponse(
                                id,
                                request.monto(),
                                BigDecimal.ZERO,
                                "Servicio temporalmente no disponible");
        }
}