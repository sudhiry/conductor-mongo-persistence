package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.TestConfiguration;
import com.netflix.conductor.mongo.db.models.PollDataDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {TestConfiguration.class})
@DataMongoTest
class PollDataDocumentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PollDataDocumentRepository pollDataDocumentRepository;

    @BeforeEach
    void setUp() {
        PollDataDocument pollDataDocument1 = new PollDataDocument();
        pollDataDocument1.setDomain("domain-1");
        pollDataDocument1.setQueueName("queue-1");
        pollDataDocument1.setJsonData("{}");
        pollDataDocumentRepository.save(pollDataDocument1);
        PollDataDocument pollDataDocument2 = new PollDataDocument();
        pollDataDocument2.setDomain("domain-2");
        pollDataDocument2.setQueueName("queue-2");
        pollDataDocument2.setJsonData("{}");
        pollDataDocumentRepository.save(pollDataDocument2);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void updateByQueueNameAndDomain() {
        pollDataDocumentRepository.updateFirstByQueueNameAndDomain("queue-1", "domain-1", "{ \"test\"}: true");
        PollDataDocument pollDataDocument = pollDataDocumentRepository.getFirstByQueueNameAndDomain("queue-1", "domain-1");
        assertThat(pollDataDocument).isNotNull();
        assertThat(pollDataDocument.getJsonData()).isEqualTo("{ \"test\"}: true");
    }

    @Test
    void existsByQueueNameAndDomain() {
        boolean exists1 = pollDataDocumentRepository.existsByQueueNameAndDomain("queue-1", "domain-1");
        assertThat(exists1).isTrue();
        boolean exists2 = pollDataDocumentRepository.existsByQueueNameAndDomain("queue-3", "domain-1");
        assertThat(exists2).isFalse();
    }

    @Test
    void getFirstByQueueNameAndDomain() {
        PollDataDocument pollDataDocument = pollDataDocumentRepository.getFirstByQueueNameAndDomain("queue-1", "domain-1");
        assertThat(pollDataDocument).isNotNull();
        assertThat(pollDataDocument.getJsonData()).isEqualTo("{}");
    }

    @Test
    void getAllByQueueName() {
        PollDataDocument pollDataDocument = new PollDataDocument();
        pollDataDocument.setDomain("domain-5");
        pollDataDocument.setQueueName("queue-5");
        pollDataDocument.setJsonData("{}");
        pollDataDocumentRepository.save(pollDataDocument);
        List<PollDataDocument> list = pollDataDocumentRepository.getAllByQueueName("queue-5");
        assertThat(list.size()).isEqualTo(1);
    }
}