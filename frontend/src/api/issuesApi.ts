import type { CreateIssueRequest, Issue } from '../types/issue';

export class IssuesApi {
  async findIssues(priority?: string): Promise<Issue[]> {
    const query = new URLSearchParams();
    if (priority) {
      query.set('priority', priority);
    }

    const queryString = query.toString();
    const url = `/api/v1/issues${queryString ? `?${queryString}` : ''}`;
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Could not load issues (HTTP ${response.status})`);
    }

    return response.json() as Promise<Issue[]>;
  }

  async createIssue(request: CreateIssueRequest): Promise<Issue> {
    const response = await fetch('/api/v1/issues', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = (await response.json()) as { message?: string };
      throw new Error(error.message ?? `Could not create issue (HTTP ${response.status})`);
    }

    return response.json() as Promise<Issue>;
  }
}
