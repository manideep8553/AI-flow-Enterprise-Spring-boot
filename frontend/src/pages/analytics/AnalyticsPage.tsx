import { useState, useEffect, useCallback, useRef } from "react";
import { motion } from "framer-motion";
import {
  TrendingUp, TrendingDown, BarChart3, Activity, Users, CheckCircle2, Clock,
  Download, RefreshCw, AlertTriangle, FileText, Bell, Brain, Server, Building2,
  Shield, Database, Zap
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Progress } from "../../components/ui/progress";
import { Skeleton } from "../../components/ui/skeleton";
import { api } from "../../lib/api";
import { formatBytes, formatDuration } from "../../lib/utils";
import {
  Line, AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  ComposedChart
} from "recharts";

const container = { hidden: { opacity: 0 }, show: { opacity: 1, transition: { staggerChildren: 0.05 } } };
const item = { hidden: { opacity: 0, y: 20 }, show: { opacity: 1, y: 0 } };

const CHART_COLORS = ["hsl(var(--primary))", "#22c55e", "#ef4444", "#f59e0b", "#8b5cf6", "#06b6d4", "#ec4899", "#14b8a6"];
const PIE_COLORS = ["hsl(var(--primary))", "#22c55e", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4"];

function toChartData(arr: any[], valueKey: string = "count"): any[] {
  if (!arr || !Array.isArray(arr)) return [];
  return arr.map((d: any) => ({
    ...d,
    period: d.period || d.name || d.date || "",
    value: d.value ?? d[valueKey] ?? 0,
  }));
}

export function AnalyticsPage() {
  const [period, setPeriod] = useState("30d");
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval>>();

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await api.getAnalyticsSummary({ period });
      setData(res.data || res);
    } catch (e: any) {
      setError(e.message || "Failed to load analytics");
    } finally {
      setLoading(false);
    }
  }, [period]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    if (autoRefresh) {
      intervalRef.current = setInterval(fetchData, 30000);
    } else {
      if (intervalRef.current) clearInterval(intervalRef.current);
    }
    return () => { if (intervalRef.current) clearInterval(intervalRef.current); };
  }, [autoRefresh, fetchData]);

  const kpi = data?.kpi;
  const workflows = data?.workflows;
  const requests = data?.requests;
  const users = data?.users;
  const documents = data?.documents;
  const fraud = data?.fraud;
  const notifications = data?.notifications;
  const ai = data?.ai;
  const scheduler = data?.scheduler;
  const bottlenecks = data?.bottlenecks || [];
  const departments = data?.departments || [];

  const exportCSV = () => {
    const summary = {
      timestamp: new Date().toISOString(),
      period,
      ...kpi,
      bottlenecks: bottlenecks.length,
      departments: departments.length,
    };
    const csv = Object.entries(summary).map(([k, v]) => `${k},${v}`).join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `analytics-${period}-${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  if (loading && !data) {
    return (
      <div className="space-y-6 p-6">
        <div className="flex justify-between"><Skeleton className="h-10 w-48" /><Skeleton className="h-10 w-64" /></div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1,2,3,4].map(i => <Skeleton key={i} className="h-32" />)}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-80" /><Skeleton className="h-80" />
        </div>
      </div>
    );
  }

  return (
    <motion.div variants={container} initial="hidden" animate="show" className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Analytics</h1>
          <p className="text-muted-foreground mt-1">Real-time platform performance and business intelligence</p>
        </div>
        <div className="flex items-center gap-3">
          <Tabs value={period} onValueChange={setPeriod}>
            <TabsList>
              <TabsTrigger value="24h">24h</TabsTrigger>
              <TabsTrigger value="7d">7d</TabsTrigger>
              <TabsTrigger value="30d">30d</TabsTrigger>
              <TabsTrigger value="90d">90d</TabsTrigger>
              <TabsTrigger value="1y">1y</TabsTrigger>
            </TabsList>
          </Tabs>
          <Button variant={autoRefresh ? "default" : "outline"} size="icon" onClick={() => setAutoRefresh(!autoRefresh)} title="Auto-refresh every 30s">
            <RefreshCw className={`w-4 h-4 ${autoRefresh ? "animate-spin" : ""}`} />
          </Button>
          <Button variant="outline" size="icon" onClick={exportCSV} title="Export CSV">
            <Download className="w-4 h-4" />
          </Button>
          <Button variant="outline" size="sm" onClick={fetchData} disabled={loading}>
            <RefreshCw className={`w-4 h-4 mr-2 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>
      </div>

      {error && (
        <Card className="border-destructive/50 bg-destructive/5">
          <CardContent className="p-4 flex items-center gap-3">
            <AlertTriangle className="w-5 h-5 text-destructive" />
            <p className="text-sm text-destructive">{error}</p>
            <Button variant="outline" size="sm" onClick={fetchData}>Retry</Button>
          </CardContent>
        </Card>
      )}

      {/* KPI Cards */}
      {kpi && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-6 gap-4">
          <motion.div variants={item}>
            <Card className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-muted-foreground uppercase tracking-wide">Completion Rate</p>
                  <Activity className="w-4 h-4 text-primary" />
                </div>
                <p className="text-2xl font-bold">{kpi.completionRate?.toFixed(1) ?? "--"}%</p>
                <div className="flex items-center gap-1 mt-1">
                  {kpi.avgDurationChange != null && (
                    <span className={`text-xs flex items-center ${kpi.avgDurationChange < 0 ? "text-green-600" : "text-red-600"}`}>
                      {kpi.avgDurationChange < 0 ? <TrendingDown className="w-3 h-3" /> : <TrendingUp className="w-3 h-3" />}
                      {Math.abs(kpi.avgDurationChange).toFixed(1)}%
                    </span>
                  )}
                  <span className="text-xs text-muted-foreground ml-1">vs prev</span>
                </div>
                <Progress value={kpi.completionRate ?? 0} className="mt-2 h-1.5" />
              </CardContent>
            </Card>
          </motion.div>

          <motion.div variants={item}>
            <Card className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-muted-foreground uppercase tracking-wide">Avg Duration</p>
                  <Clock className="w-4 h-4 text-blue-500" />
                </div>
                <p className="text-2xl font-bold">{kpi.avgDurationSeconds ? formatDuration(kpi.avgDurationSeconds * 1000) : "--"}</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {kpi.totalExecutions?.toLocaleString()} total executions
                </p>
              </CardContent>
            </Card>
          </motion.div>

          <motion.div variants={item}>
            <Card className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-muted-foreground uppercase tracking-wide">Active Users</p>
                  <Users className="w-4 h-4 text-green-500" />
                </div>
                <p className="text-2xl font-bold">{kpi.activeUsers?.toLocaleString() ?? "--"}</p>
                <div className="flex items-center gap-1 mt-1">
                  {kpi.activeUserChange != null && (
                    <span className={`text-xs flex items-center ${kpi.activeUserChange >= 0 ? "text-green-600" : "text-red-600"}`}>
                      {kpi.activeUserChange >= 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                      {Math.abs(kpi.activeUserChange).toFixed(1)}%
                    </span>
                  )}
                  <span className="text-xs text-muted-foreground">of {kpi.totalUsers?.toLocaleString()} total</span>
                </div>
              </CardContent>
            </Card>
          </motion.div>

          <motion.div variants={item}>
            <Card className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-muted-foreground uppercase tracking-wide">Requests</p>
                  <FileText className="w-4 h-4 text-purple-500" />
                </div>
                <p className="text-2xl font-bold">{kpi.totalRequests?.toLocaleString() ?? "--"}</p>
                <div className="flex items-center gap-1 mt-1">
                  {kpi.pendingApprovals > 0 && (
                    <Badge variant="warning" className="text-xs">{kpi.pendingApprovals} pending</Badge>
                  )}
                  {kpi.requestChange != null && (
                    <span className={`text-xs flex items-center ml-1 ${kpi.requestChange >= 0 ? "text-green-600" : "text-red-600"}`}>
                      {kpi.requestChange >= 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                      {Math.abs(kpi.requestChange).toFixed(1)}%
                    </span>
                  )}
                </div>
              </CardContent>
            </Card>
          </motion.div>

          <motion.div variants={item}>
            <Card className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-muted-foreground uppercase tracking-wide">Documents</p>
                  <Database className="w-4 h-4 text-orange-500" />
                </div>
                <p className="text-2xl font-bold">{kpi.totalDocuments?.toLocaleString() ?? "--"}</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {kpi.totalStorageBytes ? formatBytes(kpi.totalStorageBytes) : "--"} stored
                </p>
              </CardContent>
            </Card>
          </motion.div>

          <motion.div variants={item}>
            <Card className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs text-muted-foreground uppercase tracking-wide">Alerts</p>
                  <Bell className="w-4 h-4 text-red-500" />
                </div>
                <p className="text-2xl font-bold">{kpi.openFraudAlerts?.toLocaleString() ?? 0}</p>
                <div className="flex items-center gap-1 mt-1">
                  {kpi.unreadNotifications > 0 && (
                    <span className="text-xs text-muted-foreground">{kpi.unreadNotifications} unread notifications</span>
                  )}
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      )}

      {/* Main Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Workflow Trend */}
        <motion.div variants={item}>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg">Execution Trends</CardTitle>
              <Badge variant="outline">{workflows?.totalThisPeriod?.toLocaleString()} this period</Badge>
            </CardHeader>
            <CardContent>
              {workflows?.trend?.length > 0 ? (
                <ResponsiveContainer width="100%" height={280}>
                  <AreaChart data={toChartData(workflows.trend)}>
                    <defs>
                      <linearGradient id="execGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                    <XAxis dataKey="period" tick={{ fontSize: 11 }} angle={-30} textAnchor="end" height={50} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))" }} />
                    <Area type="monotone" dataKey="count" stroke="hsl(var(--primary))" fill="url(#execGrad)" strokeWidth={2} name="Executions" />
                  </AreaChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-[280px] flex items-center justify-center text-muted-foreground text-sm">
                  No execution data available
                </div>
              )}
              {workflows && (
                <div className="grid grid-cols-3 gap-4 mt-4 pt-4 border-t">
                  <div className="text-center">
                    <p className="text-lg font-bold text-green-600">{workflows.completed?.toLocaleString()}</p>
                    <p className="text-xs text-muted-foreground">Completed ({workflows.successRate?.toFixed(1)}%)</p>
                  </div>
                  <div className="text-center">
                    <p className="text-lg font-bold text-red-600">{workflows.failed?.toLocaleString()}</p>
                    <p className="text-xs text-muted-foreground">Failed</p>
                  </div>
                  <div className="text-center">
                    <p className="text-lg font-bold text-blue-600">{workflows.running?.toLocaleString()}</p>
                    <p className="text-xs text-muted-foreground">Running</p>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </motion.div>

        {/* Request Trend + Approval Rate */}
        <motion.div variants={item}>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg">Request Activity</CardTitle>
              {requests && (
                <Badge variant="outline" className="text-xs">
                  SLA: {requests.slaCompliance?.toFixed(1) ?? "--"}%
                </Badge>
              )}
            </CardHeader>
            <CardContent>
              {requests?.trend?.length > 0 ? (
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={toChartData(requests.trend)}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                    <XAxis dataKey="period" tick={{ fontSize: 10 }} angle={-30} textAnchor="end" height={40} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip contentStyle={{ borderRadius: 8, border: "1px solid hsl(var(--border))" }} />
                    <Bar dataKey="count" fill="hsl(var(--primary))" radius={[2, 2, 0, 0]} name="Requests" />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-[200px] flex items-center justify-center text-muted-foreground text-sm">
                  No request data available
                </div>
              )}
              <div className="grid grid-cols-4 gap-2 mt-3 pt-3 border-t">
                <div className="text-center">
                  <p className="text-sm font-bold text-green-600">{requests?.approved?.toLocaleString() ?? 0}</p>
                  <p className="text-[10px] text-muted-foreground">Approved</p>
                </div>
                <div className="text-center">
                  <p className="text-sm font-bold text-red-600">{requests?.rejected?.toLocaleString() ?? 0}</p>
                  <p className="text-[10px] text-muted-foreground">Rejected</p>
                </div>
                <div className="text-center">
                  <p className="text-sm font-bold text-yellow-600">{requests?.pendingApproval?.toLocaleString() ?? 0}</p>
                  <p className="text-[10px] text-muted-foreground">Pending</p>
                </div>
                <div className="text-center">
                  <p className="text-sm font-bold text-orange-600">{requests?.escalated?.toLocaleString() ?? 0}</p>
                  <p className="text-[10px] text-muted-foreground">Escalated</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Second Row: Approval Trend + Bottlenecks */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <motion.div variants={item}>
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Approval Trends</CardTitle>
            </CardHeader>
            <CardContent>
              {requests?.approvalTrend?.length > 0 ? (
                <ResponsiveContainer width="100%" height={260}>
                  <ComposedChart data={requests.approvalTrend}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                    <XAxis dataKey="period" tick={{ fontSize: 10 }} angle={-30} textAnchor="end" height={40} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip contentStyle={{ borderRadius: 8 }} />
                    <Legend />
                    <Bar dataKey="successCount" fill="#22c55e" radius={[2,2,0,0]} name="Approved" stackId="a" />
                    <Bar dataKey="failureCount" fill="#ef4444" radius={[2,2,0,0]} name="Rejected" stackId="a" />
                    <Line type="monotone" dataKey="count" stroke="hsl(var(--primary))" strokeWidth={2} name="Total" dot={false} />
                  </ComposedChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-[260px] flex items-center justify-center text-muted-foreground text-sm">
                  No approval data available
                </div>
              )}
              <div className="flex items-center justify-between mt-3 pt-3 border-t text-sm">
                <span>Approval Rate: <strong>{requests?.approvalRate?.toFixed(1) ?? "--"}%</strong></span>
                <span>Avg Time: <strong>{requests?.avgApprovalTimeHours ? `${requests.avgApprovalTimeHours.toFixed(1)}h` : "--"}</strong></span>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item}>
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Workflow Bottlenecks</CardTitle>
            </CardHeader>
            <CardContent>
              {bottlenecks.length > 0 ? (
                <div className="space-y-3 max-h-[300px] overflow-y-auto">
                  {bottlenecks.slice(0, 8).map((b: any, i: number) => (
                    <div key={i} className="flex items-center justify-between p-2.5 rounded-lg bg-muted/50 hover:bg-muted transition-colors">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">{b.workflowName || b.stepId}</p>
                        <p className="text-xs text-muted-foreground truncate">Step: {b.stepName || b.stepId}</p>
                      </div>
                      <div className="flex items-center gap-3 ml-3">
                        <div className="text-right">
                          <p className="text-xs font-medium">{b.occurrenceCount}x</p>
                          <p className="text-[10px] text-muted-foreground">{b.avgDurationSeconds?.toFixed(1)}s</p>
                        </div>
                        <Badge variant={b.severity === "HIGH" ? "destructive" : b.severity === "MEDIUM" ? "warning" : "secondary"}>
                          {b.severity}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="h-[200px] flex items-center justify-center text-muted-foreground text-sm">
                  <div className="text-center">
                    <CheckCircle2 className="w-8 h-8 mx-auto mb-2 text-green-500" />
                    <p>No bottlenecks detected</p>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Third Row: Fraud + AI + Notifications */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <motion.div variants={item}>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><Shield className="w-4 h-4 inline mr-2" />Fraud Detection</CardTitle>
              {fraud && <Badge variant={fraud.openAlerts > 0 ? "destructive" : "success"}>{fraud.openAlerts} alerts</Badge>}
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Total Checks</span>
                  <span className="font-bold">{fraud?.totalChecks?.toLocaleString() ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Pending Review</span>
                  <span className="font-bold text-yellow-600">{fraud?.pendingReview ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Confirmed</span>
                  <span className="font-bold text-red-600">{fraud?.confirmed ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Avg Risk Score</span>
                  <span className="font-bold">{fraud?.avgRiskScore?.toFixed(2) ?? "--"}</span>
                </div>
                {fraud?.byRiskLevel && (
                  <div className="pt-2 border-t space-y-2">
                    {Object.entries(fraud.byRiskLevel).map(([k, v]) => (
                      <div key={k} className="flex items-center gap-2">
                        <span className="text-xs w-16">{k}</span>
                        <Progress value={Number(v) / Math.max(...Object.values(fraud.byRiskLevel) as number[]) * 100} className="h-1.5 flex-1" />
                        <span className="text-xs w-8 text-right">{v}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item}>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><Brain className="w-4 h-4 inline mr-2" />AI Service</CardTitle>
              <Badge variant="outline">{ai?.totalCalls ?? 0} calls</Badge>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Acceptance Rate</span>
                  <span className="font-bold text-green-600">{ai?.acceptanceRate?.toFixed(1) ?? "--"}%</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Avg Response</span>
                  <span className="font-bold">{ai?.avgResponseTimeMs ? `${ai.avgResponseTimeMs.toFixed(0)}ms` : "--"}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Accepted</span>
                  <span className="font-bold text-green-600">{ai?.accepted ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Total Tokens</span>
                  <span className="font-bold">{ai?.totalTokensUsed?.toLocaleString() ?? 0}</span>
                </div>
                {ai?.byRequestType && (
                  <div className="pt-2 border-t">
                    <p className="text-xs text-muted-foreground mb-2">By Type</p>
                    {Object.entries(ai.byRequestType).slice(0, 4).map(([k, v]) => (
                      <div key={k} className="flex justify-between text-xs py-0.5">
                        <span className="truncate mr-2">{k}</span>
                        <span className="font-medium">{v}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item}>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><Bell className="w-4 h-4 inline mr-2" />Notifications</CardTitle>
              <Badge variant="secondary">{notifications?.delivered?.toLocaleString()} delivered</Badge>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Total</span>
                  <span className="font-bold">{notifications?.total?.toLocaleString() ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Delivery Rate</span>
                  <span className="font-bold text-green-600">{notifications?.deliveryRate?.toFixed(1) ?? "--"}%</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Failed</span>
                  <span className="font-bold text-red-600">{notifications?.failed ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Unread</span>
                  <span className="font-bold">{notifications?.unread ?? 0}</span>
                </div>
                {notifications?.byChannel && (
                  <div className="pt-2 border-t">
                    <p className="text-xs text-muted-foreground mb-2">By Channel</p>
                    <div className="grid grid-cols-2 gap-1">
                      {Object.entries(notifications.byChannel).map(([k, v]) => (
                        <div key={k} className="flex justify-between text-xs py-0.5">
                          <span>{k}</span>
                          <span className="font-medium">{v}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Fourth Row: Departments + Users + Scheduler */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <motion.div variants={item} className="lg:col-span-1">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><Building2 className="w-4 h-4 inline mr-2" />Departments</CardTitle>
              <Badge variant="outline">{departments.length}</Badge>
            </CardHeader>
            <CardContent>
              {departments.length > 0 ? (
                <div className="space-y-2 max-h-[300px] overflow-y-auto">
                  {departments.map((d: any, i: number) => (
                    <div key={i} className="flex items-center justify-between p-2 rounded-lg bg-muted/30">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">{d.departmentName || d.departmentId}</p>
                        <p className="text-[10px] text-muted-foreground">{d.activeUsers}/{d.totalUsers} active users</p>
                      </div>
                      <div className="text-right ml-2">
                        <p className="text-xs font-medium">{d.completionRate?.toFixed(0) ?? 0}%</p>
                        <p className="text-[10px] text-muted-foreground">{d.completedRequests}/{d.totalRequests}</p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="h-[150px] flex items-center justify-center text-muted-foreground text-sm">
                  No department data
                </div>
              )}
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item} className="lg:col-span-1">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><Users className="w-4 h-4 inline mr-2" />User Activity</CardTitle>
              <Badge variant="outline">{users?.active?.toLocaleString()} active</Badge>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Total Users</span>
                  <span className="font-bold">{users?.total?.toLocaleString() ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Active (this period)</span>
                  <span className="font-bold text-green-600">{users?.active?.toLocaleString() ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">New (this period)</span>
                  <span className="font-bold text-blue-600">{users?.newThisPeriod?.toLocaleString() ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Active Sessions</span>
                  <span className="font-bold">{users?.withSessions?.toLocaleString() ?? 0}</span>
                </div>
                {users?.byRole && (
                  <div className="pt-2 border-t">
                    <p className="text-xs text-muted-foreground mb-2">By Role</p>
                    {Object.entries(users.byRole).map(([k, v]) => (
                      <div key={k} className="flex justify-between text-xs py-0.5">
                        <span>{k}</span>
                        <span className="font-medium">{v}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item} className="lg:col-span-1">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><Server className="w-4 h-4 inline mr-2" />Scheduler</CardTitle>
              <Badge variant={scheduler?.failedToday > 0 ? "destructive" : "success"}>
                {scheduler?.running ?? 0} running
              </Badge>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex justify-between items-center">
                  <span className="text-sm">Today Success</span>
                  <span className="font-bold text-green-600">{scheduler?.completedToday ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Today Failed</span>
                  <span className="font-bold text-red-600">{scheduler?.failedToday ?? 0}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Success Rate</span>
                  <span className="font-bold">{scheduler?.successRate?.toFixed(1) ?? "--"}%</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm">Active Locks</span>
                  <span className="font-bold">{scheduler?.activeLocks ?? 0}</span>
                </div>
                {scheduler?.jobs?.length > 0 && (
                  <div className="pt-2 border-t max-h-[120px] overflow-y-auto">
                    <p className="text-xs text-muted-foreground mb-1">Job Health</p>
                    {scheduler.jobs.slice(0, 5).map((j: any, i: number) => (
                      <div key={i} className="flex items-center justify-between text-xs py-0.5">
                        <span className="truncate mr-2">{j.jobName}</span>
                        <span className={`font-medium ${j.failures > 0 ? "text-red-600" : "text-green-600"}`}>
                          {j.successRate?.toFixed(0)}%
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Documents + Pie Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <motion.div variants={item}>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><Database className="w-4 h-4 inline mr-2" />Documents & Storage</CardTitle>
              <Badge variant="outline">{formatBytes(documents?.totalStorageBytes ?? 0)}</Badge>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-sm">Total</span>
                    <span className="font-bold">{documents?.total?.toLocaleString() ?? 0}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm">Processing</span>
                    <span className="font-bold text-blue-600">{documents?.processing ?? 0}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm">Completed</span>
                    <span className="font-bold text-green-600">{documents?.completed ?? 0}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm">Archived</span>
                    <span className="font-bold text-muted-foreground">{documents?.archived ?? 0}</span>
                  </div>
                </div>
                <div>
                  {documents?.byType && Object.keys(documents.byType).length > 0 && (
                    <ResponsiveContainer width="100%" height={160}>
                      <PieChart>
                        <Pie data={Object.entries(documents.byType).map(([k, v]) => ({ name: k, value: v }))}
                          cx="50%" cy="50%" innerRadius={40} outerRadius={70} dataKey="value" paddingAngle={2}>
                          {Object.entries(documents.byType).map((_, i) => (
                            <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                          ))}
                        </Pie>
                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        <motion.div variants={item}>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-lg"><BarChart3 className="w-4 h-4 inline mr-2" />Workflow Triggers</CardTitle>
            </CardHeader>
            <CardContent>
              {workflows?.byTriggerType && Object.keys(workflows.byTriggerType).length > 0 ? (
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={Object.entries(workflows.byTriggerType).map(([k, v]) => ({ name: k, value: v }))}
                    layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                    <XAxis type="number" tick={{ fontSize: 11 }} />
                    <YAxis type="category" dataKey="name" tick={{ fontSize: 11 }} width={80} />
                    <Tooltip contentStyle={{ borderRadius: 8 }} />
                    <Bar dataKey="value" fill="hsl(var(--primary))" radius={[0, 4, 4, 0]}>
                      {Object.entries(workflows.byTriggerType).map((_, i) => (
                        <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div className="h-[260px] flex items-center justify-center text-muted-foreground text-sm">
                  No trigger data available
                </div>
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </motion.div>
  );
}
