package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.TestConfiguration;
import com.netflix.conductor.mongo.db.models.QueueMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {TestConfiguration.class})
@DataMongoTest
public class QueueMessageRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private QueueMessageRepository queueMessageRepository;

    @BeforeEach
    void setUp() {
        QueueMessage queueMessage1 = new QueueMessage();
        queueMessage1.setQueueName("queue-1");
        queueMessage1.setMessageId("message-1");
        queueMessage1.setPopped(true);
        queueMessage1.setPriority(0);
        queueMessage1.setOffsetTimeSeconds(100);
        queueMessage1.setDeliverOn(getOffsetAddedDate(-70));
        queueMessage1.setPayload("{}");
        queueMessageRepository.save(queueMessage1);
        QueueMessage queueMessage2 = new QueueMessage();
        queueMessage2.setQueueName("queue-1");
        queueMessage2.setMessageId("message-2");
        queueMessage2.setPopped(true);
        queueMessage2.setPriority(0);
        queueMessage2.setOffsetTimeSeconds(100);
        queueMessage2.setDeliverOn(new Date());
        queueMessage2.setPayload("{}");
        queueMessageRepository.save(queueMessage2);
        QueueMessage queueMessage3 = new QueueMessage();
        queueMessage3.setQueueName("queue-2");
        queueMessage3.setMessageId("message-1");
        queueMessage3.setPopped(true);
        queueMessage3.setPriority(0);
        queueMessage3.setOffsetTimeSeconds(100);
        queueMessage3.setDeliverOn(new Date());
        queueMessage3.setPayload("{}");
        queueMessageRepository.save(queueMessage2);
    }

    private Date getOffsetAddedDate(int offsetInSeconds) {
        Date oldDate = new Date();
        Calendar gcal = new GregorianCalendar();
        gcal.setTime(oldDate);
        gcal.add(Calendar.SECOND, offsetInSeconds);
        Date newDate = gcal.getTime();
        return newDate;
    }

    @Test
    void updateAllByPoppedAndDeliverOn() {
        Date date = getOffsetAddedDate(-60);
        queueMessageRepository.updateAllByPoppedAndDeliverOn(
                true,
                date,
                false
        );
        List<QueueMessage> list = queueMessageRepository.findAll();
        list.forEach(queueMessage -> {
            if(queueMessage.getDeliverOn().compareTo(date) < 0 ) {
                assertThat(queueMessage.isPopped()).isFalse();
            } else {
                assertThat(queueMessage.isPopped()).isTrue();
            }
        });
    }

    @Test
    void updateByPoppedAndQueueNameAndMessageIds() {
        QueueMessage queueMessage1 = new QueueMessage();
        queueMessage1.setQueueName("queue-3");
        queueMessage1.setMessageId("message-1");
        queueMessage1.setPopped(false);
        queueMessage1.setPriority(0);
        queueMessage1.setDeliverOn(getOffsetAddedDate(-70));
        queueMessage1.setPayload("{}");
        queueMessageRepository.save(queueMessage1);
        QueueMessage queueMessage2 = new QueueMessage();
        queueMessage2.setQueueName("queue-3");
        queueMessage2.setMessageId("message-2");
        queueMessage2.setPopped(false);
        queueMessage2.setPriority(0);
        queueMessage2.setDeliverOn(getOffsetAddedDate(-70));
        queueMessage2.setPayload("{}");
        queueMessageRepository.save(queueMessage2);
        QueueMessage queueMessage3 = new QueueMessage();
        queueMessage3.setQueueName("queue-3");
        queueMessage3.setMessageId("message-3");
        queueMessage3.setPopped(false);
        queueMessage3.setPriority(0);
        queueMessage3.setDeliverOn(getOffsetAddedDate(-70));
        queueMessage3.setPayload("{}");
        queueMessageRepository.save(queueMessage3);
        queueMessageRepository.updateByPoppedAndQueueNameAndMessageIds(
                false,"queue-3", List.of("message-1", "message-2"), true
        );
        QueueMessage out1 = queueMessageRepository.getFirstByQueueNameAndMessageId("queue-3", "message-1");
        assertThat(out1.isPopped()).isEqualTo(true);
        QueueMessage out2 = queueMessageRepository.getFirstByQueueNameAndMessageId("queue-3", "message-2");
        assertThat(out2.isPopped()).isEqualTo(true);
        QueueMessage out3 = queueMessageRepository.getFirstByQueueNameAndMessageId("queue-3", "message-3");
        assertThat(out3.isPopped()).isEqualTo(false);
    }

    @Test
    void updateByQueueNameAndMessageId() {
        Date date = new Date();
        queueMessageRepository.updateByQueueNameAndMessageId("queue-1", "message-1", 0, date);
        QueueMessage out1 = queueMessageRepository.getFirstByQueueNameAndMessageId("queue-1", "message-1");
        assertThat(out1.getOffsetTimeSeconds()).isEqualTo(0);
        assertThat(out1.getDeliverOn()).isEqualTo(date);
    }

    @Test
    void getFirstByQueueNameAndMessageId() {
        QueueMessage out1 = queueMessageRepository.getFirstByQueueNameAndMessageId("queue-1", "message-1");
        assertThat(out1).isNotNull();
    }

    @Test
    void existsByQueueNameAndMessageId() {
        boolean out1 = queueMessageRepository.existsByQueueNameAndMessageId("queue-1", "message-1");
        assertThat(out1).isTrue();
        boolean out2 = queueMessageRepository.existsByQueueNameAndMessageId("queue-1", "message-5");
        assertThat(out2).isFalse();
    }

    // TODO add remaining tests
}
