package com.duoc.demo.Partition;

import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BankPartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(BankPartitioner.class);

    private int totalRecords;

    public BankPartitioner(@Value("${app.totalRecords}") int totalRecords) {
        this.totalRecords = totalRecords;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        Map<String, ExecutionContext> partitions = new LinkedHashMap<>();

        int partitionSize = (int) Math.ceil((double) totalRecords / gridSize);

        
        log.info("Grid size: {}", gridSize);
        log.info("total records: {}", totalRecords);

        for (int i = 0; i < gridSize; i++) {

            int start = i * partitionSize;
            int end = Math.min(start + partitionSize, totalRecords);

            if (start >= totalRecords) {
                break;
            }

            ExecutionContext context = new ExecutionContext();

            context.putInt("start", start);
            context.putInt("end", end);

            partitions.put("partition" + i, context);
        }

        return partitions;
    }

}
