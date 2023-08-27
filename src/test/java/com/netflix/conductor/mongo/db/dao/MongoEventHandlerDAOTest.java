package com.netflix.conductor.mongo.db.dao;

import com.netflix.conductor.common.metadata.events.EventHandler;
import com.netflix.conductor.mongo.db.TestConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {TestConfiguration.class})
@DataMongoTest
class MongoEventHandlerDAOTest extends BaseMongoTest {

    @Autowired
    private MongoEventHandlerDAO mongoEventHandlerDAO;

    @Test
    void addEventHandler() {
        assertThat(mongoEventHandlerDAO).isNotNull();
    }

    @Test
    public void testEventHandlers() {
        String event1 = "SQS::arn:account090:sqstest1";
        String event2 = "SQS::arn:account090:sqstest2";

        EventHandler eventHandler = new EventHandler();
        eventHandler.setName(UUID.randomUUID().toString());
        eventHandler.setActive(false);
        EventHandler.Action action = new EventHandler.Action();
        action.setAction(EventHandler.Action.Type.start_workflow);
        action.setStart_workflow(new EventHandler.StartWorkflow());
        action.getStart_workflow().setName("workflow_x");
        eventHandler.getActions().add(action);
        eventHandler.setEvent(event1);

        mongoEventHandlerDAO.addEventHandler(eventHandler);
        List<EventHandler> all = mongoEventHandlerDAO.getAllEventHandlers();
        Assertions.assertNotNull(all);
        Assertions.assertEquals(1, all.size());
        Assertions.assertEquals(eventHandler.getName(), all.get(0).getName());
        Assertions.assertEquals(eventHandler.getEvent(), all.get(0).getEvent());

        List<EventHandler> byEvents = mongoEventHandlerDAO.getEventHandlersForEvent(event1, true);
        Assertions.assertNotNull(byEvents);
        Assertions.assertEquals(0, byEvents.size());        //event is marked as in-active

        eventHandler.setActive(true);
        eventHandler.setEvent(event2);
        mongoEventHandlerDAO.updateEventHandler(eventHandler);

        all = mongoEventHandlerDAO.getAllEventHandlers();
        Assertions.assertNotNull(all);
        Assertions.assertEquals(1, all.size());

        byEvents = mongoEventHandlerDAO.getEventHandlersForEvent(event1, true);
        Assertions.assertNotNull(byEvents);
        Assertions.assertEquals(0, byEvents.size());

        byEvents = mongoEventHandlerDAO.getEventHandlersForEvent(event2, true);
        Assertions.assertNotNull(byEvents);
        Assertions.assertEquals(1, byEvents.size());
    }

}