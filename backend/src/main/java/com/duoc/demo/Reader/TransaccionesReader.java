package com.duoc.demo.Reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.core.io.ClassPathResource;

import com.duoc.demo.Model.LegacyTransacciones;

public class TransaccionesReader implements ItemReader<LegacyTransacciones> {

    private final List<LegacyTransacciones> transaccionesList;
    private int index;
    private final int end;

    public TransaccionesReader(String resourcePath, int start, int end) throws IOException {
        List<LegacyTransacciones> transaccionesList = cargarTransacciones(resourcePath);

        this.transaccionesList = transaccionesList.subList(start, Math.min(end, transaccionesList.size()));

        this.index = 0;
        this.end = this.transaccionesList.size();
    }

    @Override
    public LegacyTransacciones read() {

        if (index >= end) {
            return null;
        }
        
        return transaccionesList.get(index++);
    }

    private List<LegacyTransacciones> cargarTransacciones(String resourcePath) throws IOException {
        List<LegacyTransacciones> transaccionesList = new ArrayList<>();
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

                String[] fields = line.split(",");
                LegacyTransacciones transaccion = new LegacyTransacciones(
                        fields[0],
                        fields[1],
                        fields[2],
                        fields[3]);
                transaccionesList.add(transaccion);
            }

            return transaccionesList;
        }
    }

}
