export interface Issue {
  id: number;
  title: string;
  description: string;
  priority: string;
  status: string;
}

export interface CreateIssueRequest {
  title: string;
  description: string;
  priority: string;
}
