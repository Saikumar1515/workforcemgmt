package com.railse.hiring.workforcemgmt.controller;

import com.railse.hiring.workforcemgmt.common.response.Response;
import com.railse.hiring.workforcemgmt.common.response.SimpleResponseStatus;
import com.railse.hiring.workforcemgmt.dto.AddCommentRequest;
import com.railse.hiring.workforcemgmt.dto.TaskFetchByDateRequest;
import com.railse.hiring.workforcemgmt.dto.TaskManagementDto;
import com.railse.hiring.workforcemgmt.dto.UpdatePriorityRequest;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task-mgmt")
public class TaskController {

    private final TaskManagementService taskService;

    public TaskController(TaskManagementService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/smart-daily-view")
    public Response<List<TaskManagementDto>> smartDailyView(@RequestBody TaskFetchByDateRequest request) {
        return new Response<>(taskService.smartDailyView(request), null,
                new SimpleResponseStatus(200, "Success"));

    }

    @PostMapping("/update-priority")
    public Response<TaskManagementDto> updatePriority(@RequestBody UpdatePriorityRequest request) {
        return new Response<>(taskService.updatePriority(request), null,
                new SimpleResponseStatus(200, "Success") {});
    }

    @GetMapping("/priority/{priority}")
    public Response<List<TaskManagementDto>> getTasksByPriority(@PathVariable Priority priority) {
        return new Response<>(taskService.getTasksByPriority(priority), null,
                new SimpleResponseStatus(200, "Success"));
    }

    @PostMapping("/add-comment")
    public Response<TaskManagementDto> addComment(@RequestBody AddCommentRequest request) {
        return new Response<>(taskService.addComment(request), null,
                new SimpleResponseStatus(200, "Success") {});
    }
}
