package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.service.TransactionProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final TransactionProcessingService transactionProcessingService;
    private final List<Transaction> receivedTransactions = new CopyOnWriteArrayList<>();

    public TransactionListener(TransactionProcessingService transactionProcessingService) {
        this.transactionProcessingService = transactionProcessingService;
    }

    @KafkaListener(topics = "${general.kafka-topic}")
    public void listen(Transaction transaction) {
        // Keep a record of the incoming transaction for debugging and later processing.
        receivedTransactions.add(transaction);
        logger.info("Received transaction: {}", transaction);

        transactionProcessingService.process(transaction);
    }

    public List<Transaction> getReceivedTransactions() {
        return List.copyOf(receivedTransactions);
    }
}
