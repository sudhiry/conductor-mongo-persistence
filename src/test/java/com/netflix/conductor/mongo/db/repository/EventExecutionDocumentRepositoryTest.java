package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.TestConfiguration;
import com.netflix.conductor.mongo.db.models.EventExecutionDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;


@ContextConfiguration(classes = {TestConfiguration.class})
@DataMongoTest
class EventExecutionDocumentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private EventExecutionDocumentRepository eventExecutionDocumentRepository;

    @BeforeEach
    void setUp() {
        EventExecutionDocument eventExecutionDocument1 =
                new EventExecutionDocument(
                        "some-event-name-1",
                        "some-event-handler-1",
                        "message-id-1",
                        "execution-id-1",
                        "{}",
                        new Date(),
                        new Date()
                );
        eventExecutionDocumentRepository.save(eventExecutionDocument1);
        EventExecutionDocument eventExecutionDocument2 =
                new EventExecutionDocument(
                        "some-event-name-2",
                        "some-event-handler-2",
                        "message-id-2",
                        "execution-id-2",
                        "{}",
                        new Date(),
                        new Date()
                );
        eventExecutionDocumentRepository.save(eventExecutionDocument2);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void eventExecutionDocumentRepositoryIsNotNull() {
        assertThat(eventExecutionDocumentRepository).isNotNull();
    }

    @Test
    void updateEventExecutionDocument() {
        EventExecutionDocument gotEventExecutionDocForCreatedOn =
                eventExecutionDocumentRepository.getFirstBy("some-event-handler-1",
                        "some-event-name-1",
                        "message-id-1",
                        "execution-id-1");
        assertThat(gotEventExecutionDocForCreatedOn).isNotNull();
        assertThat(gotEventExecutionDocForCreatedOn.getCreatedOn()).isNotNull();
        eventExecutionDocumentRepository.updateEventExecutionDocument(
                "some-event-handler-1",
                "some-event-name-1",
                "message-id-1",
                "execution-id-1",
                "{\"test\": 1}"
                );
        EventExecutionDocument gotEventExecutionDocOption =
                eventExecutionDocumentRepository.getFirstBy(
                        "some-event-handler-1",
                        "some-event-name-1",
                        "message-id-1",
                        "execution-id-1");
        assertThat(gotEventExecutionDocOption).isNotNull();
        assertThat(gotEventExecutionDocOption.getJsonData()).isEqualTo("{\"test\": 1}");
        assertThat(gotEventExecutionDocOption.getModifiedOn()).isNotNull();
    }

    @Test
    void deleteEventExecutionDocument() {
        eventExecutionDocumentRepository.deleteEventExecutionDocument(
                "some-event-handler-1",
                "some-event-name-1",
                "message-id-1",
                "execution-id-1");
        EventExecutionDocument gotEventExecutionDoc =
                eventExecutionDocumentRepository.getFirstBy(
                        "some-event-handler-1",
                        "some-event-name-1",
                        "message-id-1",
                        "execution-id-1");
        assertThat(gotEventExecutionDoc).isNull();
    }

}