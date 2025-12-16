package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class TransactionProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessingService.class);
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final RestTemplate restTemplate;

    public TransactionProcessingService(UserRepository userRepository,
                                        TransactionRecordRepository transactionRecordRepository,
                                        RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void process(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        long senderId = transaction.getSenderId();
        long recipientId = transaction.getRecipientId();
        float amount = transaction.getAmount();

        UserRecord sender = userRepository.findById(senderId);
        UserRecord recipient = userRepository.findById(recipientId);

        if (sender == null || recipient == null) {
            logger.debug("Ignoring transaction; sender or recipient not found: {}", transaction);
            return;
        }

        if (sender.getBalance() < amount) {
            logger.debug("Ignoring transaction; insufficient funds for sender {}: {}", senderId, amount);
            return;
        }

        // Call incentives API for valid transactions
        Incentive incentive = null;
        float incentiveAmount = 0.0f;
        try {
            incentive = restTemplate.postForObject(INCENTIVE_API_URL, transaction, Incentive.class);
            if (incentive != null) {
                incentiveAmount = incentive.getAmount();
                logger.debug("Received incentive amount {} for transaction", incentiveAmount);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch incentive for transaction: {}", e.getMessage());
            // Continue processing without incentive on API failure
        }

        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(recipient.getBalance() + amount + incentiveAmount);

        userRepository.save(sender);
        userRepository.save(recipient);

        TransactionRecord record = new TransactionRecord(sender, recipient, amount, incentiveAmount);
        transactionRecordRepository.save(record);

        // Log Waldorf's balance (userId=5) for debugging Task 3
        if (senderId == 5 || recipientId == 5) {
            UserRecord waldorf = userRepository.findById(5);
            if (waldorf != null) {
                logger.info("WALDORF BALANCE AFTER TRANSACTION: {}", waldorf.getBalance());
            }
        }

        // Log Wilbur's balance (userId=9) for debugging Task 4
        if (senderId == 9 || recipientId == 9) {
            UserRecord wilbur = userRepository.findById(9);
            if (wilbur != null) {
                logger.info("WILBUR BALANCE AFTER TRANSACTION: {}", wilbur.getBalance());
            }
        }
    }
}
