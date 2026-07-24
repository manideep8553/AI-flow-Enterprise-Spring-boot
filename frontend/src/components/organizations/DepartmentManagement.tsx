import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Plus, Search, MoreVertical, Edit2, Trash2, ToggleLeft, ToggleRight, Shield } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import { Card, CardHeader, CardTitle, CardContent } from "../ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog";
import { Label } from "../ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator } from "../ui/dropdown-menu";
import { useToast } from "../ui/use-toast";

export function DepartmentManagement({ orgId }: { orgId: string }) {
  const { toast } = useToast();
  const [departments, setDepartments] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [activeFilter, setActiveFilter] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [editDialog, setEditDialog] = useState(false);
  const [editingDept, setEditingDept] = useState<any>(null);
  const [newDept, setNewDept] = useState({ name: "", code: "", description: "", parentDepartmentId: "", costCenter: "", email: "", phone: "", location: "" });

  const loadDepartments = () => {
    setLoading(true);
    api.getDepartments(orgId, { page, size: 20, search: search || undefined, active: activeFilter ? activeFilter === "active" : undefined }).then((res) => {
      setDepartments(res.content);
      setTotal(res.totalElements);
    }).finally(() => setLoading(false));
  };

  useEffect(() => { loadDepartments(); }, [page, search, activeFilter, orgId]);

  const handleCreate = async () => {
    try {
      await api.createDepartment(orgId, newDept);
      toast({ title: "Department created" });
      setCreateDialog(false);
      setNewDept({ name: "", code: "", description: "", parentDepartmentId: "", costCenter: "", email: "", phone: "", location: "" });
      loadDepartments();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleUpdate = async () => {
    try {
      await api.updateDepartment(orgId, editingDept.id, editingDept);
      toast({ title: "Department updated" });
      setEditDialog(false);
      setEditingDept(null);
      loadDepartments();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteDepartment(orgId, id);
      toast({ title: "Department deleted" });
      loadDepartments();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleToggle = async (id: string, currentActive: boolean) => {
    try {
      await api.toggleDepartment(orgId, id);
      setDepartments((prev) => prev.map((d) => (d.id === id ? { ...d, active: !currentActive } : d)));
      toast({ title: currentActive ? "Department deactivated" : "Department activated" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Loading departments...</div>;
  }

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input placeholder="Search departments..." className="pl-10" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
          </div>
          <Select value={activeFilter} onValueChange={setActiveFilter}>
            <SelectTrigger className="w-32"><SelectValue placeholder="Status" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="">All</SelectItem>
              <SelectItem value="active">Active</SelectItem>
              <SelectItem value="inactive">Inactive</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <Button onClick={() => setCreateDialog(true)}><Plus className="w-4 h-4 mr-2" /> Add Department</Button>
      </div>

      <div className="space-y-3">
        {departments.map((dept: any) => (
          <Card key={dept.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-4 flex items-center justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <h3 className="font-semibold">{dept.name}</h3>
                  <Badge variant={dept.active ? "success" : "secondary"}>{dept.active ? "Active" : "Inactive"}</Badge>
                </div>
                {dept.code && <p className="text-sm text-muted-foreground">Code: {dept.code}</p>}
                {dept.description && <p className="text-sm text-muted-foreground mt-1">{dept.description}</p>}
                <div className="flex items-center gap-4 mt-2 text-xs text-muted-foreground">
                  {dept.location && <span>Location: {dept.location}</span>}
                  {dept.costCenter && <span>Cost Center: {dept.costCenter}</span>}
                  {dept.email && <span>Email: {dept.email}</span>}
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="ghost" size="icon" onClick={() => { setEditingDept(dept); setEditDialog(true); }}>
                  <Edit2 className="w-4 h-4" />
                </Button>
                <Button variant="ghost" size="icon" onClick={() => handleToggle(dept.id, dept.active)}>
                  {dept.active ? <ToggleRight className="w-4 h-4 text-green-600" /> : <ToggleLeft className="w-4 h-4 text-muted-foreground" />}
                </Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild><Button variant="ghost" size="icon"><MoreVertical className="w-4 h-4" /></Button></DropdownMenuTrigger>
                  <DropdownMenuContent>
                    <DropdownMenuItem onClick={() => handleDelete(dept.id)} className="text-destructive">Delete Department</DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardContent>
          </Card>
        ))}
        {departments.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">No departments found</div>
        )}
      </div>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{total} total departments</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <Button variant="outline" size="sm" disabled={departments.length < 20} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      </div>

      <Dialog open={createDialog} onOpenChange={setCreateDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add Department</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Name *</Label><Input value={newDept.name} onChange={(e) => setNewDept({ ...newDept, name: e.target.value })} /></div>
              <div className="space-y-2"><Label>Code</Label><Input value={newDept.code} onChange={(e) => setNewDept({ ...newDept, code: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Description</Label><Input value={newDept.description} onChange={(e) => setNewDept({ ...newDept, description: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Cost Center</Label><Input value={newDept.costCenter} onChange={(e) => setNewDept({ ...newDept, costCenter: e.target.value })} /></div>
              <div className="space-y-2"><Label>Location</Label><Input value={newDept.location} onChange={(e) => setNewDept({ ...newDept, location: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Email</Label><Input value={newDept.email} onChange={(e) => setNewDept({ ...newDept, email: e.target.value })} /></div>
              <div className="space-y-2"><Label>Phone</Label><Input value={newDept.phone} onChange={(e) => setNewDept({ ...newDept, phone: e.target.value })} /></div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialog(false)}>Cancel</Button>
            <Button onClick={handleCreate}>Create Department</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={editDialog} onOpenChange={setEditDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Edit Department</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Name *</Label><Input value={editingDept?.name || ""} onChange={(e) => setEditingDept({ ...editingDept, name: e.target.value })} /></div>
              <div className="space-y-2"><Label>Code</Label><Input value={editingDept?.code || ""} onChange={(e) => setEditingDept({ ...editingDept, code: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Description</Label><Input value={editingDept?.description || ""} onChange={(e) => setEditingDept({ ...editingDept, description: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Cost Center</Label><Input value={editingDept?.costCenter || ""} onChange={(e) => setEditingDept({ ...editingDept, costCenter: e.target.value })} /></div>
              <div className="space-y-2"><Label>Location</Label><Input value={editingDept?.location || ""} onChange={(e) => setEditingDept({ ...editingDept, location: e.target.value })} /></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Email</Label><Input value={editingDept?.email || ""} onChange={(e) => setEditingDept({ ...editingDept, email: e.target.value })} /></div>
              <div className="space-y-2"><Label>Phone</Label><Input value={editingDept?.phone || ""} onChange={(e) => setEditingDept({ ...editingDept, phone: e.target.value })} /></div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialog(false)}>Cancel</Button>
            <Button onClick={handleUpdate}>Update Department</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}