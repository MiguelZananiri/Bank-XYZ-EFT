package com.duoc.bffweb.service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import com.duoc.bffweb.dto.CuentaBackendResponse;
import com.duoc.bffweb.dto.CuentaWebResponse;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

@Service
public class CuentaWebService {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    private final Retry retry;
    private final Bulkhead bulkhead;

    public CuentaWebService(
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

    public CuentaWebResponse obtenerCuenta(Integer id) {

        Supplier<CuentaWebResponse> supplier = () -> {

            CuentaBackendResponse cuenta = restClient.get()
                    .uri("/api/cuentas/{id}", id)
                    .retrieve()
                    .body(CuentaBackendResponse.class);

            return convertirAWeb(cuenta);
        };

        Supplier<CuentaWebResponse> conBulkhead = Bulkhead.decorateSupplier(bulkhead, supplier);

        Supplier<CuentaWebResponse> conRetry = Retry.decorateSupplier(retry, conBulkhead);

        return circuitBreaker.run(
                conRetry,
                throwable -> obtenerCuentaFallback(id, throwable));
    }

    public CuentaWebResponse obtenerCuentaFallback(
            Integer id,
            Throwable e) {

        System.out.println("CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

        return new CuentaWebResponse(
                id,
                "Servicio no disponible",
                0,
                "N/A",
                null,
                null,
                null,
                null,
                "TEMPORALMENTE_NO_DISPONIBLE");
    }

    public List<CuentaWebResponse> obtenerCuentas() {

        Supplier<List<CuentaWebResponse>> supplier = () -> {

            CuentaBackendResponse[] cuentas = restClient.get()
                    .uri("/api/cuentas")
                    .retrieve()
                    .body(CuentaBackendResponse[].class);

            if (cuentas == null) {
                return List.of();
            }

            return Arrays.stream(cuentas)
                    .map(this::convertirAWeb)
                    .toList();
        };

        Supplier<List<CuentaWebResponse>> conBulkhead = Bulkhead.decorateSupplier(bulkhead, supplier);

        Supplier<List<CuentaWebResponse>> conRetry = Retry.decorateSupplier(retry, conBulkhead);

        return circuitBreaker.run(
                conRetry,
                throwable -> obtenerCuentasFallback(throwable));
    }

    public List<CuentaWebResponse> obtenerCuentasFallback(Throwable e) {

        System.out.println(
                "CIRCUIT BREAKER ACTIVADO: " + e.getMessage());

        return List.of();
    }

    private CuentaWebResponse convertirAWeb(CuentaBackendResponse cuenta) {

        return new CuentaWebResponse(
                cuenta.cuentaId(),
                cuenta.nombre(),
                cuenta.edad(),
                cuenta.tipo(),
                cuenta.saldoInicial(),
                cuenta.tasaInteres(),
                cuenta.interesCalculado(),
                cuenta.saldoFinal(),
                cuenta.estado());
    }
}