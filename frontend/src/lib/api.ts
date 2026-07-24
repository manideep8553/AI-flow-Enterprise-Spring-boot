const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

function getAccessToken(): string | null {
  return localStorage.getItem("accessToken");
}

async function refreshTokens(): Promise<boolean> {
  const refreshToken = localStorage.getItem("refreshToken");
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("refreshToken", data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getAccessToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (res.status === 401 && !endpoint.includes("/auth/")) {
    const refreshed = await refreshTokens();
    if (refreshed) {
      const newToken = getAccessToken();
      headers["Authorization"] = `Bearer ${newToken}`;
      res = await fetch(`${API_BASE}${endpoint}`, {
        ...options,
        headers,
      });
    }
  }

  if (!res.ok) {
    const errBody = await res.text();
    let errMsg: string;
    try {
      const parsed = JSON.parse(errBody);
      errMsg = parsed.message || parsed.error || errBody;
    } catch {
      errMsg = errBody || `HTTP ${res.status}`;
    }
    throw new Error(errMsg);
  }

  if (res.status === 204) return undefined as T;

  return res.json();
}

export function buildQueryString(params: Record<string, string | number | boolean | undefined | null>): string {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== null && v !== ""
  );
  if (entries.length === 0) return "";
  return "?" + entries.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`).join("&");
}

export const api = {
  // Auth
  login: (data: { email: string; password: string }) =>
    apiRequest<{ accessToken: string; refreshToken: string; tokenType: string; expiresIn: number; userId: string; username: string; email: string; roles: string[] }>("/auth/login", { method: "POST", body: JSON.stringify(data) }),

  register: (data: { username: string; email: string; password: string; firstName: string; lastName: string; role?: string }) =>
    apiRequest<void>("/auth/register", { method: "POST", body: JSON.stringify(data) }),

  refreshToken: (refreshToken: string) =>
    apiRequest<{ accessToken: string; refreshToken: string }>("/auth/refresh", { method: "POST", body: JSON.stringify({ refreshToken }) }),

  logout: () => apiRequest<void>("/auth/logout", { method: "POST" }),

  validateToken: () => apiRequest<{ valid: boolean; userId?: string }>("/auth/validate", { method: "POST" }),

  forgotPassword: (email: string) =>
    apiRequest<void>("/auth/password/forgot", { method: "POST", body: JSON.stringify({ email }) }),

  resetPassword: (token: string, newPassword: string) =>
    apiRequest<void>("/auth/password/reset", { method: "POST", body: JSON.stringify({ token, newPassword }) }),

  changePassword: (currentPassword: string, newPassword: string) =>
    apiRequest<void>("/auth/password/change", { method: "POST", body: JSON.stringify({ currentPassword, newPassword }) }),

  verifyEmail: (token: string) =>
    apiRequest<void>(`/auth/email/verify?token=${token}`, { method: "GET" }),

  resendVerification: (email: string) =>
    apiRequest<void>("/auth/email/resend-verification", { method: "POST", body: JSON.stringify({ email }) }),

  getSessions: () => apiRequest<{ id: string; deviceName: string; lastActivityAt: string; ipAddress: string }[]>("/auth/sessions"),

  revokeSession: (sessionId: string) =>
    apiRequest<void>(`/auth/sessions/${sessionId}`, { method: "DELETE" }),

  revokeAllSessions: () =>
    apiRequest<void>("/auth/sessions", { method: "DELETE" }),

  // Users
  getUsers: (params?: { page?: number; size?: number; role?: string; department?: string; active?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/users" + buildQueryString(params || {})),

  getUser: (id: string) => apiRequest<any>(`/users/${id}`),

  getUserByUsername: (username: string) => apiRequest<any>(`/users/by-username/${username}`),

  createUser: (data: any) => apiRequest<any>("/users", { method: "POST", body: JSON.stringify(data) }),

  updateUser: (id: string, data: any) => apiRequest<any>(`/users/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  deactivateUser: (id: string) => apiRequest<void>(`/users/${id}/deactivate`, { method: "PATCH" }),

  activateUser: (id: string) => apiRequest<void>(`/users/${id}/activate`, { method: "PATCH" }),

  // Workflows
  getWorkflows: (params?: { page?: number; size?: number; status?: string; search?: string; tag?: string; category?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/workflows" + buildQueryString(params || {})),

  getWorkflow: (id: string) => apiRequest<any>(`/workflows/${id}`),

  createWorkflow: (data: any, createdBy?: string) =>
    apiRequest<any>("/workflows" + (createdBy ? `?createdBy=${createdBy}` : ""), { method: "POST", body: JSON.stringify(data) }),

  updateWorkflow: (id: string, data: any) => apiRequest<any>(`/workflows/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  deleteWorkflow: (id: string) => apiRequest<void>(`/workflows/${id}`, { method: "DELETE" }),

  publishWorkflow: (id: string) => apiRequest<void>(`/workflows/${id}/publish`, { method: "POST" }),

  archiveWorkflow: (id: string) => apiRequest<void>(`/workflows/${id}/archive`, { method: "POST" }),

  executeWorkflow: (id: string, data: { triggeredBy?: string; inputParams?: Record<string, unknown> }) =>
    apiRequest<any>(`/workflows/${id}/execute`, { method: "POST", body: JSON.stringify(data) }),

  validateWorkflow: (id: string) => apiRequest<{ valid: boolean; errors: string[] }>(`/workflows/${id}/validate`, { method: "POST" }),

  rollbackWorkflow: (id: string, version: number) =>
    apiRequest<void>(`/workflows/${id}/rollback/${version}`, { method: "POST" }),

  exportWorkflow: (id: string) => apiRequest<any>(`/workflows/${id}/export`),

  importWorkflow: (data: any, createdBy?: string) =>
    apiRequest<any>("/workflows/import" + (createdBy ? `?createdBy=${createdBy}` : ""), { method: "POST", body: JSON.stringify(data) }),

  getWorkflowVersions: (workflowId: string) =>
    apiRequest<any[]>(`/workflows/versions/${workflowId}`),

  // Executions
  getExecutions: (params?: { page?: number; size?: number; workflowId?: string; status?: string; triggeredBy?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/executions" + buildQueryString(params || {})),

  getExecution: (id: string) => apiRequest<any>(`/executions/${id}`),

  startExecution: (id: string) => apiRequest<void>(`/executions/${id}/start`, { method: "POST" }),

  cancelExecution: (id: string) => apiRequest<void>(`/executions/${id}/cancel`, { method: "POST" }),

  suspendExecution: (id: string) => apiRequest<void>(`/executions/${id}/suspend`, { method: "POST" }),

  resumeExecution: (id: string) => apiRequest<void>(`/executions/${id}/resume`, { method: "POST" }),

  retryExecution: (id: string) => apiRequest<void>(`/executions/${id}/retry`, { method: "POST" }),

  retryExecutionStep: (id: string, stepId: string) =>
    apiRequest<void>(`/executions/${id}/retry-step/${stepId}`, { method: "POST" }),

  // Tasks
  getTasks: (params?: { page?: number; size?: number; workflowId?: string; executionId?: string; assignee?: string; status?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/tasks" + buildQueryString(params || {})),

  getTask: (id: string) => apiRequest<any>(`/tasks/${id}`),

  createTask: (data: any) => apiRequest<any>("/tasks", { method: "POST", body: JSON.stringify(data) }),

  updateTask: (id: string, data: any) => apiRequest<any>(`/tasks/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  updateTaskStatus: (id: string, status: string) =>
    apiRequest<void>(`/tasks/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),

  assignTask: (id: string, assignee: string) =>
    apiRequest<void>(`/tasks/${id}/assign`, { method: "PATCH", body: JSON.stringify({ assignee }) }),

  // Requests
  getRequests: (params?: { page?: number; size?: number; requestTypeId?: string; status?: string; submittedBy?: string; assignedTo?: string; escalated?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/requests" + buildQueryString(params || {})),

  getMyRequests: (params?: { page?: number; size?: number; status?: string; requestTypeId?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/requests/my" + buildQueryString(params || {})),

  getApprovals: (params?: { page?: number; size?: number; status?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/requests/approvals" + buildQueryString(params || {})),

  getRequest: (id: string) => apiRequest<any>(`/requests/${id}`),

  createRequest: (data: any) => apiRequest<any>("/requests", { method: "POST", body: JSON.stringify(data) }),

  updateRequest: (id: string, data: any) => apiRequest<any>(`/requests/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  submitRequest: (id: string) => apiRequest<void>(`/requests/${id}/submit`, { method: "POST" }),

  approveRequest: (id: string, comment?: string) =>
    apiRequest<void>(`/requests/${id}/approve`, { method: "POST", body: JSON.stringify({ comment }) }),

  rejectRequest: (id: string, comment?: string) =>
    apiRequest<void>(`/requests/${id}/reject`, { method: "POST", body: JSON.stringify({ comment }) }),

  cancelRequest: (id: string) => apiRequest<void>(`/requests/${id}/cancel`, { method: "POST" }),

  addComment: (id: string, text: string) =>
    apiRequest<void>(`/requests/${id}/comments`, { method: "POST", body: JSON.stringify({ text }) }),

  updateRequestFields: (id: string, fields: Record<string, unknown>) =>
    apiRequest<void>(`/requests/${id}/fields`, { method: "PUT", body: JSON.stringify(fields) }),

  assignRequest: (id: string, assignedTo: string) =>
    apiRequest<void>(`/requests/${id}/assign`, { method: "POST", body: JSON.stringify({ assignedTo }) }),

  escalateRequest: (id: string, reason: string) =>
    apiRequest<void>(`/requests/${id}/escalate`, { method: "POST", body: JSON.stringify({ reason }) }),

  deleteRequest: (id: string) => apiRequest<void>(`/requests/${id}`, { method: "DELETE" }),

  // Request Types
  getRequestTypes: (params?: { page?: number; size?: number; category?: string; active?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/request-types" + buildQueryString(params || {})),

  getActiveRequestTypes: () => apiRequest<any[]>("/request-types/active"),

  getRequestType: (id: string) => apiRequest<any>(`/request-types/${id}`),

  createRequestType: (data: any) => apiRequest<any>("/request-types", { method: "POST", body: JSON.stringify(data) }),

  updateRequestType: (id: string, data: any) => apiRequest<any>(`/request-types/${id}`, { method: "PUT", body: JSON.stringify(data) }),

  deleteRequestType: (id: string) => apiRequest<void>(`/request-types/${id}`, { method: "DELETE" }),

  toggleRequestType: (id: string) => apiRequest<void>(`/request-types/${id}/toggle-active`, { method: "POST" }),

  // Documents
  getDocuments: (params?: { page?: number; size?: number; documentType?: string; processingStatus?: string; uploadedBy?: string; search?: string; tag?: string; category?: string; archived?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/documents" + buildQueryString(params || {})),

  getDocument: (id: string) => apiRequest<any>(`/documents/${id}`),

  downloadDocument: (id: string) => `${API_BASE}/documents/${id}/download`,

  getDocumentDownloadUrl: (id: string, expirySeconds?: number) =>
    apiRequest<{ url: string }>(`/documents/${id}/download-url${expirySeconds ? `?expirySeconds=${expirySeconds}` : ""}`),

  getDocumentPreview: (id: string) => apiRequest<{ url: string }>(`/documents/${id}/preview`),

  getDocumentVersions: (id: string) => apiRequest<any[]>(`/documents/${id}/versions`),

  restoreDocumentVersion: (id: string, versionNumber: number) =>
    apiRequest<void>(`/documents/${id}/versions/${versionNumber}/restore`, { method: "POST" }),

  updateDocument: (id: string, formData: FormData) => apiRequest<any>(`/documents/${id}`, { method: "PUT", body: formData }),

  deleteDocument: (id: string) => apiRequest<void>(`/documents/${id}`, { method: "DELETE" }),

  archiveDocument: (id: string) => apiRequest<void>(`/documents/${id}/archive`, { method: "POST" }),

  restoreDocument: (id: string) => apiRequest<void>(`/documents/${id}/restore`, { method: "POST" }),

  getDocumentStatistics: () => apiRequest<any>("/documents/statistics"),

  uploadDocument: (formData: FormData) => apiRequest<any>("/documents/upload", { method: "POST", body: formData }),

  uploadProfileImage: (formData: FormData) => apiRequest<{ url: string }>("/upload/profile-image", { method: "POST", body: formData }),

  // Fraud
  analyzeFraud: (data: { requestId?: string; userId?: string; claimAmount: number; category: string; vendor?: string; description?: string }) =>
    apiRequest<any>("/fraud/analyze", { method: "POST", body: JSON.stringify(data) }),

  getFraudChecks: (params?: { page?: number; size?: number; riskLevel?: string; status?: string; userId?: string; department?: string; escalated?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/fraud/checks" + buildQueryString(params || {})),

  getFraudCheck: (id: string) => apiRequest<any>(`/fraud/checks/${id}`),

  reviewFraud: (id: string, data: { status: string; reviewNotes?: string }) =>
    apiRequest<void>(`/fraud/checks/${id}/review`, { method: "POST", body: JSON.stringify(data) }),

  escalateFraud: (id: string) => apiRequest<void>(`/fraud/checks/${id}/escalate`, { method: "POST" }),

  getFraudAlerts: (params?: { page?: number; size?: number; riskLevel?: string; resolved?: boolean; acknowledged?: boolean; userId?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/fraud/alerts" + buildQueryString(params || {})),

  acknowledgeFraudAlert: (id: string) => apiRequest<void>(`/fraud/alerts/${id}/acknowledge`, { method: "POST" }),

  resolveFraudAlert: (id: string) => apiRequest<void>(`/fraud/alerts/${id}/resolve`, { method: "POST" }),

  getFraudStatistics: (params?: { department?: string; timeframe?: string }) =>
    apiRequest<any>("/fraud/statistics" + buildQueryString(params || {})),

  getFraudRules: () => apiRequest<any[]>("/fraud/rules"),
  createFraudRule: (data: Record<string, unknown>) => apiRequest<any>("/fraud/rules", { method: "POST", body: JSON.stringify(data) }),
  updateFraudRule: (id: string, data: Record<string, unknown>) => apiRequest<any>(`/fraud/rules/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteFraudRule: (id: string) => apiRequest<void>(`/fraud/rules/${id}`, { method: "DELETE" }),

  // AI
  aiQuery: (data: { query: string; context?: Record<string, unknown>; requestId?: string; workflowId?: string }) =>
    apiRequest<{ response: string; confidence?: number; sources?: string[] }>("/ai/query", { method: "POST", body: JSON.stringify(data) }),

  getApprovalRecommendation: (requestId: string) =>
    apiRequest<{ recommendation: string; confidence: number; reasoning: string }>(`/ai/recommendations/approval/${requestId}`),

  getRequestSummary: (requestId: string) =>
    apiRequest<{ summary: string; keyPoints: string[] }>(`/ai/summaries/request/${requestId}`),

  getDecisionExplanation: (executionId: string, stepId?: string) =>
    apiRequest<{ explanation: string; factors: string[] }>(`/ai/explain/${executionId}${stepId ? `?stepId=${stepId}` : ""}`),

  predictOutcome: (requestId: string) =>
    apiRequest<{ prediction: string; probability: number; factors: string[] }>(`/ai/predict/${requestId}`),

  recommendApprover: (requestId: string) =>
    apiRequest<{ recommendedApprover: string; reason: string }>(`/ai/recommendations/approver/${requestId}`),

  optimizeWorkflow: (workflowId: string) =>
    apiRequest<{ suggestions: string[]; expectedImprovement: string }>(`/ai/optimize/${workflowId}`),

  detectBottlenecks: () =>
    apiRequest<{ bottlenecks: { workflowId: string; workflowName: string; stepId: string; avgDuration: number; severity: string }[] }>("/ai/bottlenecks"),

  getWorkflowInsights: (workflowId: string) =>
    apiRequest<{ insights: string[]; metrics: Record<string, number> }>(`/ai/insights/${workflowId}`),

  analyzeTrends: (params?: { timeframe?: string; metric?: string }) =>
    apiRequest<{ trends: { period: string; value: number; change: number }[] }>("/ai/trends" + buildQueryString(params || {})),

  compareWorkflows: (workflowIds: string[]) =>
    apiRequest<{ comparison: any }>("/ai/compare", { method: "POST", body: JSON.stringify(workflowIds) }),

  generateAIReport: (workflowId: string, reportType?: string) =>
    apiRequest<{ report: string; summary: string }>(`/ai/report/${workflowId}${reportType ? `?reportType=${reportType}` : ""}`),

  submitAIFeedback: (data: { decisionLogId: string; feedbackPositive: boolean; feedback?: string }) =>
    apiRequest<void>("/ai/feedback", { method: "POST", body: JSON.stringify(data) }),

  // Audit Logs
  getAuditLogs: (params?: { page?: number; size?: number; entityType?: string; entityId?: string; performedBy?: string; action?: string; from?: string; to?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/audit-logs" + buildQueryString(params || {})),

  getAuditLog: (id: string) => apiRequest<any>(`/audit-logs/${id}`),

  searchAuditLogs: (filter: any) =>
    apiRequest<{ content: any[]; totalElements: number }>("/audit-logs/search", { method: "POST", body: JSON.stringify(filter) }),

  getAuditLogsByEntity: (entityType: string, entityId: string) =>
    apiRequest<any[]>(`/audit-logs/entity/${entityType}/${entityId}`),

  getAuditLogsByWorkflow: (workflowId: string) => apiRequest<any[]>(`/audit-logs/workflow/${workflowId}`),

  getAuditLogsByExecution: (executionId: string) => apiRequest<any[]>(`/audit-logs/execution/${executionId}`),

  getAuditLogSummary: (params?: { from?: string; to?: string }) =>
    apiRequest<{ total: number; byAction: Record<string, number> }>("/audit-logs/summary" + buildQueryString(params || {})),

  getAuditLogStats: (params?: { from?: string; to?: string }) =>
    apiRequest<{ dailyCounts: { date: string; count: number }[]; topUsers: { user: string; count: number }[] }>("/audit-logs/stats" + buildQueryString(params || {})),

  exportAuditLogs: (params: { format: string; from?: string; to?: string }) =>
    apiRequest<{ downloadUrl: string }>("/audit-logs/export", { method: "POST", body: JSON.stringify(params) }),

  // Compliance
  getComplianceReports: (params?: { page?: number; size?: number; reportType?: string; status?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/compliance/reports" + buildQueryString(params || {})),

  getComplianceReport: (id: string) => apiRequest<any>(`/compliance/reports/${id}`),

  generateComplianceReport: (reportType: string, from?: string, to?: string) =>
    apiRequest<any>("/compliance/reports/generate" + buildQueryString({ reportType, from, to }), { method: "POST" }),

  generateUserActivityReport: (from?: string, to?: string) =>
    apiRequest<any>("/compliance/reports/user-activity" + buildQueryString({ from, to }), { method: "POST" }),

  generateSecurityAuditReport: (from?: string, to?: string) =>
    apiRequest<any>("/compliance/reports/security-audit" + buildQueryString({ from, to }), { method: "POST" }),

  generateWorkflowAuditReport: (from?: string, to?: string) =>
    apiRequest<any>("/compliance/reports/workflow-audit" + buildQueryString({ from, to }), { method: "POST" }),

  generateDataAccessReport: (from?: string, to?: string) =>
    apiRequest<any>("/compliance/reports/data-access" + buildQueryString({ from, to }), { method: "POST" }),

  // Triggers
  getTriggers: (params?: { page?: number; size?: number; workflowId?: string; type?: string; active?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/triggers" + buildQueryString(params || {})),

  getTrigger: (id: string) => apiRequest<any>(`/triggers/${id}`),
  createTrigger: (data: any) => apiRequest<any>("/triggers", { method: "POST", body: JSON.stringify(data) }),
  updateTrigger: (id: string, data: any) => apiRequest<any>(`/triggers/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteTrigger: (id: string) => apiRequest<void>(`/triggers/${id}`, { method: "DELETE" }),
  activateTrigger: (id: string) => apiRequest<void>(`/triggers/${id}/activate`, { method: "PATCH" }),
  deactivateTrigger: (id: string) => apiRequest<void>(`/triggers/${id}/deactivate`, { method: "PATCH" }),

  // Organizations
  getOrganizations: (params?: { page?: number; size?: number; search?: string; industry?: string; active?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/organizations" + buildQueryString(params || {})),

  getOrganization: (id: string) => apiRequest<any>(`/organizations/${id}`),
  createOrganization: (data: any, createdBy?: string) =>
    apiRequest<any>("/organizations" + (createdBy ? `?createdBy=${createdBy}` : ""), { method: "POST", body: JSON.stringify(data) }),
  updateOrganization: (id: string, data: any) => apiRequest<any>(`/organizations/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteOrganization: (id: string) => apiRequest<void>(`/organizations/${id}`, { method: "DELETE" }),
  toggleOrganization: (id: string) => apiRequest<void>(`/organizations/${id}/toggle-active`, { method: "PATCH" }),

  // Organization Settings
  getOrganizationSettings: (orgId: string) =>
    apiRequest<any>(`/organizations/${orgId}/settings`),
  updateOrganizationSettings: (orgId: string, data: any) =>
    apiRequest<any>(`/organizations/${orgId}/settings`, { method: "PUT", body: JSON.stringify(data) }),

  // Departments
  getDepartments: (orgId: string, params?: { page?: number; size?: number; search?: string; active?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>
      (`/organizations/${orgId}/departments` + buildQueryString(params || {})),
  getDepartment: (orgId: string, id: string) => apiRequest<any>(`/organizations/${orgId}/departments/${id}`),
  createDepartment: (orgId: string, data: any) => apiRequest<any>(`/organizations/${orgId}/departments`, { method: "POST", body: JSON.stringify(data) }),
  updateDepartment: (orgId: string, id: string, data: any) => apiRequest<any>(`/organizations/${orgId}/departments/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteDepartment: (orgId: string, id: string) => apiRequest<void>(`/organizations/${orgId}/departments/${id}`, { method: "DELETE" }),
  toggleDepartment: (orgId: string, id: string) => apiRequest<void>(`/organizations/${orgId}/departments/${id}/toggle-active`, { method: "PATCH" }),

  // Teams
  getTeams: (deptId: string, params?: { page?: number; size?: number; search?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>
      (`/departments/${deptId}/teams` + buildQueryString(params || {})),
  getTeam: (id: string) => apiRequest<any>(`/departments/teams/${id}`),
  createTeam: (deptId: string, data: any) => apiRequest<any>(`/departments/${deptId}/teams`, { method: "POST", body: JSON.stringify(data) }),
  updateTeam: (id: string, data: any) => apiRequest<any>(`/departments/teams/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteTeam: (id: string) => apiRequest<void>(`/departments/teams/${id}`, { method: "DELETE" }),

  // Business Units
  getBusinessUnits: (orgId: string, params?: { page?: number; size?: number; active?: boolean }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>
      (`/organizations/${orgId}/business-units` + buildQueryString(params || {})),
  getBusinessUnit: (id: string) => apiRequest<any>(`/organizations/business-units/${id}`),
  createBusinessUnit: (orgId: string, data: any) => apiRequest<any>(`/organizations/${orgId}/business-units`, { method: "POST", body: JSON.stringify(data) }),
  updateBusinessUnit: (id: string, data: any) => apiRequest<any>(`/organizations/business-units/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteBusinessUnit: (id: string) => apiRequest<void>(`/organizations/business-units/${id}`, { method: "DELETE" }),

  // Designations
  getDesignations: (orgId: string, params?: { page?: number; size?: number; search?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>
      (`/organizations/${orgId}/designations` + buildQueryString(params || {})),
  getDesignation: (id: string) => apiRequest<any>(`/organizations/designations/${id}`),
  createDesignation: (orgId: string, data: any) => apiRequest<any>(`/organizations/${orgId}/designations`, { method: "POST", body: JSON.stringify(data) }),
  updateDesignation: (id: string, data: any) => apiRequest<any>(`/organizations/designations/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteDesignation: (id: string) => apiRequest<void>(`/organizations/designations/${id}`, { method: "DELETE" }),

  // Employees
  getEmployees: (orgId: string, params?: { page?: number; size?: number; departmentId?: string; status?: string; search?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>
      (`/organizations/${orgId}/employees` + buildQueryString(params || {})),
  getEmployee: (orgId: string, id: string) => apiRequest<any>(`/organizations/${orgId}/employees/${id}`),
  getEmployeeByUserId: (userId: string) => apiRequest<any>(`/organizations/employees/by-user/${userId}`),
  createEmployee: (orgId: string, data: any) => apiRequest<any>(`/organizations/${orgId}/employees`, { method: "POST", body: JSON.stringify(data) }),
  updateEmployee: (orgId: string, id: string, data: any) => apiRequest<any>(`/organizations/${orgId}/employees/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  updateEmployeeStatus: (id: string, status: string) => apiRequest<any>(`/organizations/employees/${id}/status?status=${encodeURIComponent(status)}`, { method: "PATCH" }),
  deleteEmployee: (id: string) => apiRequest<void>(`/organizations/employees/${id}`, { method: "DELETE" }),

  // Notifications
  getNotifications: (params?: { page?: number; size?: number; read?: boolean; type?: string }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number; number: number; size: number }>("/notifications" + buildQueryString(params || {})),

  getNotification: (id: string) => apiRequest<any>(`/notifications/${id}`),
  markNotificationRead: (id: string) => apiRequest<void>(`/notifications/${id}/read`, { method: "POST" }),
  getUnreadCount: () => apiRequest<{ count: number }>("/notifications/unread-count"),
  getNotificationStats: () => apiRequest<{ total: number; unread: number; byType: Record<string, number> }>("/notifications/stats"),

  // Notification Preferences
  getNotificationPreferences: () => apiRequest<any>("/notification-preferences"),
  updateNotificationPreferences: (data: any) => apiRequest<any>("/notification-preferences", { method: "PUT", body: JSON.stringify(data) }),

  // Notification Templates
  getNotificationTemplates: () => apiRequest<any[]>("/notification-templates"),

  // User Preferences
  getUserPreferences: () => apiRequest<any>("/preferences"),
  updateUserPreferences: (data: any) => apiRequest<any>("/preferences", { method: "PUT", body: JSON.stringify(data) }),

  // Workflow Templates
  getWorkflowTemplates: () => apiRequest<any[]>("/workflow-templates"),
  getWorkflowTemplate: (id: string) => apiRequest<any>(`/workflow-templates/${id}`),
  useWorkflowTemplate: (id: string) => apiRequest<any>(`/workflow-templates/${id}/use`, { method: "POST" }),

  // Scheduler Admin
  getSchedulerJobs: () => apiRequest<any[]>("/admin/scheduler/jobs"),
  getBatchJobs: () => apiRequest<any[]>("/admin/scheduler/batch-jobs"),
  triggerSchedulerJob: (jobName: string) =>
    apiRequest<void>(`/admin/scheduler/jobs/${jobName}/trigger`, { method: "POST" }),
  launchBatchJob: (jobName: string) =>
    apiRequest<void>(`/admin/scheduler/batch-jobs/${jobName}/launch`, { method: "POST" }),
  getSchedulerExecutions: (params?: { page?: number; size?: number }) =>
    apiRequest<{ content: any[]; totalElements: number; totalPages: number }>("/admin/scheduler/executions" + buildQueryString(params || {})),
  getSchedulerExecution: (jobName: string) =>
    apiRequest<any[]>(`/admin/scheduler/executions/${jobName}`),
  getSchedulerSummary: () => apiRequest<any>("/admin/scheduler/summary"),
  getSchedulerLocks: () => apiRequest<any[]>("/admin/scheduler/locks"),
  deleteSchedulerLock: (lockKey: string) => apiRequest<void>(`/admin/scheduler/locks/${lockKey}`, { method: "DELETE" }),

  // Analytics
  getAnalyticsSummary: (params?: { period?: string; department?: string; workflowId?: string }) =>
    apiRequest<any>("/analytics/summary" + buildQueryString(params || {})),

  getWorkflowAnalytics: (period?: string) =>
    apiRequest<any>("/analytics/workflows" + buildQueryString({ period })),

  getWorkflowMetrics: (period?: string) =>
    apiRequest<any>("/analytics/workflows/metrics" + buildQueryString({ period })),

  getRequestAnalytics: (period?: string) =>
    apiRequest<any>("/analytics/requests" + buildQueryString({ period })),

  getUserAnalytics: (period?: string) =>
    apiRequest<any>("/analytics/users" + buildQueryString({ period })),

  getDocumentAnalytics: () =>
    apiRequest<any>("/analytics/documents"),

  getFraudAnalytics: (period?: string) =>
    apiRequest<any>("/analytics/fraud" + buildQueryString({ period })),

  getNotificationAnalytics: (period?: string) =>
    apiRequest<any>("/analytics/notifications" + buildQueryString({ period })),

  getAIAnalytics: (period?: string) =>
    apiRequest<any>("/analytics/ai" + buildQueryString({ period })),

  getSchedulerAnalytics: () =>
    apiRequest<any>("/analytics/scheduler"),

  getBottlenecks: () =>
    apiRequest<any[]>("/analytics/bottlenecks"),

  getDepartmentAnalytics: (period?: string) =>
    apiRequest<any[]>("/analytics/departments" + buildQueryString({ period })),

  getKPI: (period?: string) =>
    apiRequest<any>("/analytics/kpi" + buildQueryString({ period })),
};
