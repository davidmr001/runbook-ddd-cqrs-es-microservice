package io.cqrs.taskmanagement.read.model.runbook;

import io.cqrs.taskmanagement.domain.model.runbook.TaskAdded;
import io.cqrs.taskmanagement.persistence.JpaEventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Observable;
import java.util.Observer;

/**
 * Read Model Projection
 */
@Component
public class RunbookTasksProjection implements Observer {

    private TaskRepository taskRepository;

    @Autowired
    public RunbookTasksProjection(JpaEventStore jpaEventStore, TaskRepository taskRepository) {
        this.taskRepository = taskRepository;

        // Subscribe Read Model to new Events
        jpaEventStore.addObserver(this);
    }

    @Override
    public void update(Observable observable, Object event) {
        // Handles only Events this Projection cares about
        if (event instanceof TaskAdded) {
            handleTaskAdded((TaskAdded) event);
        }
    }

    private void handleTaskAdded(TaskAdded event) {
        // For now persisting read models as Entities, but the read model DTOs could be persisted directly to file system, etc.

        TaskEntity task = new TaskEntity(
                event.getRunbookId(),
                event.getTaskId(),
                event.getAssigneeId(),
                event.getName(),
                event.getDescription(),
                null
        );

        taskRepository.save(task);
    }

}
