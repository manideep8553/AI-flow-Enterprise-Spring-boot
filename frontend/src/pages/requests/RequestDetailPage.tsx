import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowLeft, CheckCircle2, XCircle, MessageSquare, Clock, User, Activity, History, Paperclip } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Separator } from "../../components/ui/separator";
import { Textarea } from "../../components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { formatDateTime } from "../../lib/utils";
import { useToast } from "../../components/ui/use-toast";

export function RequestDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [request, setRequest] = useState<any>(null);
  const [comment, setComment] = useState("");

  useEffect(() => {
    if (!id) return;
    api.getRequest(id).then(setRequest).catch(() => navigate("/requests"));
  }, [id, navigate]);

  const handleAction = async (action: "approve" | "reject") => {
    try {
      if (action === "approve") await api.approveRequest(request.id);
      else await api.rejectRequest(request.id);
      toast({ title: `Request ${action}d` });
      const updated = await api.getRequest(request.id);
      setRequest(updated);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleAddComment = async () => {
    if (!comment.trim()) return;
    try {
      await api.addComment(request.id, comment);
      setComment("");
      const updated = await api.getRequest(request.id);
      setRequest(updated);
      toast({ title: "Comment added" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  if (!request) return <div className="flex items-center justify-center h-64"><Activity className="w-6 h-6 animate-spin" /></div>;

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate("/requests")}><ArrowLeft className="w-5 h-5" /></Button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold">{request.title}</h1>
              <Badge>{request.status.replace(/_/g, " ")}</Badge>
            </div>
            <p className="text-sm text-muted-foreground mt-1">{request.requestTypeName} • Submitted by {request.submittedByName} • {formatDateTime(request.submittedAt)}</p>
          </div>
        </div>
        {(request.status === "PENDING_APPROVAL" || request.status === "SUBMITTED") && (
          <div className="flex gap-2">
            <Button variant="outline" className="text-green-600 border-green-200 hover:bg-green-50" onClick={() => handleAction("approve")}>
              <CheckCircle2 className="w-4 h-4 mr-2" /> Approve
            </Button>
            <Button variant="outline" className="text-red-600 border-red-200 hover:bg-red-50" onClick={() => handleAction("reject")}>
              <XCircle className="w-4 h-4 mr-2" /> Reject
            </Button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader><CardTitle className="text-lg">Details</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              {request.description && <p className="text-sm">{request.description}</p>}
              {request.fields && Object.entries(request.fields).map(([key, val]) => (
                <div key={key} className="flex justify-between text-sm">
                  <span className="text-muted-foreground capitalize">{key.replace(/([A-Z])/g, " $1")}</span>
                  <span className="font-medium">{String(val)}</span>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle className="text-lg flex items-center gap-2"><MessageSquare className="w-4 h-4" /> Comments</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              {request.comments?.map((c: any) => (
                <div key={c.id} className="p-3 rounded-lg bg-muted/50">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium">{c.authorName}</span>
                    <span className="text-xs text-muted-foreground">{formatDateTime(c.createdAt)}</span>
                  </div>
                  <p className="text-sm mt-1">{c.text}</p>
                </div>
              ))}
              <div className="flex gap-2">
                <Textarea value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Add a comment..." className="flex-1" />
                <Button onClick={handleAddComment}>Send</Button>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader><CardTitle className="text-lg">Info</CardTitle></CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex justify-between"><span className="text-muted-foreground">Priority</span><Badge variant="outline">{request.priority || "Normal"}</Badge></div>
              <div className="flex justify-between"><span className="text-muted-foreground">Status</span><Badge>{request.status.replace(/_/g, " ")}</Badge></div>
              <div className="flex justify-between"><span className="text-muted-foreground">Assigned To</span><span>{request.assignedToName || "Unassigned"}</span></div>
              {request.dueDate && <div className="flex justify-between"><span className="text-muted-foreground">Due Date</span><span>{formatDateTime(request.dueDate)}</span></div>}
              <Separator />
              <div className="flex justify-between"><span className="text-muted-foreground">Workflow</span><span>{request.workflowName || "N/A"}</span></div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle className="text-lg flex items-center gap-2"><History className="w-4 h-4" /> History</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              {request.statusHistory?.map((entry: any, idx: number) => (
                <div key={idx} className="flex items-start gap-2 text-sm">
                  <div className="w-2 h-2 rounded-full bg-primary mt-1.5 shrink-0" />
                  <div>
                    <p className="text-xs text-muted-foreground">{formatDateTime(entry.timestamp)}</p>
                    <p>{entry.fromStatus || "Created"} → {entry.toStatus || request.status}</p>
                  </div>
                </div>
              )) || <p className="text-sm text-muted-foreground">No history</p>}
            </CardContent>
          </Card>
        </div>
      </div>
    </motion.div>
  );
}
