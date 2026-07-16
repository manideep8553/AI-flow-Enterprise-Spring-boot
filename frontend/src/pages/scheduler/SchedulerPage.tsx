import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Calendar, Play, Activity, CheckCircle2, XCircle, Clock, RefreshCw, Lock } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Progress } from "../../components/ui/progress";
import { formatRelativeTime, formatDateTime } from "../../lib/utils";
import { useToast } from "../../components/ui/use-toast";

export function SchedulerPage() {
  const { toast } = useToast();
  const [jobs, setJobs] = useState<any[]>([]);
  const [batchJobs, setBatchJobs] = useState<any[]>([]);
  const [summary, setSummary] = useState<any>({});
  const [locks, setLocks] = useState<any[]>([]);

  useEffect(() => {
    Promise.all([
      api.getSchedulerJobs(),
      api.getBatchJobs(),
      api.getSchedulerSummary(),
      api.getSchedulerLocks(),
    ]).then(([j, b, s, l]) => {
      setJobs(j);
      setBatchJobs(b);
      setSummary(s);
      setLocks(l);
    }).catch(() => {});
  }, []);

  const triggerJob = async (jobName: string) => {
    try {
      await api.triggerSchedulerJob(jobName);
      toast({ title: `Job ${jobName} triggered` });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Scheduler</h1>
          <p className="text-muted-foreground mt-1">Monitor and manage scheduled jobs</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{summary.totalJobs || jobs.length}</p><p className="text-xs text-muted-foreground">Total Jobs</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{summary.runningJobs || 0}</p><p className="text-xs text-muted-foreground">Running</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold text-green-600">{summary.completedToday || 0}</p><p className="text-xs text-muted-foreground">Completed Today</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold text-red-600">{summary.failedToday || 0}</p><p className="text-xs text-muted-foreground">Failed Today</p></CardContent></Card>
      </div>

      <Tabs defaultValue="jobs">
        <TabsList>
          <TabsTrigger value="jobs">Scheduler Jobs</TabsTrigger>
          <TabsTrigger value="batch">Batch Jobs</TabsTrigger>
          <TabsTrigger value="locks">Locks ({locks.length})</TabsTrigger>
        </TabsList>

        <TabsContent value="jobs" className="space-y-3">
          {jobs.map((job: any) => (
            <Card key={job.jobName}>
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Clock className="w-5 h-5 text-muted-foreground" />
                  <div>
                    <p className="font-medium">{job.jobName}</p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <Badge variant={job.status === "RUNNING" ? "info" : job.status === "PAUSED" ? "warning" : "secondary"}>{job.status}</Badge>
                      {job.cronExpression && <span>{job.cronExpression}</span>}
                      <span>•</span>
                      <span>Success: {job.successCount}/{job.totalExecutions}</span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {job.lastExecutedAt && <span className="text-xs text-muted-foreground">{formatRelativeTime(job.lastExecutedAt)}</span>}
                  <Button variant="outline" size="sm" onClick={() => triggerJob(job.jobName)}><Play className="w-3 h-3 mr-1" /> Trigger</Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </TabsContent>

        <TabsContent value="batch" className="space-y-3">
          {batchJobs.map((job: any) => (
            <Card key={job.jobName}>
              <CardContent className="p-4 flex items-center justify-between">
                <div>
                  <p className="font-medium">{job.jobName}</p>
                  <p className="text-xs text-muted-foreground">Last: {job.lastExecution ? formatRelativeTime(job.lastExecution) : "Never"} • Exit: {job.lastExitCode || "N/A"}</p>
                </div>
                <Button variant="outline" size="sm" onClick={() => api.launchBatchJob(job.jobName)}><Play className="w-3 h-3 mr-1" /> Launch</Button>
              </CardContent>
            </Card>
          ))}
        </TabsContent>

        <TabsContent value="locks" className="space-y-3">
          {locks.map((lock: any) => (
            <Card key={lock.lockKey || lock.name}>
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Lock className="w-5 h-5 text-muted-foreground" />
                  <div>
                    <p className="font-medium">{lock.lockKey || lock.name}</p>
                    <p className="text-xs text-muted-foreground">{lock.owner || "Unknown"} • {formatRelativeTime(lock.createdAt || lock.acquiredAt)}</p>
                  </div>
                </div>
                <Button variant="outline" size="sm" className="text-destructive" onClick={() => api.deleteSchedulerLock(lock.lockKey || lock.name)}>Release</Button>
              </CardContent>
            </Card>
          ))}
        </TabsContent>
      </Tabs>
    </motion.div>
  );
}
