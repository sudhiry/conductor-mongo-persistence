package com.netflix.conductor.mongo.db.dao;

import com.google.common.collect.ImmutableList;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.mongo.db.TestConfiguration;
import com.netflix.conductor.mongo.db.models.QueueDocument;
import com.netflix.conductor.mongo.db.models.QueueMessage;

import com.netflix.conductor.mongo.db.repository.QueueMessageRepository;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ContextConfiguration(classes = {TestConfiguration.class})
@DataMongoTest
@TestMethodOrder(MethodOrderer.MethodName.class)
class MongoQueueDAOTest extends BaseMongoTest {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private MongoQueueDAO mongoQueueDAO;

    @Autowired
    private QueueMessageRepository queueMessageRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUpBeforeEach() {
        mongoTemplate.dropCollection(QueueMessage.class);
        mongoTemplate.dropCollection(QueueDocument.class);
    }

    @Test
    public void complexQueueTest() {
        String queueName = "TestQueueComplex";
        long offsetTimeInSecond = 0;

        for (int i = 0; i < 10; i++) {
            String messageId = "complex_msg_" + i;
            mongoQueueDAO.push(queueName, messageId, offsetTimeInSecond);
        }
        int size = mongoQueueDAO.getSize(queueName);
        Assertions.assertEquals(10, size);
        Map<String, Long> details = mongoQueueDAO.queuesDetail();
        Assertions.assertEquals(1, details.size());
        Assertions.assertEquals(10L, details.get(queueName).longValue());

        for (int i = 0; i < 10; i++) {
            String messageId = "complex_msg_" + i;
            mongoQueueDAO.pushIfNotExists(queueName, messageId, offsetTimeInSecond);
        }

        List<String> popped = mongoQueueDAO.pop(queueName, 10, 100);
        Assertions.assertNotNull(popped);
        Assertions.assertEquals(10, popped.size());

        Map<String, Map<String, Map<String, Long>>> verbose = mongoQueueDAO.queuesDetailVerbose();
        Assertions.assertEquals(1, verbose.size());
        long shardSize = verbose.get(queueName).get("a").get("size");
        long unackedSize = verbose.get(queueName).get("a").get("uacked");
        Assertions.assertEquals(0, shardSize);
        Assertions.assertEquals(10, unackedSize);

        popped.forEach(messageId -> mongoQueueDAO.ack(queueName, messageId));

        verbose = mongoQueueDAO.queuesDetailVerbose();
        Assertions.assertEquals(1, verbose.size());
        shardSize = verbose.get(queueName).get("a").get("size");
        unackedSize = verbose.get(queueName).get("a").get("uacked");
        Assertions.assertEquals(0, shardSize);
        Assertions.assertEquals(0, unackedSize);

        popped = mongoQueueDAO.pop(queueName, 10, 100);
        Assertions.assertNotNull(popped);
        Assertions.assertEquals(0, popped.size());

        for (int i = 0; i < 10; i++) {
            String messageId = "complex_msg_" + i;
            mongoQueueDAO.pushIfNotExists(queueName, messageId, offsetTimeInSecond);
        }
        size = mongoQueueDAO.getSize(queueName);
        Assertions.assertEquals(10, size);

        for (int i = 0; i < 10; i++) {
            String messageId = "complex_msg_" + i;
            Assertions.assertTrue(mongoQueueDAO.containsMessage(queueName, messageId));
            mongoQueueDAO.remove(queueName, messageId);
        }

        size = mongoQueueDAO.getSize(queueName);
        Assertions.assertEquals(0, size);

        for (int i = 0; i < 10; i++) {
            String messageId = "complex_msg_" + i;
            mongoQueueDAO.pushIfNotExists(queueName, messageId, offsetTimeInSecond);
        }
        mongoQueueDAO.flush(queueName);
        size = mongoQueueDAO.getSize(queueName);
        Assertions.assertEquals(0, size);
    }

    /**
     * Test fix for https://github.com/Netflix/conductor/issues/1892
     */
    @Test
    public void containsMessageTest() {
        String queueName = "TestQueueContains";
        long offsetTimeInSecond = 0;

        for (int i = 0; i < 10; i++) {
            String messageId = "msg" + i;
            mongoQueueDAO.push(queueName, messageId, offsetTimeInSecond);
        }
        int size = mongoQueueDAO.getSize(queueName);
        Assertions.assertEquals(10, size);

        for (int i = 0; i < 10; i++) {
            String messageId = "msg" + i;
            Assertions.assertTrue(mongoQueueDAO.containsMessage(queueName, messageId));
            mongoQueueDAO.remove(queueName, messageId);
        }
        for (int i = 0; i < 10; i++) {
            String messageId = "msg" + i;
            Assertions.assertFalse(mongoQueueDAO.containsMessage(queueName, messageId));
        }
    }

    /**
     * Test fix for https://github.com/Netflix/conductor/issues/399
     *
     * @since 1.8.2-rc5
     */
    @Test
    public void pollMessagesTest() {
        final List<Message> messages = new ArrayList<>();
        final String queueName = "issue399_testQueue";
        final int totalSize = 10;

        for (int i = 0; i < totalSize; i++) {
            String payload = "{\"id\": " + i + ", \"msg\":\"test " + i + "\"}";
            Message m = new Message("testmsg-" + i, payload, "");
            if (i % 2 == 0) {
                // Set priority on message with pair id
                m.setPriority(99 - i);
            }
            messages.add(m);
        }

        // Populate the queue with our test message batch
        mongoQueueDAO.push(queueName, ImmutableList.copyOf(messages));

        // Assert that all messages were persisted and no extras are in there
        Assertions.assertEquals(totalSize, mongoQueueDAO.getSize(queueName));

        final int firstPollSize = 3;
        List<Message> firstPoll = mongoQueueDAO.pollMessages(queueName, firstPollSize, 10_000);
        Assertions.assertNotNull(firstPoll, "First poll was null");
        Assertions.assertFalse(firstPoll.isEmpty(), "First poll was empty");
        Assertions.assertEquals(firstPollSize, firstPoll.size(), "First poll size mismatch");

        final int secondPollSize = 4;
        List<Message> secondPoll = mongoQueueDAO.pollMessages(queueName, secondPollSize, 10_000);
        Assertions.assertNotNull(secondPoll, "Second poll was null");
        Assertions.assertFalse(secondPoll.isEmpty(), "Second poll was empty");
        Assertions.assertEquals(secondPollSize, secondPoll.size(), "Second poll size mismatch");

        // Assert that the total queue size hasn't changed
        Assertions.assertEquals(
                totalSize,
                mongoQueueDAO.getSize(queueName),
                "Total queue size should have remained the same");

        // Assert that our un-popped messages match our expected size
        final long expectedSize = totalSize - firstPollSize - secondPollSize;
        long count = queueMessageRepository.countByQueueNameAndPopped(queueName, false);
        Assertions.assertEquals(expectedSize, count, "Remaining queue size mismatch");
    }

    /**
     * Test fix for https://github.com/Netflix/conductor/issues/448
     *
     * @since 1.8.2-rc5
     */
    @Test
    public void pollDeferredMessagesTest() throws InterruptedException {
        final List<Message> messages = new ArrayList<>();
        final String queueName = "issue448_testQueue";
        final int totalSize = 10;

        for (int i = 0; i < totalSize; i++) {
            int offset = 0;
            if (i < 5) {
                offset = 0;
            } else if (i == 6 || i == 7) {
                // Purposefully skipping id:5 to test out of order deliveries
                // Set id:6 and id:7 for a 2s delay to be picked up in the second polling batch
                offset = 5;
            } else {
                // Set all other queue messages to have enough of a delay that they won't
                // accidentally
                // be picked up.
                offset = 10_000 + i;
            }

            String payload = "{\"id\": " + i + ",\"offset_time_seconds\":" + offset + "}";
            Message m = new Message("testmsg-" + i, payload, "");
            messages.add(m);
            mongoQueueDAO.push(queueName, "testmsg-" + i, offset);
        }

        // Assert that all messages were persisted and no extras are in there
        Assertions.assertEquals(totalSize, mongoQueueDAO.getSize(queueName), "Queue size mismatch");

        final int firstPollSize = 4;
        List<Message> firstPoll = mongoQueueDAO.pollMessages(queueName, firstPollSize, 100);
        Assertions.assertNotNull(firstPoll, "First poll was null");
        Assertions.assertFalse(firstPoll.isEmpty(), "First poll was empty");
        Assertions.assertEquals(firstPollSize, firstPoll.size(), "First poll size mismatch");

        List<String> firstPollMessageIds =
                messages.stream()
                        .map(Message::getId)
                        .toList()
                        .subList(0, firstPollSize + 1);

        for (int i = 0; i < firstPollSize; i++) {
            String actual = firstPoll.get(i).getId();
            Assertions.assertTrue(firstPollMessageIds.contains(actual), "Unexpected Id: " + actual);
        }

        final int secondPollSize = 3;

        // Sleep a bit to get the next batch of messages
        logger.debug("Sleeping for second poll...");
        Thread.sleep(5_000);

        // Poll for many more messages than expected
        List<Message> secondPoll = mongoQueueDAO.pollMessages(queueName, secondPollSize + 10, 100);
        Assertions.assertNotNull(secondPoll, "Second poll was null");
        Assertions.assertFalse(secondPoll.isEmpty(), "Second poll was empty");
        Assertions.assertEquals(secondPollSize, secondPoll.size(), "Second poll size mismatch");

        List<String> expectedIds = Arrays.asList("testmsg-4", "testmsg-6", "testmsg-7");
        for (int i = 0; i < secondPollSize; i++) {
            String actual = secondPoll.get(i).getId();
            Assertions.assertTrue(expectedIds.contains(actual), "Unexpected Id: " + actual);
        }

        // Assert that the total queue size hasn't changed
        Assertions.assertEquals(
                totalSize,
                mongoQueueDAO.getSize(queueName),
                "Total queue size should have remained the same");


        // Assert that our un-popped messages match our expected size
        final long expectedSize = totalSize - firstPollSize - secondPollSize;
        long count = queueMessageRepository.countByQueueNameAndPopped(queueName, false);
        Assertions.assertEquals(expectedSize, count, "Remaining queue size mismatch");
    }

    @Test
    public void processUnacksTest() {
        final String queueName = "process_unacks_test";
        // Count of messages in the queue(s)
        final int count = 10;
        // Number of messages to process acks for
        final int unackedCount = 4;
        // A secondary queue to make sure we don't accidentally process other queues
        final String otherQueueName = "process_unacks_test_other_queue";

        // Create testing queue with some messages (but not all) that will be popped/acked.
        for (int i = 0; i < count; i++) {
            int offset = 0;
            if (i >= unackedCount) {
                offset = 1_000_000;
            }

            mongoQueueDAO.push(queueName, "unack-" + i, offset);
        }

        // Create a second queue to make sure that unacks don't occur for it
        for (int i = 0; i < count; i++) {
            mongoQueueDAO.push(otherQueueName, "other-" + i, 0);
        }

        // Poll for first batch of messages (should be equal to unackedCount)
        List<Message> polled = mongoQueueDAO.pollMessages(queueName, 100, 10_000);
        Assertions.assertNotNull(polled);
        Assertions.assertFalse(polled.isEmpty());
        Assertions.assertEquals(unackedCount, polled.size());

        // Poll messages from the other queue so we know they don't get unacked later
        mongoQueueDAO.pollMessages(otherQueueName, 100, 10_000);

        // Ack one of the polled messages
        Assertions.assertTrue(mongoQueueDAO.ack(queueName, "unack-1"));

        // Should have one less un-acked popped message in the queue
        Long uacked = mongoQueueDAO.queuesDetailVerbose().get(queueName).get("a").get("uacked");
        Assertions.assertNotNull(uacked);
        Assertions.assertEquals(uacked.longValue(), unackedCount - 1);

        // Process unacks
        mongoQueueDAO.processUnacks(queueName);

        // Check uacks for both queues after processing
        Map<String, Map<String, Map<String, Long>>> details = mongoQueueDAO.queuesDetailVerbose();
        uacked = details.get(queueName).get("a").get("uacked");
        Assertions.assertNotNull(uacked);
        Assertions.assertEquals(
                uacked.longValue(),
                unackedCount - 1, "The messages that were polled should be unacked still");

        Long otherUacked = details.get(otherQueueName).get("a").get("uacked");
        Assertions.assertNotNull(otherUacked);
        Assertions.assertEquals(
                otherUacked.longValue(), count, "Other queue should have all unacked messages");

        Long size = mongoQueueDAO.queuesDetail().get(queueName);
        Assertions.assertNotNull(size);
        Assertions.assertEquals(size.longValue(), count - unackedCount);
    }

}