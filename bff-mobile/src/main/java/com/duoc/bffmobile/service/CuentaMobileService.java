package com.duoc.bffmobile.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import com.duoc.bffmobile.dto.CuentaBackendResponse;
import com.duoc.bffmobile.dto.CuentaMobileResponse;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

@Service
public class CuentaMobileService {

        private final RestClient restClient;
        private final CircuitBreaker circuitBreaker;

        private final Retry retry;
        private final Bulkhead bulkhead;

        public CuentaMobileService(
                        @Value("${bankxyz.backend.url}") String backendUrl,
                        CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                        RetryRegistry retryRegistry,
                        BulkheadRegistry bulkheadRegistry) {

                this.restClient = RestClient.builder()
                                .baseUrl(backendUrl)
                                .build();

                this.circuitBreaker = circuitBreakerFactory.create("circuitBreaker");

                this.retry = retryRegistry.retry("backendRetry");

                this.bulkhead = bulkheadRegistry.bulkhead("backendBulkhead");
        }

        public List<CuentaMobileResponse> obtenerCuentas() {

                Supplier<List<CuentaMobileResponse>> supplier = () -> {

                        CuentaBackendResponse[] cuentas = restClient.get()
                                        .uri("/api/cuentas")
                                        .retrieve()
                                        .body(CuentaBackendResponse[].class);

                        if (cuentas == null) {
                                return List.of();
                        }

                        return Arrays.stream(cuentas)
                                        .map(this::convertirAMobile)
                                        .toList();
                };

                Supplier<List<CuentaMobileResponse>> conBulkhead = Bulkhead.decorateSupplier(bulkhead, supplier);

                Supplier<List<CuentaMobileResponse>> conRetry = Retry.decorateSupplier(retry, conBulkhead);

                return circuitBreaker.run(
                                conRetry,
                                throwable -> obtenerCuentasFallback(throwable));
        }

        private List<CuentaMobileResponse> obtenerCuentasFallback(Throwable e) {

                System.out.println(
                                "CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

                return List.of();
        }

        public CuentaMobileResponse obtenerCuenta(Integer id) {

                Supplier<CuentaMobileResponse> supplier = () -> {

                        CuentaBackendResponse cuenta = restClient.get()
                                        .uri("/api/cuentas/{id}", id)
                                        .retrieve()
                                        .body(CuentaBackendResponse.class);

                        if (cuenta == null) {
                                throw new IllegalStateException(
                                                "El backend devolvió una respuesta vacía");
                        }

                        return convertirAMobile(cuenta);
                };

                Supplier<CuentaMobileResponse> conBulkhead = Bulkhead.decorateSupplier(bulkhead, supplier);

                Supplier<CuentaMobileResponse> conRetry = Retry.decorateSupplier(retry, conBulkhead);

                return circuitBreaker.run(
                                conRetry,
                                throwable -> obtenerCuentaFallback(id, throwable));
        }

        private CuentaMobileResponse obtenerCuentaFallback(
                        Integer id,
                        Throwable e) {

                System.out.println("CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

                return new CuentaMobileResponse(
                                id,
                                "Servicio no disponible",
                                "N/A",
                                BigDecimal.ZERO,
                                "TEMPORALMENTE_NO_DISPONIBLE");
        }

        private CuentaMobileResponse convertirAMobile(
                        CuentaBackendResponse cuenta) {

                return new CuentaMobileResponse(
                                cuenta.cuentaId(),
                                cuenta.nombre(),
                                cuenta.tipo(),
                                cuenta.saldoFinal(),
                                cuenta.estado());
        }
}