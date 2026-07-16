import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Shield, Search, Filter, AlertTriangle, CheckCircle2, Eye, TrendingUp } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Textarea } from "../../components/ui/textarea";
import { Progress } from "../../components/ui/progress";
import { formatRelativeTime, getRiskColor } from "../../lib/utils";
import { useToast } from "../../components/ui/use-toast";

export function FraudDetectionPage() {
  const { toast } = useToast();
  const [checks, setChecks] = useState<any[]>([]);
  const [alerts, setAlerts] = useState<any[]>([]);
  const [stats, setStats] = useState<any>({});
  const [tab, setTab] = useState("checks");
  const [selectedCheck, setSelectedCheck] = useState<any>(null);
  const [reviewNotes, setReviewNotes] = useState("");

  useEffect(() => {
    Promise.all([
      api.getFraudChecks({ page: 0, size: 20 }),
      api.getFraudAlerts({ page: 0, size: 20 }),
      api.getFraudStatistics(),
    ]).then(([c, a, s]) => {
      setChecks(c.content);
      setAlerts(a.content);
      setStats(s);
    }).catch(() => {});
  }, []);

  const handleReview = async (id: string, status: string) => {
    try {
      await api.reviewFraud(id, { status, reviewNotes });
      toast({ title: `Fraud check ${status.toLowerCase()}` });
      setSelectedCheck(null);
      const res = await api.getFraudChecks({ page: 0, size: 20 });
      setChecks(res.content);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const resolveAlert = async (id: string) => {
    await api.resolveFraudAlert(id);
    const res = await api.getFraudAlerts({ page: 0, size: 20 });
    setAlerts(res.content);
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Fraud Detection</h1>
          <p className="text-muted-foreground mt-1">AI-powered fraud analysis and prevention</p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="destructive" className="text-sm">{stats.criticalRiskCount || 0} Critical</Badge>
          <Badge variant="warning" className="text-sm">{stats.highRiskCount || 0} High Risk</Badge>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{stats.totalChecks || 0}</p><p className="text-xs text-muted-foreground">Total Checks</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{stats.confirmedFraud || 0}</p><p className="text-xs text-muted-foreground">Confirmed Fraud</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{stats.avgRiskScore?.toFixed(1) || "0"}</p><p className="text-xs text-muted-foreground">Avg Risk Score</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{alerts.filter(a => !a.resolved).length}</p><p className="text-xs text-muted-foreground">Active Alerts</p></CardContent></Card>
      </div>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="checks">Fraud Checks</TabsTrigger>
          <TabsTrigger value="alerts">Alerts ({alerts.filter(a => !a.resolved).length})</TabsTrigger>
        </TabsList>

        <TabsContent value="checks" className="space-y-3">
          {checks.map((check: any) => (
            <Card key={check.id} className="hover:shadow-md transition-shadow cursor-pointer" onClick={() => setSelectedCheck(check)}>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3">
                      <Shield className={`w-5 h-5 ${getRiskColor(check.riskLevel)}`} />
                      <p className="font-medium">{check.userName}</p>
                      <Badge className={getRiskColor(check.riskLevel)}>{check.riskLevel}</Badge>
                      <Badge variant={check.status === "REVIEWED" ? "success" : check.status === "CONFIRMED" ? "destructive" : "warning"}>
                        {check.status}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-sm text-muted-foreground">
                      <span>{check.category}</span>
                      <span>•</span>
                      <span>${check.claimAmount?.toLocaleString()}</span>
                      <span>•</span>
                      <span>{formatRelativeTime(check.checkedAt)}</span>
                      {check.escalated && <Badge variant="destructive" className="text-xs">Escalated</Badge>}
                    </div>
                  </div>
                  <Progress value={check.overallRiskScore * 10} className="w-24" />
                </div>
              </CardContent>
            </Card>
          ))}
        </TabsContent>

        <TabsContent value="alerts" className="space-y-3">
          {alerts.map((alert: any) => (
            <Card key={alert.id}>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <AlertTriangle className={`w-5 h-5 ${getRiskColor(alert.riskLevel)}`} />
                    <div>
                      <p className="font-medium">{alert.userName}</p>
                      <p className="text-sm text-muted-foreground">{alert.summary}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {!alert.acknowledged && <Button variant="outline" size="sm" onClick={() => api.acknowledgeFraudAlert(alert.id)}>Acknowledge</Button>}
                    {!alert.resolved && <Button variant="outline" size="sm" className="text-green-600" onClick={() => resolveAlert(alert.id)}>Resolve</Button>}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </TabsContent>
      </Tabs>

      <Dialog open={!!selectedCheck} onOpenChange={() => setSelectedCheck(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>Fraud Check Details</DialogTitle></DialogHeader>
          {selectedCheck && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div><span className="text-muted-foreground">User</span><p className="font-medium">{selectedCheck.userName}</p></div>
                <div><span className="text-muted-foreground">Department</span><p className="font-medium">{selectedCheck.department}</p></div>
                <div><span className="text-muted-foreground">Category</span><p className="font-medium">{selectedCheck.category}</p></div>
                <div><span className="text-muted-foreground">Amount</span><p className="font-medium">${selectedCheck.claimAmount?.toLocaleString()}</p></div>
                <div><span className="text-muted-foreground">Risk Score</span><p className="font-medium">{selectedCheck.overallRiskScore?.toFixed(1)}/100</p></div>
                <div><span className="text-muted-foreground">Status</span><Badge>{selectedCheck.status}</Badge></div>
              </div>
              {selectedCheck.explanation && (
                <div><p className="text-sm text-muted-foreground mb-1">Explanation</p><p className="text-sm">{selectedCheck.explanation}</p></div>
              )}
              {selectedCheck.aiRecommendation && (
                <div><p className="text-sm text-muted-foreground mb-1">AI Recommendation</p><p className="text-sm">{selectedCheck.aiRecommendation}</p></div>
              )}
              {selectedCheck.status === "PENDING" && (
                <div className="space-y-3 pt-4 border-t">
                  <Textarea value={reviewNotes} onChange={(e) => setReviewNotes(e.target.value)} placeholder="Review notes..." />
                  <div className="flex gap-2 justify-end">
                    <Button variant="outline" onClick={() => handleReview(selectedCheck.id, "DISMISSED")}>Dismiss</Button>
                    <Button onClick={() => handleReview(selectedCheck.id, "CONFIRMED")}>Confirm Fraud</Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}
