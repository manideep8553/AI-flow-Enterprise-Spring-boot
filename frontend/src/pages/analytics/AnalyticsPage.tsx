import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { TrendingUp, TrendingDown, BarChart3, PieChart, Activity, Users, CheckCircle2, Clock } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Badge } from "../../components/ui/badge";
import { api } from "../../lib/api";

export function AnalyticsPage() {
  const [timeframe, setTimeframe] = useState("30d");
  const [trends, setTrends] = useState<any[]>([]);
  const [insights, setInsights] = useState<any[]>([]);

  useEffect(() => {
    Promise.all([
      api.analyzeTrends({ timeframe, metric: "completion_rate" }),
      api.detectBottlenecks(),
    ]).then(([trendRes, bottleneckRes]) => {
      setTrends(trendRes.trends || []);
      setInsights(bottleneckRes.bottlenecks || []);
    }).catch(() => {});
  }, [timeframe]);

  const stats = [
    { label: "Completion Rate", value: "94.2%", change: "+2.1%", positive: true, icon: CheckCircle2 },
    { label: "Avg Duration", value: "2.4m", change: "-12.5%", positive: true, icon: Clock },
    { label: "Active Users", value: "1,284", change: "+8.3%", positive: true, icon: Users },
    { label: "Total Executions", value: "45,892", change: "+15.7%", positive: true, icon: Activity },
  ];

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Analytics</h1>
          <p className="text-muted-foreground mt-1">Platform performance and trend analysis</p>
        </div>
        <Tabs value={timeframe} onValueChange={setTimeframe}>
          <TabsList>
            <TabsTrigger value="7d">7d</TabsTrigger>
            <TabsTrigger value="30d">30d</TabsTrigger>
            <TabsTrigger value="90d">90d</TabsTrigger>
            <TabsTrigger value="1y">1y</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat) => (
          <Card key={stat.label}>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="p-2 rounded-lg bg-primary/10"><stat.icon className="w-5 h-5 text-primary" /></div>
                <Badge variant={stat.positive ? "success" : "destructive"} className="text-xs">
                  {stat.positive ? <TrendingUp className="w-3 h-3 mr-1 inline" /> : <TrendingDown className="w-3 h-3 mr-1 inline" />}
                  {stat.change}
                </Badge>
              </div>
              <p className="text-2xl font-bold mt-3">{stat.value}</p>
              <p className="text-sm text-muted-foreground">{stat.label}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card className="lg:col-span-2">
          <CardHeader><CardTitle className="text-lg">Execution Trends</CardTitle></CardHeader>
          <CardContent>
            <div className="h-64 flex items-center justify-center bg-muted/30 rounded-lg">
              {trends.length > 0 ? (
                <div className="w-full h-full p-4">
                  <div className="flex items-end gap-2 h-full">
                    {trends.map((t: any, i: number) => (
                      <div key={i} className="flex-1 flex flex-col items-center gap-1">
                        <div className="w-full bg-primary/20 rounded-t" style={{ height: `${Math.min(100, (t.value || 0) * 10)}%` }}>
                          <div className="w-full bg-primary rounded-t" style={{ height: `${Math.min(100, (t.value || 0) * 10)}%` }} />
                        </div>
                        <span className="text-xs text-muted-foreground">{t.period?.slice(5) || t.date?.slice(5)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="text-center">
                  <BarChart3 className="w-12 h-12 mx-auto text-muted-foreground mb-2" />
                  <p className="text-muted-foreground">No trend data available</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle className="text-lg">Bottlenecks</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {insights.length > 0 ? insights.map((b: any, i: number) => (
              <div key={i} className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
                <div>
                  <p className="text-sm font-medium">{b.workflowName}</p>
                  <p className="text-xs text-muted-foreground">Step: {b.stepId}</p>
                </div>
                <Badge variant={b.severity === "HIGH" ? "destructive" : b.severity === "MEDIUM" ? "warning" : "secondary"}>
                  {b.severity}
                </Badge>
              </div>
            )) : (
              <p className="text-sm text-muted-foreground text-center py-8">No bottlenecks detected</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle className="text-lg">Top Metrics</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {[
              { label: "Avg Response Time", value: "1.2s", change: "-5%" },
              { label: "Error Rate", value: "0.8%", change: "-0.2%" },
              { label: "Throughput", value: "842/day", change: "+12%" },
              { label: "SLA Compliance", value: "99.2%", change: "+0.5%" },
            ].map((m) => (
              <div key={m.label} className="flex items-center justify-between">
                <span className="text-sm">{m.label}</span>
                <div className="text-right">
                  <p className="text-sm font-medium">{m.value}</p>
                  <p className="text-xs text-green-600">{m.change}</p>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </motion.div>
  );
}
