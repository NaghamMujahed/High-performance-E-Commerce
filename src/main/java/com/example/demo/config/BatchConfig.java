package com.example.demo.config;

import com.example.demo.batch.SalesItemProcessor;
import com.example.demo.batch.SalesItemReader;
import com.example.demo.batch.SalesItemWriter;
import com.example.demo.batch.SalesPartitioner;
import com.example.demo.model.Sale;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import com.example.demo.batch.ChunkSizeCalculator;
import com.example.demo.repository.SaleRepository;

@Configuration
public class BatchConfig {
    private final ChunkSizeCalculator chunkSizeCalculator;
    private final SaleRepository saleRepository;

    public BatchConfig(ChunkSizeCalculator chunkSizeCalculator,
                       SaleRepository saleRepository) {
        this.chunkSizeCalculator = chunkSizeCalculator;
        this.saleRepository = saleRepository;
    }

    @Bean
    public Job salesInventoryJob(JobRepository jobRepository, Step masterStep) {
        return new JobBuilder("salesInventoryJob", jobRepository)
                .start(masterStep)
                .build();
    }

    @Bean
    public Step masterStep(JobRepository jobRepository, SalesPartitioner partitioner, PartitionHandler partitionHandler) {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", partitioner)
                .partitionHandler(partitionHandler)
                .build();
    }

    @Bean
    public PartitionHandler partitionHandler(Step workerStep, TaskExecutor batchTaskExecutor) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(workerStep);
        handler.setTaskExecutor(batchTaskExecutor);
        handler.setGridSize(4);
        return handler;
    }

    @Bean
    public Step workerStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           SalesItemReader reader,
                           SalesItemProcessor processor,
                           SalesItemWriter writer) {

        long totalRecords = saleRepository.countByProcessed(false);
        int chunkSize = chunkSizeCalculator.calculate(totalRecords);

        return new StepBuilder("workerStep", jobRepository)
                .<Sale, Sale>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("batch-thread-");
        executor.initialize();
        return executor;
    }
}