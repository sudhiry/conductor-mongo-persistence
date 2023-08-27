package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.TestConfiguration;
import com.netflix.conductor.mongo.db.models.MetaEventHandler;
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
public class EventHandlerRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private EventHandlerRepository eventHandlerRepository;

    @BeforeEach
    void setUp() {
        MetaEventHandler metaEventHandler1 = new MetaEventHandler();
        metaEventHandler1.setName("name-1");
        metaEventHandler1.setEvent("event-1");
        metaEventHandler1.setActive(true);
        metaEventHandler1.setJsonData("{}");
        eventHandlerRepository.save(metaEventHandler1);
        MetaEventHandler metaEventHandler2 = new MetaEventHandler();
        metaEventHandler2.setName("name-2");
        metaEventHandler2.setEvent("event-2");
        metaEventHandler2.setActive(true);
        metaEventHandler2.setJsonData("{}");
        eventHandlerRepository.save(metaEventHandler2);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void findFirstByName() {
        MetaEventHandler out = eventHandlerRepository.getFirstByName("name-1");
        assertThat(out).isNotNull();
    }

    @Test
    void existsEventHandlerByName() {
        boolean isExist1 = eventHandlerRepository.existsByName("name-2");
        assertThat(isExist1).isTrue();
        boolean isExist2 = eventHandlerRepository.existsByName("name-3");
        assertThat(isExist2).isFalse();
    }

    @Test
    void deleteEventHandlerByName() {
        eventHandlerRepository.deleteEventHandlerByName("name-2");
        boolean isExist = eventHandlerRepository.existsByName("name-2");
        assertThat(isExist).isFalse();
    }

    @Test
    void getEventHandlersByEventAndActive() {
        MetaEventHandler metaEventHandler = new MetaEventHandler();
        metaEventHandler.setName("name-4");
        metaEventHandler.setEvent("event-2");
        metaEventHandler.setActive(true);
        metaEventHandler.setJsonData("{}");
        eventHandlerRepository.save(metaEventHandler);
        List<MetaEventHandler> list = eventHandlerRepository.getEventHandlersByEventAndActive("event-2", true);
        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    void updateEventHandlerByName() {
        eventHandlerRepository.updateEventHandlerByName(
            "name-1",
            "event-11",
            false,
            "{ \"status\": false}"
        );
        MetaEventHandler out = eventHandlerRepository.getFirstByName("name-1");
        assertThat(out.getEvent()).isEqualTo("event-11");
        assertThat(out.getActive()).isEqualTo(false);
        assertThat(out.getJsonData()).isEqualTo("{ \"status\": false}");
    }
}
