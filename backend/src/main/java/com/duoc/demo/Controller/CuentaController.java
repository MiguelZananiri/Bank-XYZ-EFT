package com.duoc.demo.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.demo.Dto.CuentaResponse;
import com.duoc.demo.Dto.RetiroRequest;
import com.duoc.demo.Dto.RetiroResponse;
import com.duoc.demo.Service.CuentaService;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public List<CuentaResponse> obtenerTodas() {
        return cuentaService.obtenerTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaResponse> obtenerPorId(
            @PathVariable Integer id) {

        return cuentaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/retiro")
    public ResponseEntity<RetiroResponse> retirar(
            @PathVariable Integer id,
            @RequestBody RetiroRequest request) {

        return ResponseEntity.ok(
                cuentaService.retirar(id, request.monto())
        );
    }
}