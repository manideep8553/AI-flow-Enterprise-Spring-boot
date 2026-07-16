import { createBrowserRouter } from "react-router-dom";
import { AppLayout } from "../components/layout/AppLayout";
import { AuthLayout } from "../components/layout/AuthLayout";
import { LoginPage } from "../pages/auth/LoginPage";
import { RegisterPage } from "../pages/auth/RegisterPage";
import { ForgotPasswordPage } from "../pages/auth/ForgotPasswordPage";
import { ResetPasswordPage } from "../pages/auth/ResetPasswordPage";
import { DashboardPage } from "../pages/dashboard/DashboardPage";
import { WorkflowListPage } from "../pages/workflows/WorkflowListPage";
import { WorkflowBuilderPage } from "../pages/workflows/WorkflowBuilderPage";
import { WorkflowDetailPage } from "../pages/workflows/WorkflowDetailPage";
import { RequestListPage } from "../pages/requests/RequestListPage";
import { RequestDetailPage } from "../pages/requests/RequestDetailPage";
import { CreateRequestPage } from "../pages/requests/CreateRequestPage";
import { DocumentListPage } from "../pages/documents/DocumentListPage";
import { DocumentUploadPage } from "../pages/documents/DocumentUploadPage";
import { AICopilotPage } from "../pages/ai/AICopilotPage";
import { NotificationsPage } from "../pages/notifications/NotificationsPage";
import { AuditLogsPage } from "../pages/audit/AuditLogsPage";
import { FraudDetectionPage } from "../pages/fraud/FraudDetectionPage";
import { AnalyticsPage } from "../pages/analytics/AnalyticsPage";
import { UserManagementPage } from "../pages/admin/UserManagementPage";
import { OrganizationsPage } from "../pages/admin/OrganizationsPage";
import { SchedulerPage } from "../pages/scheduler/SchedulerPage";
import { ProfilePage } from "../pages/profile/ProfilePage";
import { SettingsPage } from "../pages/settings/SettingsPage";
import { CompliancePage } from "../pages/compliance/CompliancePage";
import { ReportsPage } from "../pages/reports/ReportsPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: "workflows", element: <WorkflowListPage /> },
      { path: "workflows/new", element: <WorkflowBuilderPage /> },
      { path: "workflows/:id", element: <WorkflowDetailPage /> },
      { path: "workflows/:id/edit", element: <WorkflowBuilderPage /> },
      { path: "requests", element: <RequestListPage /> },
      { path: "requests/new", element: <CreateRequestPage /> },
      { path: "requests/:id", element: <RequestDetailPage /> },
      { path: "documents", element: <DocumentListPage /> },
      { path: "documents/upload", element: <DocumentUploadPage /> },
      { path: "ai/copilot", element: <AICopilotPage /> },
      { path: "notifications", element: <NotificationsPage /> },
      { path: "audit-logs", element: <AuditLogsPage /> },
      { path: "fraud", element: <FraudDetectionPage /> },
      { path: "analytics", element: <AnalyticsPage /> },
      { path: "users", element: <UserManagementPage /> },
      { path: "organizations", element: <OrganizationsPage /> },
      { path: "scheduler", element: <SchedulerPage /> },
      { path: "profile", element: <ProfilePage /> },
      { path: "settings", element: <SettingsPage /> },
      { path: "compliance", element: <CompliancePage /> },
      { path: "reports", element: <ReportsPage /> },
    ],
  },
  {
    path: "/",
    element: <AuthLayout />,
    children: [
      { path: "login", element: <LoginPage /> },
      { path: "register", element: <RegisterPage /> },
      { path: "forgot-password", element: <ForgotPasswordPage /> },
      { path: "reset-password", element: <ResetPasswordPage /> },
    ],
  },
]);
