package com.example.issuehub.issue;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Issue create(@Valid @RequestBody CreateIssueRequest request) {
        return issueService.create(request);
    }

    @GetMapping
    public List<Issue> findAll(
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssuePriority priority
    ) {
        return issueService.findAll(status, priority);
    }

    @GetMapping("/by-title")
    public Issue findByExactTitle(@RequestParam String title) {
        return issueService.findByExactTitle(title);
    }

    @GetMapping("/{id}")
    public Issue findById(@PathVariable long id) {
        return issueService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public Issue changeStatus(@PathVariable long id, @Valid @RequestBody UpdateStatusRequest request) {
        return issueService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        issueService.delete(id);
    }
}
