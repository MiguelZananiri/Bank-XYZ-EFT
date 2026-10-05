package com.duoc.demo.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.duoc.demo.Dto.CuentaResponse;

@Repository
public class CuentaRepository {

        private final JdbcTemplate jdbcTemplate;

        public CuentaRepository(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        public List<CuentaResponse> findAll() {

                String sql = """
                        SELECT
                        CUENTA_ID,
                        NOMBRE,
                        SALDO_INICIAL,
                        EDAD,
                        TIPO,
                        TASA_INTERES,
                        INTERES_CALCULADO,
                        SALDO_FINAL,
                        ESTADO
                        FROM INTERESES_PROCESADOS
                        ORDER BY CUENTA_ID
                        """;

                return jdbcTemplate.query(sql, (rs, rowNum) ->
                        new CuentaResponse(
                                rs.getInt("CUENTA_ID"),
                                rs.getString("NOMBRE"),
                                rs.getBigDecimal("SALDO_INICIAL"),
                                rs.getInt("EDAD"),
                                rs.getString("TIPO"),
                                rs.getBigDecimal("TASA_INTERES"),
                                rs.getBigDecimal("INTERES_CALCULADO"),
                                rs.getBigDecimal("SALDO_FINAL"),
                                rs.getString("ESTADO")
                        )
                );
        }

        public Optional<CuentaResponse> findById(Integer cuentaId) {

                String sql = """
                        SELECT
                        CUENTA_ID,
                        NOMBRE,
                        SALDO_INICIAL,
                        EDAD,
                        TIPO,
                        TASA_INTERES,
                        INTERES_CALCULADO,
                        SALDO_FINAL,
                        ESTADO
                        FROM INTERESES_PROCESADOS
                        WHERE CUENTA_ID = ?
                        """;

                List<CuentaResponse> resultados = jdbcTemplate.query(
                        sql,
                        (rs, rowNum) ->
                                new CuentaResponse(
                                        rs.getInt("CUENTA_ID"),
                                        rs.getString("NOMBRE"),
                                        rs.getBigDecimal("SALDO_INICIAL"),
                                        rs.getInt("EDAD"),
                                        rs.getString("TIPO"),
                                        rs.getBigDecimal("TASA_INTERES"),
                                        rs.getBigDecimal("INTERES_CALCULADO"),
                                        rs.getBigDecimal("SALDO_FINAL"),
                                        rs.getString("ESTADO")
                                ),
                        cuentaId
                );

                return resultados.stream().findFirst();
        }

        public int retirar(Integer cuentaId, BigDecimal monto) {

                String sql = """
                        UPDATE INTERESES_PROCESADOS
                        SET SALDO_FINAL = SALDO_FINAL - ?
                        WHERE CUENTA_ID = ?
                        AND ESTADO = 'VALIDA'
                        AND LOWER(TIPO) = 'ahorro'
                        AND SALDO_FINAL >= ?
                        """;

                return jdbcTemplate.update(
                        sql,
                        monto,
                        cuentaId,
                        monto
                );
        }
}