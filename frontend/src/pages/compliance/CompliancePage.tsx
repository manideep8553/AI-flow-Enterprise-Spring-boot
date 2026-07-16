import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Shield, FileText, Download, Plus, Search, CheckCircle2, Clock, AlertTriangle } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Label } from "../../components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { formatDateTime, formatRelativeTime } from "../../lib/utils";

const reportTypes = [
  { value: "user-activity", label: "User Activity Report" },
  { value: "security-audit", label: "Security Audit Report" },
  { value: "workflow-audit", label: "Workflow Audit Report" },
  { value: "data-access", label: "Data Access Report" },
];

export function CompliancePage() {
  const [reports, setReports] = useState<any[]>([]);
  const [selectedType, setSelectedType] = useState("user-activity");
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    api.getComplianceReports({ page: 0, size: 20 }).then((res) => {
      setReports(res.content);
    }).catch(() => {});
  }, []);

  const generateReport = async () => {
    setGenerating(true);
    try {
      let res;
      switch (selectedType) {
        case "user-activity": res = await api.generateUserActivityReport(); break;
        case "security-audit": res = await api.generateSecurityAuditReport(); break;
        case "workflow-audit": res = await api.generateWorkflowAuditReport(); break;
        case "data-access": res = await api.generateDataAccessReport(); break;
        default: res = await api.generateComplianceReport(selectedType);
      }
      setReports((prev) => [res, ...prev]);
    } catch (err: any) {
      console.error("Report generation failed:", err);
    } finally {
      setGenerating(false);
    }
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Compliance</h1>
          <p className="text-muted-foreground mt-1">Regulatory compliance and audit reports</p>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle className="text-lg flex items-center gap-2"><FileText className="w-5 h-5" /> Generate Report</CardTitle></CardHeader>
        <CardContent>
          <div className="flex items-end gap-4">
            <div className="flex-1 space-y-2">
              <Label className="text-sm">Report Type</Label>
              <Select value={selectedType} onValueChange={setSelectedType}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {reportTypes.map((rt) => (
                    <SelectItem key={rt.value} value={rt.value}>{rt.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button onClick={generateReport} disabled={generating}>
              <Plus className="w-4 h-4 mr-2" /> {generating ? "Generating..." : "Generate Report"}
            </Button>
          </div>
        </CardContent>
      </Card>

      <div className="space-y-3">
        {reports.map((report: any) => (
          <Card key={report.id}>
            <CardContent className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-lg bg-primary/10">
                  <Shield className="w-5 h-5 text-primary" />
                </div>
                <div>
                  <p className="font-medium">{report.reportType?.replace(/-/g, " ") || "Compliance Report"}</p>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    <Badge variant={report.status === "COMPLETED" ? "success" : report.status === "GENERATING" ? "info" : "warning"}>{report.status}</Badge>
                    <span>{report.generatedBy}</span>
                    <span>•</span>
                    <span>{formatRelativeTime(report.generatedAt)}</span>
                  </div>
                </div>
              </div>
              <Button variant="outline" size="sm"><Download className="w-4 h-4 mr-1" /> Download</Button>
            </CardContent>
          </Card>
        ))}
        {reports.length === 0 && (
          <div className="text-center py-12"><p className="text-muted-foreground">No compliance reports yet. Generate your first report above.</p></div>
        )}
      </div>
    </motion.div>
  );
}
