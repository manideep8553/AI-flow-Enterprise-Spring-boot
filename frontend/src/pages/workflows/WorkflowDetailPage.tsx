import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowLeft, Play, Archive, Trash2, Download, RotateCcw, CheckCircle2, Activity } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Progress } from "../../components/ui/progress";
import { Separator } from "../../components/ui/separator";
import { formatDateTime } from "../../lib/utils";
import { useToast } from "../../components/ui/use-toast";

export function WorkflowDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [workflow, setWorkflow] = useState<any>(null);
  const [executions, setExecutions] = useState<any[]>([]);

  useEffect(() => {
    if (!id) return;
    Promise.all([
      api.getWorkflow(id),
      api.getExecutions({ workflowId: id, page: 0, size: 10 }),
    ]).then(([wf, exec]) => {
      setWorkflow(wf);
      setExecutions(exec.content);
    }).catch(() => navigate("/workflows"));
  }, [id, navigate]);

  if (!workflow) return <div className="flex items-center justify-center h-64"><Activity className="w-6 h-6 animate-spin" /></div>;

  const handleExecute = async () => {
    try {
      await api.executeWorkflow(workflow.id, {});
      toast({ title: "Execution started" });
    } catch (err: any) {
      toast({ title: "Execution failed", description: err.message, variant: "destructive" });
    }
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate("/workflows")}><ArrowLeft className="w-5 h-5" /></Button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold">{workflow.name}</h1>
              <Badge variant={workflow.status === "PUBLISHED" ? "success" : workflow.status === "DRAFT" ? "warning" : "secondary"}>{workflow.status}</Badge>
              <span className="text-sm text-muted-foreground">v{workflow.version}</span>
            </div>
            {workflow.description && <p className="text-muted-foreground mt-1">{workflow.description}</p>}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => navigate(`/workflows/${id}/edit`)}><Activity className="w-4 h-4 mr-2" /> Edit</Button>
          <Button onClick={handleExecute}><Play className="w-4 h-4 mr-2" /> Execute</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{workflow.steps?.length || 0}</p><p className="text-xs text-muted-foreground">Steps</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{executions.length}</p><p className="text-xs text-muted-foreground">Executions</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{executions.filter((e: any) => e.status === "COMPLETED").length}</p><p className="text-xs text-muted-foreground">Successful</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{workflow.tags?.length || 0}</p><p className="text-xs text-muted-foreground">Tags</p></CardContent></Card>
      </div>

      <Tabs defaultValue="steps">
        <TabsList>
          <TabsTrigger value="steps">Steps</TabsTrigger>
          <TabsTrigger value="executions">Executions</TabsTrigger>
          <TabsTrigger value="versions">Versions</TabsTrigger>
        </TabsList>
        <TabsContent value="steps" className="space-y-2">
          {workflow.steps?.map((step: any, idx: number) => (
            <Card key={step.stepId}>
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className="text-sm text-muted-foreground">{idx + 1}.</span>
                  <div>
                    <p className="font-medium">{step.name}</p>
                    <p className="text-xs text-muted-foreground">{step.type.replace(/_/g, " ")}{step.description ? ` - ${step.description}` : ""}</p>
                  </div>
                </div>
                <Badge variant="outline">{step.type.replace(/_/g, " ")}</Badge>
              </CardContent>
            </Card>
          ))}
        </TabsContent>
        <TabsContent value="executions" className="space-y-2">
          {executions.map((exec: any) => (
            <Card key={exec.id} className="cursor-pointer hover:shadow-md transition-shadow" onClick={() => navigate(`/executions/${exec.id}`)}>
              <CardContent className="p-4 flex items-center justify-between">
                <div>
                  <p className="font-medium">{exec.workflowName}</p>
                  <p className="text-xs text-muted-foreground">{exec.triggeredBy} • {formatDateTime(exec.startedAt)}</p>
                </div>
                <Badge variant={exec.status === "COMPLETED" ? "success" : exec.status === "RUNNING" ? "info" : exec.status === "FAILED" ? "destructive" : "warning"}>{exec.status}</Badge>
              </CardContent>
            </Card>
          ))}
        </TabsContent>
        <TabsContent value="versions">
          <p className="text-muted-foreground text-sm">Version history coming soon</p>
        </TabsContent>
      </Tabs>
    </motion.div>
  );
}
