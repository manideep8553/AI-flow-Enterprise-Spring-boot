import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Plus, Search, MoreVertical, Edit2, Trash2 } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog";
import { Label } from "../ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../ui/dropdown-menu";
import { useToast } from "../ui/use-toast";

export function TeamManagement({ orgId }: { orgId: string }) {
  const { toast } = useToast();
  const [departments, setDepartments] = useState<any[]>([]);
  const [deptId, setDeptId] = useState("");
  const [teams, setTeams] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [editDialog, setEditDialog] = useState(false);
  const [editingTeam, setEditingTeam] = useState<any>(null);
  const [newTeam, setNewTeam] = useState({ name: "", description: "", leadEmployeeId: "", email: "", slackChannel: "" });

  useEffect(() => {
    api.getDepartments(orgId, { size: 100 }).then((res) => {
      setDepartments(res.content);
      if (!deptId && res.content.length > 0) {
        setDeptId(res.content[0].id);
      }
    }).finally(() => setLoading(false));
  }, [orgId]);

  useEffect(() => {
    if (deptId) loadTeams();
  }, [deptId, page, search]);

  const loadTeams = () => {
    api.getTeams(deptId, { page, size: 20, search: search || undefined }).then((res) => {
      setTeams(res.content);
      setTotal(res.totalElements);
    });
  };

  const handleCreate = async () => {
    try {
      await api.createTeam(deptId, newTeam);
      toast({ title: "Team created" });
      setCreateDialog(false);
      setNewTeam({ name: "", description: "", leadEmployeeId: "", email: "", slackChannel: "" });
      loadTeams();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleUpdate = async () => {
    try {
      await api.updateTeam(editingTeam.id, editingTeam);
      toast({ title: "Team updated" });
      setEditDialog(false);
      setEditingTeam(null);
      loadTeams();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteTeam(id);
      toast({ title: "Team deleted" });
      loadTeams();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Loading teams...</div>;
  }

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <Label>Department:</Label>
          <Select value={deptId} onValueChange={(v) => { setDeptId(v); setPage(0); setSearch(""); }}>
            <SelectTrigger className="w-64"><SelectValue placeholder="Select department" /></SelectTrigger>
            <SelectContent>
              {departments.map((d: any) => (
                <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex-1">
          <Input placeholder="Search teams..." value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
      </div>

      <div className="flex items-center justify-between">
        <Button onClick={() => setCreateDialog(true)} disabled={!deptId}><Plus className="w-4 h-4 mr-2" /> Add Team</Button>
      </div>

      <div className="space-y-3">
        {teams.map((team: any) => (
          <Card key={team.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-4 flex items-center justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <h3 className="font-semibold">{team.name}</h3>
                  <Badge variant={team.active ? "success" : "secondary"}>{team.active ? "Active" : "Inactive"}</Badge>
                </div>
                {team.description && <p className="text-sm text-muted-foreground mt-1">{team.description}</p>}
                <div className="flex items-center gap-4 mt-2 text-xs text-muted-foreground">
                  {team.email && <span>Email: {team.email}</span>}
                  {team.slackChannel && <span>Slack: #{team.slackChannel}</span>}
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="icon" onClick={() => { setEditingTeam(team); setEditDialog(true); }}>
                  <Edit2 className="w-4 h-4" />
                </Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild><Button variant="ghost" size="icon"><MoreVertical className="w-4 h-4" /></Button></DropdownMenuTrigger>
                  <DropdownMenuContent>
                    <DropdownMenuItem onClick={() => handleDelete(team.id)} className="text-destructive">Delete Team</DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardContent>
          </Card>
        ))}
        {teams.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">No teams found{deptId ? "" : " — select a department"}</div>
        )}
      </div>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{total} total teams</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <Button variant="outline" size="sm" disabled={teams.length < 20} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      </div>

      <Dialog open={createDialog} onOpenChange={setCreateDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add Team</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2"><Label>Name *</Label><Input value={newTeam.name} onChange={(e) => setNewTeam({ ...newTeam, name: e.target.value })} /></div>
            <div className="space-y-2"><Label>Description</Label><Input value={newTeam.description} onChange={(e) => setNewTeam({ ...newTeam, description: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Lead Employee ID</Label><Input value={newTeam.leadEmployeeId} onChange={(e) => setNewTeam({ ...newTeam, leadEmployeeId: e.target.value })} /></div>
              <div className="space-y-2"><Label>Email</Label><Input value={newTeam.email} onChange={(e) => setNewTeam({ ...newTeam, email: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Slack Channel</Label><Input value={newTeam.slackChannel} onChange={(e) => setNewTeam({ ...newTeam, slackChannel: e.target.value })} /></div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialog(false)}>Cancel</Button>
            <Button onClick={handleCreate}>Create Team</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={editDialog} onOpenChange={setEditDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Edit Team</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2"><Label>Name *</Label><Input value={editingTeam?.name || ""} onChange={(e) => setEditingTeam({ ...editingTeam, name: e.target.value })} /></div>
            <div className="space-y-2"><Label>Description</Label><Input value={editingTeam?.description || ""} onChange={(e) => setEditingTeam({ ...editingTeam, description: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Lead Employee ID</Label><Input value={editingTeam?.leadEmployeeId || ""} onChange={(e) => setEditingTeam({ ...editingTeam, leadEmployeeId: e.target.value })} /></div>
              <div className="space-y-2"><Label>Email</Label><Input value={editingTeam?.email || ""} onChange={(e) => setEditingTeam({ ...editingTeam, email: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Slack Channel</Label><Input value={editingTeam?.slackChannel || ""} onChange={(e) => setEditingTeam({ ...editingTeam, slackChannel: e.target.value })} /></div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialog(false)}>Cancel</Button>
            <Button onClick={handleUpdate}>Update Team</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}