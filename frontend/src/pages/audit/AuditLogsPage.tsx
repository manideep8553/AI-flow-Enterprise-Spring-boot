import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Search, Filter, Download, Eye, Activity } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "../../components/ui/dialog";
import { formatDateTime } from "../../lib/utils";

export function AuditLogsPage() {
  const [logs, setLogs] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [selectedLog, setSelectedLog] = useState<any>(null);
  const [search, setSearch] = useState("");
  const [actionFilter, setActionFilter] = useState("");

  useEffect(() => {
    api.getAuditLogs({ page, size: 20, action: actionFilter || undefined }).then((res) => {
      setLogs(res.content);
      setTotal(res.totalElements);
    }).catch(() => {});
  }, [page, actionFilter]);

  const actionColors: Record<string, string> = {
    CREATE: "bg-green-100 text-green-800", UPDATE: "bg-blue-100 text-blue-800",
    DELETE: "bg-red-100 text-red-800", LOGIN: "bg-purple-100 text-purple-800",
    LOGOUT: "bg-gray-100 text-gray-800", READ: "bg-gray-100 text-gray-800",
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Audit Logs</h1>
          <p className="text-muted-foreground mt-1">Track all platform activities and changes</p>
        </div>
        <Button variant="outline"><Download className="w-4 h-4 mr-2" /> Export</Button>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input placeholder="Search audit logs..." className="pl-10" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Select value={actionFilter} onValueChange={setActionFilter}>
          <SelectTrigger className="w-36"><SelectValue placeholder="Action" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="">All Actions</SelectItem>
            <SelectItem value="CREATE">Create</SelectItem>
            <SelectItem value="UPDATE">Update</SelectItem>
            <SelectItem value="DELETE">Delete</SelectItem>
            <SelectItem value="LOGIN">Login</SelectItem>
            <SelectItem value="LOGOUT">Logout</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" size="icon"><Filter className="w-4 h-4" /></Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="divide-y">
            {logs.map((log: any) => (
              <div key={log.id} className="p-4 flex items-center justify-between hover:bg-muted/50 cursor-pointer transition-colors" onClick={() => setSelectedLog(log)}>
                <div className="flex items-center gap-4">
                  <div className={`px-2 py-1 rounded text-xs font-medium ${actionColors[log.action] || "bg-gray-100 text-gray-800"}`}>
                    {log.action}
                  </div>
                  <div>
                    <p className="text-sm font-medium">{log.entityType} • {log.entityId}</p>
                    <p className="text-xs text-muted-foreground">{log.performedBy} • {log.endpoint}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-xs text-muted-foreground">{formatDateTime(log.timestamp)}</p>
                  <Badge variant={log.success ? "success" : "destructive"} className="text-xs">{log.success ? "Success" : "Failed"}</Badge>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{total} total logs</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <Button variant="outline" size="sm" disabled={logs.length < 20} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      </div>

      <Dialog open={!!selectedLog} onOpenChange={() => setSelectedLog(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>Audit Log Details</DialogTitle></DialogHeader>
          {selectedLog && (
            <div className="space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-4">
                <div><p className="text-muted-foreground">Action</p><p className="font-medium">{selectedLog.action}</p></div>
                <div><p className="text-muted-foreground">Entity</p><p className="font-medium">{selectedLog.entityType} ({selectedLog.entityId})</p></div>
                <div><p className="text-muted-foreground">Performed By</p><p className="font-medium">{selectedLog.performedBy}</p></div>
                <div><p className="text-muted-foreground">Timestamp</p><p className="font-medium">{formatDateTime(selectedLog.timestamp)}</p></div>
                <div><p className="text-muted-foreground">IP Address</p><p className="font-medium">{selectedLog.ipAddress}</p></div>
                <div><p className="text-muted-foreground">Endpoint</p><p className="font-medium">{selectedLog.httpMethod} {selectedLog.endpoint}</p></div>
              </div>
              {selectedLog.details && (
                <div><p className="text-muted-foreground mb-1">Details</p><p className="bg-muted p-3 rounded text-xs font-mono whitespace-pre-wrap">{JSON.stringify(selectedLog.details, null, 2)}</p></div>
              )}
              {selectedLog.previousValues && (
                <div><p className="text-muted-foreground mb-1">Previous Values</p><pre className="bg-muted p-3 rounded text-xs font-mono">{JSON.stringify(selectedLog.previousValues, null, 2)}</pre></div>
              )}
              {selectedLog.newValues && (
                <div><p className="text-muted-foreground mb-1">New Values</p><pre className="bg-muted p-3 rounded text-xs font-mono">{JSON.stringify(selectedLog.newValues, null, 2)}</pre></div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}
