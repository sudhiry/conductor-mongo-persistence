package com.netflix.conductor.mongo.db.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.mongo.db.repository.QueueMessageRepository;
import com.netflix.conductor.mongo.db.repository.QueueRepository;
import com.netflix.conductor.mongo.db.models.QueueDocument;
import com.netflix.conductor.mongo.db.models.QueueMessage;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
public class MongoQueueDAO extends MongoDBBaseDAO implements QueueDAO {

    private static final Long UNACK_SCHEDULE_MS = 60_000L;

    private final MongoTemplate mongoTemplate;

    private final QueueMessageRepository queueMessageRepository;

    private final QueueRepository queueRepository;

    public MongoQueueDAO(
            ObjectMapper objectMapper,
            @Qualifier("MongoDBRetryTemplate") RetryTemplate retryTemplate,
            MongoTemplate mongoTemplate,
            QueueMessageRepository queueMessageRepository,
            QueueRepository queueRepository
    ) {
        super(objectMapper, retryTemplate);
        this.mongoTemplate = mongoTemplate;
        this.queueMessageRepository = queueMessageRepository;
        this.queueRepository = queueRepository;
    }

    @PostConstruct
    public void initiateBackgroundProcess () {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::processAllUnacks,
                UNACK_SCHEDULE_MS,
                UNACK_SCHEDULE_MS,
                TimeUnit.MILLISECONDS);
    }

    private Date getOffsetAddedDate(int offsetInSeconds) {
        Date oldDate = new Date();
        Calendar gcal = new GregorianCalendar();
        gcal.setTime(oldDate);
        gcal.add(Calendar.SECOND, offsetInSeconds);
        return gcal.getTime();
    }

    public void processAllUnacks() {
        logger.trace("processAllUnacks started");
        queueMessageRepository.updateAllByPoppedAndDeliverOn(
                true,
                getOffsetAddedDate(-60),
                false
        );
    }

    @Override
    public void push(String queueName, String messageId, long offsetTimeInSecond) {
        push(queueName, messageId, 0, offsetTimeInSecond);
    }

    @Override
    public void push(String queueName, String messageId, int priority, long offsetTimeInSecond) {
        pushMessage(queueName, messageId, null, priority, offsetTimeInSecond);
    }

    private void createQueueIfNotExists(String queueName) {
        if(queueRepository.existsByName(queueName)) {
            return;
        }
        QueueDocument queueDocument = new QueueDocument();
        queueDocument.setName(queueName);
        queueRepository.save(queueDocument);
    }

    private void pushMessage(
            String queueName, String messageId, String payload,
            Integer priority, long offsetTimeInSecond) {
        createQueueIfNotExists(queueName);
        QueueMessage queueMessage = queueMessageRepository.getFirstByQueueNameAndMessageId(
                queueName,
                messageId
        );
        if(null != queueMessage) {
            queueMessage.setPriority(priority);
            queueMessage.setOffsetTimeSeconds(offsetTimeInSecond);
            queueMessage.setDeliverOn(getOffsetAddedDate(((Long)offsetTimeInSecond).intValue()));
            queueMessage.setPopped(false);
        } else {
            queueMessage = new QueueMessage();
            queueMessage.setQueueName(queueName);
            queueMessage.setMessageId(messageId);
            queueMessage.setPriority(priority);
            queueMessage.setOffsetTimeSeconds(offsetTimeInSecond);
            queueMessage.setDeliverOn(getOffsetAddedDate(((Long)offsetTimeInSecond).intValue()));
            queueMessage.setPopped(false);
            queueMessage.setPayload(payload);
        }
        queueMessageRepository.save(queueMessage);
    }

    @Override
    public void push(String queueName, List<Message> messages) {
        messages.forEach(message ->
            pushMessage(
                queueName,
                message.getId(),
                message.getPayload(),
                message.getPriority(),
                0
            )
        );
    }

    @Override
    public boolean pushIfNotExists(String queueName, String messageId, long offsetTimeInSecond) {
        return pushIfNotExists(queueName, messageId, 0, offsetTimeInSecond);
    }

    @Override
    public boolean pushIfNotExists(String queueName, String messageId, int priority, long offsetTimeInSecond) {
        if (!queueMessageRepository.existsByQueueNameAndMessageId(queueName, messageId)) {
            pushMessage(queueName, messageId, null, priority, offsetTimeInSecond);
            return true;
        }
        return false;
    }

    @Override
    public boolean containsMessage(String queueName, String messageId) {
        return queueMessageRepository.existsByQueueNameAndMessageId(queueName, messageId);
    }

    private List<Message> peekMessages(String queueName, int count) {
        if (count < 1) {
            return Collections.emptyList();
        }
        // FIXME Should We use Stream or Query should limit the count
        List<QueueMessage> queueMessages = queueMessageRepository.getQueueMessagesBy(
                queueName,
                false,
                getOffsetAddedDate(1000)
        ).stream().limit(count).toList();
        if(queueMessages.isEmpty()) {
            return Collections.emptyList();
        }
        return queueMessages.stream().map(queueMessage -> {
            Message message = new Message();
            message.setId(queueMessage.getMessageId());
            message.setPriority(queueMessage.getPriority());
            message.setPayload(queueMessage.getPayload());
            return message;
        }).toList();
    }

    private List<Message> popMessages(String queueName, int count, int timeout) {
        long start = System.currentTimeMillis();
        List<Message> messages = peekMessages(queueName, count);
        while (messages.size() < count && ((System.currentTimeMillis() - start) < timeout)) {
            Uninterruptibles.sleepUninterruptibly(200, TimeUnit.MILLISECONDS);
            messages = peekMessages(queueName, count);
        }
        if (messages.isEmpty()) {
            return messages;
        }
        List<String> messageId = messages.stream().map(Message::getId).toList();
        queueMessageRepository.updateByPoppedAndQueueNameAndMessageIds(
                false, queueName, messageId, true);
        return messages;
    }

    @Override
    public List<String> pop(String queueName, int count, int timeout) {
        List<Message> messages = popMessages(queueName, count, timeout);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }
        return messages.stream().map(Message::getId).collect(Collectors.toList());
    }

    @Override
    public List<Message> pollMessages(String queueName, int count, int timeout) {
        List<Message> messages = popMessages(queueName, count, timeout);
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }
        return messages;
    }

    private void removeMessage(String queueName, String messageId) {
        queueMessageRepository.deleteAllByQueueNameAndMessageId(queueName, messageId);
    }

    @Override
    public void remove(String queueName, String messageId) {
        removeMessage(queueName, messageId);
    }

    @Override
    public int getSize(String queueName) {
        return (int) queueMessageRepository.countByQueueName(queueName);
    }

    @Override
    public boolean ack(String queueName, String messageId) {
        retryTemplate.execute( context -> {
            removeMessage(queueName, messageId);
            return null;
        });
        return true;
    }

    @Override
    public boolean setUnackTimeout(String queueName, String messageId, long unackTimeout) {
        long updatedOffsetTimeInSecond = unackTimeout / 1000;
        queueMessageRepository.updateByQueueNameAndMessageId(
                queueName,
                messageId,
                updatedOffsetTimeInSecond,
                getOffsetAddedDate(((Long)updatedOffsetTimeInSecond).intValue())
        );
        return true;
    }

    @Override
    public void flush(String queueName) {
        queueMessageRepository.deleteAllByQueueName(queueName);
    }

    @Override
    public Map<String, Long> queuesDetail() {
        // FIXME Revisit for optimization in query.
        List<String> queueNames = mongoTemplate.findDistinct("name", QueueDocument.class, String.class);
        return queueNames.stream().map(queueName -> {
            long count = queueMessageRepository.countByQueueNameAndPopped(queueName, false);
            return new Pair<>(queueName, count);
        }).collect(Collectors.toMap(Pair::getValue0, Pair::getValue1));
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> queuesDetailVerbose() {
        // FIXME Revisit for optimization in query.
        List<String> queueNames = mongoTemplate.findDistinct("name", QueueDocument.class, String.class);
        return queueNames.stream().map(queueName -> {
            long size = queueMessageRepository.countByQueueNameAndPopped(queueName, false);
            long uacked = queueMessageRepository.countByQueueNameAndPopped(queueName, true);
            return new Pair<String, Map<String, Map<String, Long>>>(
                    queueName,
                    ImmutableMap.of("a", ImmutableMap.of("size", size, "uacked", uacked ))
            );
        }).collect(Collectors.toMap(Pair::getValue0, Pair::getValue1));
    }

    @Override
    public boolean resetOffsetTime(String queueName, String messageId) {
        // FIXME return true based on the update done, currently updateByQueueNameAndMessageId do not return anything.
        queueMessageRepository.updateByQueueNameAndMessageId(
            queueName,
            messageId,
            0,
            getOffsetAddedDate(0)
        );
        return true;
    }
}
