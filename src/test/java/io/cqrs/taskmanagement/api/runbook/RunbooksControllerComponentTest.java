package io.cqrs.taskmanagement.api.runbook;

import io.cqrs.taskmanagement.persistence.JpaEventStoreRepository;
import io.cqrs.taskmanagement.read.model.runbook.RunbookRepository;
import io.cqrs.taskmanagement.read.model.runbook.RunbookEntity;
import io.cqrs.taskmanagement.read.model.runbook.TaskEntity;
import io.cqrs.taskmanagement.read.model.runbook.TaskRepository;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RunbooksControllerComponentTest {

    private static final String RUNBOOKS_URL = "/runbooks";
    private static final String PROJECT_ID = "project-id";
    private static final String RUNBOOK_ID = "runbook-id";
    private static final String RUNBOOK_NAME = "runbook-name";
    private static final String OWNER_ID = "owner-id";
    private static final String TASK_NAME = "task-name";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JpaEventStoreRepository eventStoreRepository;

    @Autowired
    private RunbookRepository runbookRepository;

    @Autowired
    private TaskRepository taskRepository;

    @After
    public void tearDown() {
        eventStoreRepository.deleteAll();
        runbookRepository.deleteAll();
        taskRepository.deleteAll();
    }

    @Test
    public void createRunbook_when_success_then_persists_runbookCreated_event() {
        HttpEntity<RunbookDto> request = new HttpEntity<>(new RunbookDto(PROJECT_ID, RUNBOOK_NAME, OWNER_ID));

        ResponseEntity<RunbookDto> response = restTemplate.postForEntity(RUNBOOKS_URL, request, RunbookDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().runbookId).isNotNull();
        assertThat(eventStoreRepository.count()).isOne();
        assertThat(eventStoreRepository.findAll().get(0).getTypeName()).contains("RunbookCreated");
        assertThat(eventStoreRepository.findAll().get(0).getAggregateId()).isEqualTo(response.getBody().runbookId);
    }

    @Test
    public void createRunbook_when_success_then_read_model_projection_persists_a_runbook_entity() {
        String userId = "user-id";
        HttpEntity<RunbookDto> request = new HttpEntity<>(new RunbookDto(PROJECT_ID, RUNBOOK_NAME, userId));

        ResponseEntity<RunbookDto> response = restTemplate.postForEntity(RUNBOOKS_URL, request, RunbookDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(runbookRepository.count()).isOne();
        RunbookEntity runbookEntity = runbookRepository.findOne(response.getBody().runbookId);
        assertThat(runbookEntity.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(runbookEntity.getName()).isEqualTo(RUNBOOK_NAME);
        assertThat(runbookEntity.getOwnerId()).isEqualTo(userId);
    }

    @Test
    public void closeRunbook_when_success_then_read_model_projection_removes_runbook_entity() {
        RunbookDto runbook = restTemplate.postForObject(
                RUNBOOKS_URL,
                new HttpEntity<>(new RunbookDto(PROJECT_ID, RUNBOOK_NAME, OWNER_ID)),
                RunbookDto.class);
        assertThat(runbookRepository.count()).isOne();

        String url = RUNBOOKS_URL + "/" + runbook.getRunbookId() + "/actions/complete";
        restTemplate.postForLocation(url, RunbookDto.class);

        assertThat(runbookRepository.count()).isZero();
    }

    @Test
    public void createTask_when_success_then_persists_taskAdded_event() {
        RunbookDto runbook = restTemplate.postForObject(
                RUNBOOKS_URL,
                new HttpEntity<>(new RunbookDto(PROJECT_ID, RUNBOOK_NAME, OWNER_ID)),
                RunbookDto.class);

        String runbookId = runbook.getRunbookId();
        String url = RUNBOOKS_URL + "/" + runbookId + "/tasks";
        HttpEntity<TaskDto> request = new HttpEntity<>(new TaskDto(runbookId, OWNER_ID, TASK_NAME));

        ResponseEntity<TaskDto> response = restTemplate.postForEntity(url, request, TaskDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(eventStoreRepository.count()).isEqualTo(2);
        assertThat(eventStoreRepository.findAll().get(1).getAggregateId()).isEqualTo(runbookId);
        assertThat(eventStoreRepository.findAll().get(1).getTypeName()).contains("TaskAdded");
    }

    @Test
    public void createTask_when_success_then_read_model_projection_persists_a_task_entity() {
        RunbookDto runbook = restTemplate.postForObject(
                RUNBOOKS_URL,
                new HttpEntity<>(new RunbookDto(PROJECT_ID, RUNBOOK_NAME, OWNER_ID)),
                RunbookDto.class);

        String runbookId = runbook.getRunbookId();
        String url = RUNBOOKS_URL + "/" + runbookId + "/tasks";
        HttpEntity<TaskDto> request = new HttpEntity<>(new TaskDto(runbookId, OWNER_ID, TASK_NAME));

        ResponseEntity<TaskDto> response = restTemplate.postForEntity(url, request, TaskDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(taskRepository.count()).isOne();
        TaskEntity taskEntity = taskRepository.findTasksByRunbookId(runbookId).get(0);
        assertThat(taskEntity.getRunbookId()).isEqualTo(runbookId);
        assertThat(taskEntity.getName()).isEqualTo(TASK_NAME);
        assertThat(taskEntity.getAssigneeId()).isEqualTo(OWNER_ID);
    }

    @Test
    public void getUncompletedRunbooks_when_runbooks_not_completed_exist_on_read_model_then_returns_them() {
        RunbookEntity runbook1 = new RunbookEntity("runbook-id-1", PROJECT_ID, "runbook-name-1", OWNER_ID, false);
        RunbookEntity runbook2 = new RunbookEntity("runbook-id-2", PROJECT_ID, "runbook-name-2", OWNER_ID, false);
        runbookRepository.save(runbook1);
        runbookRepository.save(runbook2);

        RunbookDto[] runbooks = restTemplate.getForObject(RUNBOOKS_URL, RunbookDto[].class);

        assertThat(runbooks).hasSize(2);
        assertThat(runbooks).extracting("name").contains("runbook-name-1", "runbook-name-2");
    }

    @Test
    public void getTasksForRunbook_when_tasks_exist_on_read_model_then_returns_them() {
        TaskEntity task1 = new TaskEntity(RUNBOOK_ID, "task-id-1", OWNER_ID, "task-name-1", "task-description-1", null);
        TaskEntity task2 = new TaskEntity(RUNBOOK_ID, "task-id-2", OWNER_ID, "task-name-2", "task-description-2", null);
        TaskEntity task3 = new TaskEntity("another-runbook", "task-id-3", OWNER_ID, "task-name-3", "task-description-3", null);
        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);

        String url = RUNBOOKS_URL + "/" + RUNBOOK_ID + "/tasks";
        TaskDto[] tasks = restTemplate.getForObject(url, TaskDto[].class);

        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting("name").contains("task-name-1", "task-name-2");
    }

//    @Test
//    public void createTask_when_nonexisting_runbook_then_returns_404_not_found() {
//        String runbookId = "non-existing-runbook-id";
//        String url = RUNBOOKS_URL + "/" + runbookId + "/tasks";
//        TaskDto taskDto = new TaskDto(runbookId, OWNER_ID, TASK_NAME);
//        HttpEntity<TaskDto> createTaskRequest = new HttpEntity<>(taskDto);
//
//        ResponseEntity<TaskDto> response = restTemplate.exchange(url, HttpMethod.POST, createTaskRequest, TaskDto.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
//
//        assertThat(eventStoreRepository.count()).isEqualTo(0);
//    }

}
