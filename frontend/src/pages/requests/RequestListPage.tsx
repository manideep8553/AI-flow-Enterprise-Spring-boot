import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Plus, Search, Filter, MessageSquare, CheckCircle2, XCircle, Clock, AlertTriangle } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent } from "../../components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { formatRelativeTime } from "../../lib/utils";

const statusColors: Record<string, string> = {
  DRAFT: "bg-gray-100 text-gray-800", SUBMITTED: "bg-blue-100 text-blue-800",
  PENDING_APPROVAL: "bg-yellow-100 text-yellow-800", APPROVED: "bg-green-100 text-green-800",
  REJECTED: "bg-red-100 text-red-800", CANCELLED: "bg-gray-100 text-gray-800",
  ESCALATED: "bg-orange-100 text-orange-800", COMPLETED: "bg-green-100 text-green-800",
};

export function RequestListPage() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState<any[]>([]);
  const [tab, setTab] = useState("all");
  const [search, setSearch] = useState("");

  useEffect(() => {
    const fetcher = tab === "mine" ? api.getMyRequests({ page: 0, size: 20 }) :
      tab === "approvals" ? api.getApprovals({ page: 0, size: 20 }) :
      api.getRequests({ page: 0, size: 20 });

    fetcher.then((res) => setRequests(res.content)).catch(() => {});
  }, [tab, search]);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Requests</h1>
          <p className="text-muted-foreground mt-1">Manage and track service requests</p>
        </div>
        <Button onClick={() => navigate("/requests/new")}>
          <Plus className="w-4 h-4 mr-2" /> New Request
        </Button>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input placeholder="Search requests..." className="pl-10" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Select defaultValue="">
          <SelectTrigger className="w-36"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="">All Status</SelectItem>
            <SelectItem value="DRAFT">Draft</SelectItem>
            <SelectItem value="PENDING_APPROVAL">Pending</SelectItem>
            <SelectItem value="APPROVED">Approved</SelectItem>
            <SelectItem value="REJECTED">Rejected</SelectItem>
          </SelectContent>
        </Select>
        <Button variant="outline" size="icon"><Filter className="w-4 h-4" /></Button>
      </div>

      <Tabs defaultValue="all" onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="all">All Requests</TabsTrigger>
          <TabsTrigger value="mine">My Requests</TabsTrigger>
          <TabsTrigger value="approvals">Pending Approval</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="space-y-3">
        {requests.map((req: any) => (
          <Card key={req.id} className="hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate(`/requests/${req.id}`)}>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <p className="font-medium">{req.title}</p>
                    <Badge className={statusColors[req.status]}>{req.status.replace(/_/g, " ")}</Badge>
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-sm text-muted-foreground">
                    <span>{req.requestTypeName}</span>
                    <span>•</span>
                    <span>{req.submittedByName}</span>
                    <span>•</span>
                    <span>{formatRelativeTime(req.submittedAt)}</span>
                    {req.dueDate && (
                      <>
                        <span>•</span>
                        <span className="flex items-center gap-1"><Clock className="w-3 h-3" /> Due {formatRelativeTime(req.dueDate)}</span>
                      </>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {req.status === "PENDING_APPROVAL" && (
                    <div className="flex gap-1">
                      <Button variant="ghost" size="icon" className="h-8 w-8 text-green-600"><CheckCircle2 className="w-4 h-4" /></Button>
                      <Button variant="ghost" size="icon" className="h-8 w-8 text-red-600"><XCircle className="w-4 h-4" /></Button>
                    </div>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
        {requests.length === 0 && (
          <div className="text-center py-12"><p className="text-muted-foreground">No requests found</p></div>
        )}
      </div>
    </motion.div>
  );
}
