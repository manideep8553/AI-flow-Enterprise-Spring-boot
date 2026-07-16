import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Plus, Search, Filter, MoreVertical, Play, Archive, Download, Trash2 } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from "../../components/ui/dropdown-menu";
import { formatRelativeTime } from "../../lib/utils";

export function WorkflowListPage() {
  const navigate = useNavigate();
  const [workflows, setWorkflows] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("PUBLISHED");
  const [page, setPage] = useState(0);

  useEffect(() => {
    api.getWorkflows({ page, size: 20, status, search: search || undefined }).then((res) => {
      setWorkflows(res.content);
      setTotal(res.totalElements);
    }).catch(() => {});
  }, [page, status, search]);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Workflows</h1>
          <p className="text-muted-foreground mt-1">Design, manage, and monitor your automation workflows</p>
        </div>
        <Button onClick={() => navigate("/workflows/new")}>
          <Plus className="w-4 h-4 mr-2" /> Create Workflow
        </Button>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input placeholder="Search workflows..." className="pl-10" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Button variant="outline" size="icon"><Filter className="w-4 h-4" /></Button>
      </div>

      <Tabs defaultValue="PUBLISHED" onValueChange={setStatus}>
        <TabsList>
          <TabsTrigger value="PUBLISHED">Published</TabsTrigger>
          <TabsTrigger value="DRAFT">Drafts</TabsTrigger>
          <TabsTrigger value="ARCHIVED">Archived</TabsTrigger>
          <TabsTrigger value="ALL">All</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="grid gap-4">
        {workflows.map((wf: any) => (
          <Card key={wf.id} className="hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate(`/workflows/${wf.id}`)}>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                    <div className="w-5 h-5 rounded-sm border-2 border-primary" />
                  </div>
                  <div>
                    <p className="font-medium">{wf.name}</p>
                    <div className="flex items-center gap-2 mt-1">
                      <Badge variant={wf.status === "PUBLISHED" ? "success" : wf.status === "DRAFT" ? "warning" : "secondary"}>{wf.status}</Badge>
                      <span className="text-xs text-muted-foreground">v{wf.version}</span>
                      <span className="text-xs text-muted-foreground">•</span>
                      <span className="text-xs text-muted-foreground">{wf.steps?.length || 0} steps</span>
                      <span className="text-xs text-muted-foreground">•</span>
                      <span className="text-xs text-muted-foreground">{formatRelativeTime(wf.createdAt)}</span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {wf.status === "PUBLISHED" && (
                    <Button variant="outline" size="sm" onClick={(e) => { e.stopPropagation(); navigate(`/workflows/${wf.id}/execute`); }}>
                      <Play className="w-4 h-4 mr-1" /> Execute
                    </Button>
                  )}
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                      <Button variant="ghost" size="icon"><MoreVertical className="w-4 h-4" /></Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent>
                      <DropdownMenuItem>Edit</DropdownMenuItem>
                      <DropdownMenuItem>Duplicate</DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem><Download className="w-4 h-4 mr-2" /> Export</DropdownMenuItem>
                      <DropdownMenuItem><Archive className="w-4 h-4 mr-2" /> Archive</DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem className="text-destructive"><Trash2 className="w-4 h-4 mr-2" /> Delete</DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
        {workflows.length === 0 && (
          <div className="text-center py-12">
            <p className="text-muted-foreground">No workflows found</p>
          </div>
        )}
      </div>
    </motion.div>
  );
}
