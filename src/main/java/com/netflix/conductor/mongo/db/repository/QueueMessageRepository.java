package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.QueueMessage;
import org.springframework.data.mongodb.repository.*;

import java.util.Date;
import java.util.List;

public interface QueueMessageRepository extends MongoRepository<QueueMessage, String> {

    @Query("{ popped: ?0, deliver_on: { $lt: ?1 } }")
    @Update("{ $set: { popped: ?2 } }")
    void updateAllByPoppedAndDeliverOn(boolean popped, Date deliverOn, boolean updatedPopped);

    @Query("{ popped: ?0, queue_name: ?1, message_id: { $in: ?2 } }")
    @Update("{ $set: { popped: ?3 } }")
    void updateByPoppedAndQueueNameAndMessageIds(boolean popped, String queueName, List<String> messageIds, boolean updatedPopped);

    @Query("{ queue_name: ?0, message_id: ?1 }")
    @Update("{ $set: { offset_time_seconds: ?2, deliver_on: ?3 } }")
    void updateByQueueNameAndMessageId(String queueName, String messageId, long offsetTimeSeconds, Date deliverOn);

    QueueMessage getFirstByQueueNameAndMessageId(String queueName, String messageId);

    @ExistsQuery("{ queue_name: ?0, message_id: ?1 }")
    boolean existsByQueueNameAndMessageId(String queueName, String messageId);

    @Query(value = "{ queue_name: ?0, popped: ?1, deliver_on: { $lte: ?2 } }", sort = "{ priority: -1, created_on: 1, deliver_on: 1 }")
    List<QueueMessage> getQueueMessagesBy(String queueName, boolean popped, Date deliverOn);

    long countByQueueNameAndPopped(String queueName, boolean popped);

    void deleteAllByQueueNameAndMessageId(String queueName, String messageId);

    @CountQuery("{ queue_name: ?0 }")
    long countByQueueName(String queueName);

    @DeleteQuery("{ queue_name: ?0 }")
    void deleteAllByQueueName(String queueName);

}
