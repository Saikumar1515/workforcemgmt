package com.railse.hiring.workforcemgmt.service.impl;

import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.TaskActivity;
import com.railse.hiring.workforcemgmt.model.TaskComment;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.model.enums.Task;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskRepository taskRepository;
    private final ITaskManagementMapper taskMapper;

    public TaskManagementServiceImpl(TaskRepository taskRepository, ITaskManagementMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskMapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();
        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");

            newTask.setCreatedTime(System.currentTimeMillis());

            createdTasks.add(taskRepository.save(newTask));
        }
        return taskMapper.modelListToDtoList(createdTasks);
    }



    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();
        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));


            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
            }
            updatedTasks.add(taskRepository.save(task));
        }
        return taskMapper.modelListToDtoList(updatedTasks);
    }


    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository
                .findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .toList();

            if (!tasksOfType.isEmpty()) {
                TaskManagement primaryTask = tasksOfType.get(0);
                primaryTask.setAssigneeId(request.getAssigneeId());
                primaryTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(primaryTask);

                for (int i = 1; i < tasksOfType.size(); i++) {
                    TaskManagement duplicate = tasksOfType.get(i);
                    duplicate.setStatus(TaskStatus.CANCELLED);
                    taskRepository.save(duplicate);
                }

            } else {
                TaskManagement newTask = new TaskManagement();
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(newTask);
            }
        }

        return "Tasks reassigned successfully for reference " + request.getReferenceId();
    }


    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> task.getTaskDeadlineTime() != null &&
                        task.getTaskDeadlineTime() >= request.getStartDate() &&
                        task.getTaskDeadlineTime() <= request.getEndDate())
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED)
                .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(filteredTasks);
    }

    @Override
    public List<TaskManagementDto> smartDailyView(TaskFetchByDateRequest request) {
        List<TaskManagement> allTasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());

        List<TaskManagement> smartTasks = allTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.CANCELLED && t.getStatus() != TaskStatus.COMPLETED)
                .filter(t -> {
                    long created = (t.getCreatedTime() != null) ? t.getCreatedTime() : 0L;
                    return
                            // Tasks created in the date range
                            (created >= request.getStartDate() && created <= request.getEndDate())
                                    || (created < request.getStartDate());
                })
                .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(smartTasks);
    }


    @Override
    public TaskManagementDto updatePriority(UpdatePriorityRequest request) {
        TaskManagement task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + request.getTaskId()));

        task.setPriority(request.getPriority());

        task.getActivityHistory().add(
                new TaskActivity(System.currentTimeMillis(), task.getId(),
                        "Priority changed to " + request.getPriority() + " by " + request.getUpdatedBy(),
                        System.currentTimeMillis())
        );

        taskRepository.save(task);
        return taskMapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> getTasksByPriority(Priority priority) {
        List<TaskManagement> tasks = taskRepository.findAll().stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toList());
        return taskMapper.modelListToDtoList(tasks);
    }

    @Override
    public TaskManagementDto addComment(AddCommentRequest request) {
        TaskManagement task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + request.getTaskId()));

        TaskComment comment = new TaskComment(System.currentTimeMillis(), task.getId(),
                request.getAuthor(), request.getComment(), System.currentTimeMillis());
        task.getComments().add(comment);

        task.getActivityHistory().add(
                new TaskActivity(System.currentTimeMillis(), task.getId(),
                        "Comment added by " + request.getAuthor(), System.currentTimeMillis())
        );

        taskRepository.save(task);
        return taskMapper.modelToDto(task);
    }

}
