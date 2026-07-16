export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  email: string;
  roles: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  department?: string;
  active: boolean;
  emailVerified: boolean;
  mfaEnabled: boolean;
  lastLoginAt?: string;
  avatarUrl?: string;
  createdAt?: string;
}

export interface Workflow {
  id: string;
  name: string;
  description?: string;
  version: number;
  status: string;
  steps: WorkflowStep[];
  metadata?: Record<string, unknown>;
  tags?: string[];
  createdBy: string;
  category?: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowStep {
  stepId: string;
  name: string;
  description?: string;
  type: string;
  order: number;
  config?: Record<string, unknown>;
  dependsOn?: string[];
  nextOnSuccess?: string;
  nextOnFailure?: string;
  timeoutSeconds?: number;
  mandatory?: boolean;
  retryConfig?: RetryConfig;
  loopConfig?: LoopConfig;
  errorStepId?: string;
}

export interface RetryConfig {
  maxAttempts: number;
  delaySeconds: number;
  backoffMultiplier?: number;
}

export interface LoopConfig {
  type: string;
  collectionExpression?: string;
  conditionExpression?: string;
  maxIterations?: number;
  loopVariable?: string;
}

export interface WorkflowExecution {
  id: string;
  workflowId: string;
  workflowName: string;
  workflowVersion: number;
  status: string;
  currentStepId?: string;
  startedAt?: string;
  completedAt?: string;
  triggeredBy: string;
  triggerType: string;
  errorMessage?: string;
  retryCount: number;
  inputParams?: Record<string, unknown>;
  outputResults?: Record<string, unknown>;
  totalDurationMs?: number;
  stepStates?: Record<string, string>;
  executionLog?: ExecutionLogEntry[];
}

export interface ExecutionLogEntry {
  stepId: string;
  stepName: string;
  status: string;
  message?: string;
  timestamp: string;
  durationMs?: number;
}

export interface Request {
  id: string;
  requestTypeId: string;
  requestTypeName: string;
  title: string;
  description?: string;
  status: string;
  submittedBy: string;
  submittedByName: string;
  submittedAt: string;
  completedAt?: string;
  dueDate?: string;
  priority: string;
  fields?: Record<string, unknown>;
  attachments?: FileAttachment[];
  comments?: CommentEntry[];
  approvalHistory?: ApprovalEntry[];
  currentApprover?: string;
  workflowExecutionId?: string;
  workflowId?: string;
  workflowName?: string;
  assignedTo?: string;
  assignedToName?: string;
  escalated?: boolean;
}

export interface RequestType {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  category?: string;
  workflowId?: string;
  workflowName?: string;
  active: boolean;
  fields: FieldDefinition[];
}

export interface FieldDefinition {
  name: string;
  label: string;
  type: string;
  required: boolean;
  defaultValue?: unknown;
  options?: { label: string; value: string }[];
  validation?: Record<string, unknown>;
}

export interface FileAttachment {
  fileName: string;
  fileSize: number;
  mimeType: string;
  documentId?: string;
  uploadedAt: string;
}

export interface CommentEntry {
  id: string;
  text: string;
  author: string;
  authorName: string;
  createdAt: string;
}

export interface ApprovalEntry {
  approver: string;
  approverName: string;
  action: string;
  comment?: string;
  timestamp: string;
}

export interface Document {
  id: string;
  fileName: string;
  originalName: string;
  mimeType: string;
  fileSize: number;
  documentType: string;
  processingStatus: string;
  uploadedBy: string;
  uploadedAt: string;
  ocrText?: string;
  summary?: string;
  tags?: string[];
  category?: string;
  notes?: string;
  archived: boolean;
  version: number;
  pageCount?: number;
  requestId?: string;
}

export interface DocumentStatistics {
  totalDocuments: number;
  totalSize: number;
  byType: Record<string, number>;
  byStatus: Record<string, number>;
  recentUploads: number;
  archivedCount: number;
}

export interface Task {
  id: string;
  workflowId: string;
  executionId: string;
  workflowStepId: string;
  name: string;
  description?: string;
  assignee: string;
  status: string;
  priority: string;
  dueDate?: string;
  completedAt?: string;
  result?: string;
  comments?: string;
}

export interface Trigger {
  id: string;
  name: string;
  description?: string;
  type: string;
  workflowId: string;
  workflowName: string;
  config?: Record<string, unknown>;
  active: boolean;
  lastTriggeredAt?: string;
  triggerCount: number;
}

export interface FraudCheck {
  id: string;
  requestId: string;
  requestType: string;
  userId: string;
  userName: string;
  department: string;
  claimAmount: number;
  category: string;
  vendor?: string;
  overallRiskScore: number;
  riskLevel: string;
  status: string;
  explanation?: string;
  aiRecommendation?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewNotes?: string;
  escalated: boolean;
  checkedAt: string;
}

export interface FraudAlert {
  id: string;
  fraudCheckId: string;
  requestId: string;
  userId: string;
  userName: string;
  claimAmount: number;
  riskLevel: string;
  flaggedCategories: string[];
  summary: string;
  acknowledged: boolean;
  resolved: boolean;
  assignedTo?: string;
  alertedAt: string;
}

export interface FraudStatistics {
  totalChecks: number;
  highRiskCount: number;
  criticalRiskCount: number;
  confirmedFraud: number;
  avgRiskScore: number;
  byCategory: Record<string, number>;
  byDepartment: Record<string, number>;
  trend: { date: string; count: number }[];
}

export interface AuditLog {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  performedBy: string;
  previousValues?: Record<string, unknown>;
  newValues?: Record<string, unknown>;
  details?: string;
  ipAddress: string;
  userAgent?: string;
  correlationId?: string;
  sessionId?: string;
  requestId?: string;
  workflowId?: string;
  executionId?: string;
  endpoint?: string;
  httpMethod?: string;
  organizationId?: string;
  success: boolean;
  failureReason?: string;
  durationMs?: number;
  timestamp: string;
}

export interface Notification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: string;
  read: boolean;
  referenceId?: string;
  referenceType?: string;
  createdAt: string;
}

export interface Organization {
  id: string;
  name: string;
  legalName?: string;
  registrationNumber?: string;
  taxId?: string;
  email?: string;
  phone?: string;
  website?: string;
  description?: string;
  industry?: string;
  employeeCount?: number;
  active: boolean;
  createdBy: string;
}

export interface Department {
  id: string;
  organizationId: string;
  name: string;
  code?: string;
  description?: string;
  headEmployeeId?: string;
  parentDepartmentId?: string;
  active: boolean;
}

export interface EmployeeProfile {
  id: string;
  userId: string;
  organizationId: string;
  employeeId: string;
  departmentId: string;
  departmentName: string;
  designationTitle: string;
  reportingManagerId?: string;
  status: string;
  employmentType: string;
  joiningDate: string;
  workEmail: string;
  workPhone?: string;
  firstName: string;
  lastName: string;
  profileImageUrl?: string;
  skills?: string[];
}

export interface UserPreference {
  theme?: string;
  language?: string;
  timezone?: string;
  notifications?: Record<string, boolean>;
}

export interface SchedulerJob {
  jobName: string;
  jobGroup: string;
  status: string;
  cronExpression?: string;
  lastExecutedAt?: string;
  nextExecutionAt?: string;
  totalExecutions: number;
  successCount: number;
  failureCount: number;
}

export interface BatchJob {
  jobName: string;
  status: string;
  lastExecution?: string;
  lastExitCode?: string;
  executionCount: number;
}

export interface WorkflowTemplate {
  id: string;
  name: string;
  description?: string;
  category?: string;
  steps: WorkflowStep[];
  tags?: string[];
  published: boolean;
  createdAt: string;
}

export interface AIQueryRequest {
  query: string;
  context?: Record<string, unknown>;
  requestId?: string;
  workflowId?: string;
}

export interface AIQueryResponse {
  response: string;
  confidence?: number;
  sources?: string[];
  modelUsed?: string;
  executionTimeMs?: number;
}

export interface ComplianceReport {
  id: string;
  reportType: string;
  status: string;
  generatedAt: string;
  generatedBy: string;
  dateRange?: { from: string; to: string };
  resultSummary?: string;
}

export interface NotificationPreference {
  email: boolean;
  sms: boolean;
  push: boolean;
  inApp: boolean;
  workflowUpdates: boolean;
  requestUpdates: boolean;
  approvalReminders: boolean;
  fraudAlerts: boolean;
  systemAlerts: boolean;
  digestFrequency?: string;
}
