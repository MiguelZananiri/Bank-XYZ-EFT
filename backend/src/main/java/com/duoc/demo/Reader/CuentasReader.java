package com.duoc.demo.Reader;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.core.io.ClassPathResource;

import com.duoc.demo.Model.LegacyCuentas;

public class CuentasReader implements ItemReader<LegacyCuentas> {

    private final List<LegacyCuentas> cuentasList;
    private int index;
    private final int end;

    public CuentasReader(String resourcePath, int start, int end) throws IOException {
        List<LegacyCuentas> cuentasList = cargarCuentas(resourcePath);

        this.cuentasList = cuentasList.subList(start, Math.min(end, cuentasList.size()));

        this.index = 0;
        this.end = this.cuentasList.size();
    }

    @Override
    public LegacyCuentas read() {

        if (index >= end) {
            return null;
        }

        return cuentasList.get(index++);
    }

    private List<LegacyCuentas> cargarCuentas(String resourcePath) throws IOException {
        List<LegacyCuentas> cuentasList = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(resourcePath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }

                String[] fields = line.split(",", -1);
                LegacyCuentas cuenta = new LegacyCuentas(
                        fields[0],
                        fields[1],
                        fields[2],
                        fields[3],
                        fields[4]);
                cuentasList.add(cuenta);
            }

            return cuentasList;
        }
    }
}
