package com.example.demo.batch;

import com.example.demo.service.BatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupJobRunner implements ApplicationRunner {

    private final BatchService batchService;

    @Value("${BATCH_JOB_ENABLED:false}")
    private boolean batchEnabled;

    public StartupJobRunner(BatchService batchService) {
        this.batchService = batchService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (batchEnabled) {
            batchService.processSalesInChunks();
        }
    }
}