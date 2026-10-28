import { IssuesApi } from './api/issuesApi';
import type { Issue } from './types/issue';
import './styles.css';

const api = new IssuesApi();
const form = getElement<HTMLFormElement>('[data-testid="create-issue-form"]');
const message = getElement<HTMLParagraphElement>('[data-testid="form-message"]');
const tableBody = getElement<HTMLTableSectionElement>('[data-testid="issues-table-body"]');
const priorityFilter = getElement<HTMLSelectElement>('[data-testid="priority-filter"]');
const issuesCount = getElement<HTMLSpanElement>('[data-testid="issues-count"]');

function getElement<T extends HTMLElement>(selector: string): T {
  const element = document.querySelector<T>(selector);
  if (!element) {
    throw new Error(`Required page element was not found: ${selector}`);
  }
  return element;
}

function showMessage(text: string, isError: boolean): void {
  message.hidden = false;
  message.textContent = text;
  message.dataset.type = isError ? 'error' : 'success';
}

function renderIssues(issues: Issue[]): void {
  tableBody.replaceChildren();
  issuesCount.textContent = String(issues.length);

  if (issues.length === 0) {
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 3;
    cell.className = 'empty-state';
    cell.dataset.testid = 'empty-state';
    cell.textContent = 'No issues match this view. Create one to get started.';
    row.append(cell);
    tableBody.append(row);
    return;
  }

  for (const issue of issues) {
    const row = document.createElement('tr');
    row.dataset.testid = 'issue-row';
    row.dataset.issueId = String(issue.id);

    const titleCell = document.createElement('td');
    titleCell.dataset.testid = 'issue-title-cell';
    titleCell.textContent = issue.title;

    const priorityCell = document.createElement('td');
    priorityCell.dataset.testid = 'issue-priority-cell';
    const priorityBadge = document.createElement('span');
    priorityBadge.className = `priority-badge priority-${issue.priority.toLowerCase()}`;
    priorityBadge.textContent = issue.priority;
    priorityCell.append(priorityBadge);

    const statusCell = document.createElement('td');
    const statusBadge = document.createElement('span');
    statusBadge.className = `status-badge status-${issue.status.toLowerCase()}`;
    statusBadge.textContent = issue.status;
    statusCell.append(statusBadge);

    row.append(titleCell, priorityCell, statusCell);
    tableBody.append(row);
  }
}

async function loadIssues(): Promise<void> {
  try {
    renderIssues(await api.findIssues(priorityFilter.value || undefined));
  } catch (error) {
    showMessage(error instanceof Error ? error.message : 'Could not load issues', true);
  }
}

priorityFilter.addEventListener('change', () => void loadIssues());

form.addEventListener('submit', async (event: SubmitEvent) => {
  event.preventDefault();
  const formData = new FormData(form);

  try {
    await api.createIssue({
      title: String(formData.get('title') ?? ''),
      description: String(formData.get('description') ?? ''),
      priority: String(formData.get('priority') ?? 'MEDIUM'),
    });
    form.reset();
    showMessage('Issue created', false);
    await loadIssues();
  } catch (error) {
    showMessage(error instanceof Error ? error.message : 'Could not create issue', true);
  }
});

void loadIssues();
