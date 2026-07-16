import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Workflow, CheckCircle2, Clock, AlertTriangle, TrendingUp, Users, FileText, Activity } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Progress } from "../../components/ui/progress";
import { api } from "../../lib/api";
import { formatRelativeTime, formatNumber } from "../../lib/utils";
import { useNavigate } from "react-router-dom";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.1 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

export function DashboardPage() {
  const navigate = useNavigate();
  const [stats, setStats] = useState({ workflows: 0, executions: 0, requests: 0, tasks: 0, activeUsers: 0, successRate: 0 });
  const [recentExecutions, setRecentExecutions] = useState<any[]>([]);
  const [pendingApprovals, setPendingApprovals] = useState<any[]>([]);

  useEffect(() => {
    Promise.all([
      api.getWorkflows({ page: 0, size: 1 }),
      api.getExecutions({ page: 0, size: 5 }),
      api.getRequests({ page: 0, size: 1 }),
      api.getTasks({ page: 0, size: 1 }),
      api.getApprovals({ page: 0, size: 5 }),
    ]).then(([wf, exec, req, tasks, approvals]) => {
      const totalExec = exec.totalElements;
      const completed = exec.content?.filter((e: any) => e.status === "COMPLETED").length || 0;
      setStats({
        workflows: wf.totalElements,
        executions: totalExec,
        requests: req.totalElements,
        tasks: tasks.totalElements,
        activeUsers: 0,
        successRate: totalExec > 0 ? Math.round((completed / totalExec) * 100) : 100,
      });
      setRecentExecutions(exec.content || []);
      setPendingApprovals(approvals.content || []);
    }).catch(() => {});
  }, []);

  const statCards = [
    { title: "Total Workflows", value: stats.workflows, icon: Workflow, color: "text-blue-600", bg: "bg-blue-50 dark:bg-blue-950" },
    { title: "Executions", value: stats.executions, icon: Activity, color: "text-green-600", bg: "bg-green-50 dark:bg-green-950" },
    { title: "Requests", value: stats.requests, icon: FileText, color: "text-purple-600", bg: "bg-purple-50 dark:bg-purple-950" },
    { title: "Active Tasks", value: stats.tasks, icon: CheckCircle2, color: "text-orange-600", bg: "bg-orange-50 dark:bg-orange-950" },
  ];

  return (
    <motion.div variants={container} initial="hidden" animate="show" className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground mt-1">Overview of your workflow automation platform</p>
        </div>
        <div className="flex gap-3">
          <Button onClick={() => navigate("/workflows/new")}>
            <Workflow className="w-4 h-4 mr-2" /> New Workflow
          </Button>
          <Button variant="outline" onClick={() => navigate("/requests/new")}>
            <FileText className="w-4 h-4 mr-2" /> New Request
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((card) => (
          <motion.div key={card.title} variants={item}>
            <Card className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-muted-foreground">{card.title}</p>
                    <p className="text-3xl font-bold mt-1">{formatNumber(card.value)}</p>
                  </div>
                  <div className={`p-3 rounded-full ${card.bg}`}>
                    <card.icon className={`w-6 h-6 ${card.color}`} />
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <motion.div variants={item} className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Recent Executions</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {recentExecutions.length === 0 ? (
                  <p className="text-muted-foreground text-sm">No recent executions</p>
                ) : (
                  recentExecutions.map((exec: any) => (
                    <div key={exec.id} className="flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors cursor-pointer" onClick={() => navigate(`/executions/${exec.id}`)}>
                      <div className="flex items-center gap-3">
                        <div className={`w-2 h-2 rounded-full ${exec.status === "COMPLETED" ? "bg-green-500" : exec.status === "RUNNING" ? "bg-blue-500" : exec.status === "FAILED" ? "bg-red-500" : "bg-yellow-500"}`} />
                        <div>
                          <p className="text-sm font-medium">{exec.workflowName}</p>
                          <p className="text-xs text-muted-foreground">{exec.triggeredBy} • {formatRelativeTime(exec.startedAt)}</p>
                        </div>
                      </div>
                      <Badge variant={exec.status === "COMPLETED" ? "success" : exec.status === "RUNNING" ? "info" : exec.status === "FAILED" ? "destructive" : "warning"}>
                        {exec.status}
                      </Badge>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item}>
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Success Rate</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-col items-center gap-4">
                <div className="relative w-32 h-32">
                  <svg className="w-full h-full" viewBox="0 0 36 36">
                    <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="hsl(var(--muted))" strokeWidth="3" />
                    <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="hsl(var(--primary))" strokeWidth="3" strokeDasharray={`${stats.successRate}, 100`} />
                  </svg>
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="text-3xl font-bold">{stats.successRate}%</span>
                  </div>
                </div>
                <div className="w-full space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Pending Approvals</span>
                    <span className="font-medium">{pendingApprovals.length}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Active Workflows</span>
                    <span className="font-medium">{stats.workflows}</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </div>

      <motion.div variants={item}>
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Pending Approvals</CardTitle>
          </CardHeader>
          <CardContent>
            {pendingApprovals.length === 0 ? (
              <p className="text-muted-foreground text-sm">No pending approvals</p>
            ) : (
              <div className="space-y-3">
                {pendingApprovals.map((req: any) => (
                  <div key={req.id} className="flex items-center justify-between p-3 rounded-lg bg-muted/50 cursor-pointer hover:bg-muted transition-colors" onClick={() => navigate(`/requests/${req.id}`)}>
                    <div>
                      <p className="text-sm font-medium">{req.title}</p>
                      <p className="text-xs text-muted-foreground">{req.requestTypeName} • {req.submittedByName}</p>
                    </div>
                    <Badge>{req.status}</Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </motion.div>
  );
}
