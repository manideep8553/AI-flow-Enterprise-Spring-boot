import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { BarChart3, FileText, Download, Calendar, TrendingUp, PieChart, Activity } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { useToast } from "../../components/ui/use-toast";

export function ReportsPage() {
  const { toast } = useToast();
  const [timeframe, setTimeframe] = useState("30d");
  const [summary, setSummary] = useState<any>({});

  useEffect(() => {
    api.getAuditLogSummary({ from: undefined, to: undefined }).then(setSummary).catch(() => {});
  }, [timeframe]);

  const reportCategories = [
    { title: "Workflow Performance", desc: "Execution metrics, success rates, and duration analysis", icon: Activity, color: "text-blue-600" },
    { title: "User Activity", desc: "Login patterns, action distribution, and user engagement", icon: TrendingUp, color: "text-green-600" },
    { title: "Request Analytics", desc: "Request volumes, approval times, and SLA compliance", icon: BarChart3, color: "text-purple-600" },
    { title: "Fraud Summary", desc: "Fraud detection rates, risk distribution, and alert metrics", icon: PieChart, color: "text-orange-600" },
    { title: "Compliance Report", desc: "Regulatory compliance status and audit trail summary", icon: FileText, color: "text-red-600" },
    { title: "Document Processing", desc: "Document volumes, OCR accuracy, and processing times", icon: FileText, color: "text-teal-600" },
  ];

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Reports</h1>
          <p className="text-muted-foreground mt-1">Generate and download platform reports</p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={timeframe} onValueChange={setTimeframe}>
            <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="7d">Last 7 days</SelectItem>
              <SelectItem value="30d">Last 30 days</SelectItem>
              <SelectItem value="90d">Last 90 days</SelectItem>
              <SelectItem value="1y">Last year</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{summary.total || 0}</p><p className="text-xs text-muted-foreground">Total Events</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold">{Object.keys(summary.byAction || {}).length}</p><p className="text-xs text-muted-foreground">Action Types</p></CardContent></Card>
        <Card><CardContent className="p-4 text-center"><p className="text-2xl font-bold text-green-600">98.5%</p><p className="text-xs text-muted-foreground">Success Rate</p></CardContent></Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {reportCategories.map((cat) => (
          <Card key={cat.title} className="hover:shadow-md transition-shadow cursor-pointer group">
            <CardContent className="p-6">
              <div className={`p-3 rounded-lg w-fit bg-muted group-hover:bg-primary/10 transition-colors`}>
                <cat.icon className={`w-6 h-6 ${cat.color}`} />
              </div>
              <h3 className="font-semibold mt-4">{cat.title}</h3>
              <p className="text-sm text-muted-foreground mt-1">{cat.desc}</p>
              <div className="flex items-center gap-2 mt-4">
                <Button variant="outline" size="sm"><Download className="w-3 h-3 mr-1" /> CSV</Button>
                <Button variant="outline" size="sm"><Download className="w-3 h-3 mr-1" /> PDF</Button>
                <Button variant="ghost" size="sm">View</Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </motion.div>
  );
}
