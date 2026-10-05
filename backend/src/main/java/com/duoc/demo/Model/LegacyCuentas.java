package com.duoc.demo.Model;

public record LegacyCuentas(
        String cuentaId,
        String fecha,
        String transaccion,
        String monto,
        String descripcion
    ) 
{
}
