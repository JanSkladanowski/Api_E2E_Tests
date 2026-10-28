package com.example.issuehub.issue;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IssueService {

    private final AtomicLong idSequence = new AtomicLong();
    private final Map<Long, Issue> issues = new ConcurrentHashMap<>();

    public Issue create(CreateIssueRequest request) {
        Instant now = Instant.now();
        long id = idSequence.incrementAndGet();
        Issue issue = new Issue(id, request.title(), request.description(), request.priority(), IssueStatus.OPEN, now, now);
        issues.put(id, issue);
        return issue;
    }

    public List<Issue> findAll(IssueStatus status, IssuePriority priority) {
        return issues.values().stream()
                .filter(issue -> status == null || issue.status() == status)
                .filter(issue -> priority == null || issue.priority() == priority)
                .sorted(Comparator.comparing(Issue::createdAt).reversed())
                .toList();
    }

    public Issue findByExactTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title must not be blank");
        }

        List<Issue> matches = issues.values().stream()
                .filter(issue -> title.equals(issue.title()))
                .limit(2)
                .toList();
        if (matches.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found");
        }
        if (matches.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Issue title is not unique");
        }
        return matches.get(0);
    }

    public Issue findById(long id) {
        Issue issue = issues.get(id);
        if (issue == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found");
        }
        return issue;
    }

    public Issue changeStatus(long id, IssueStatus newStatus) {
        Issue current = findById(id);
        if (!isAllowedTransition(current.status(), newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Status cannot be changed from " + current.status() + " to " + newStatus);
        }

        Issue updated = new Issue(
                current.id(),
                current.title(),
                current.description(),
                current.priority(),
                newStatus,
                current.createdAt(),
                Instant.now()
        );
        issues.put(id, updated);
        return updated;
    }

    public void delete(long id) {
        if (issues.remove(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found");
        }
    }

    private boolean isAllowedTransition(IssueStatus current, IssueStatus requested) {
        return switch (current) {
            case OPEN -> requested == IssueStatus.IN_PROGRESS;
            case IN_PROGRESS -> requested == IssueStatus.RESOLVED;
            case RESOLVED -> requested == IssueStatus.CLOSED;
            case CLOSED -> false;
        };
    }
}
