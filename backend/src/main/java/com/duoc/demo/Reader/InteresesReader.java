package com.duoc.demo.Reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.core.io.ClassPathResource;

import com.duoc.demo.Model.LegacyIntereses;

public class InteresesReader implements ItemReader<LegacyIntereses> {

    private final List<LegacyIntereses> interesesList;
    private int index;
    private final int end;

    public InteresesReader(String resourcePath, int start, int end) throws IOException {
        List<LegacyIntereses> interesesList = cargarIntereses(resourcePath);
        
        this.interesesList = interesesList.subList(start, Math.min(end, interesesList.size()));

        this.index = 0;
        this.end = this.interesesList.size();
    }

    @Override
    public LegacyIntereses read() {

        if (index >= end) {
            return null;
        }
        
        return interesesList.get(index++);
    }

    private List<LegacyIntereses> cargarIntereses(String resourcePath)
            throws IOException {

        List<LegacyIntereses> interesesList = new ArrayList<>();
        ClassPathResource resource =
                new ClassPathResource(resourcePath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        resource.getInputStream(),
                        StandardCharsets.UTF_8))) {

            String line;
            int numeroLinea = 0;

            while ((line = reader.readLine()) != null) {

                numeroLinea++;

                // Primera línea = encabezado del CSV
                if (numeroLinea == 1) {
                    continue;
                }

                String[] fields = line.split(",", -1);

                LegacyIntereses interes =
                        new LegacyIntereses(
                                fields[0],
                                fields[1],
                                fields[2],
                                fields[3],
                                fields[4],
                                numeroLinea);

                interesesList.add(interes);
            }

            return interesesList;
        }
    }
}
