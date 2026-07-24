import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Plus, Search, MoreVertical, Edit2, Trash2 } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import { Card, CardContent } from "../ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog";
import { Label } from "../ui/label";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../ui/dropdown-menu";
import { useToast } from "../ui/use-toast";

export function DesignationManagement({ orgId }: { orgId: string }) {
  const { toast } = useToast();
  const [designations, setDesignations] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [editDialog, setEditDialog] = useState(false);
  const [editingDesig, setEditingDesig] = useState<any>(null);
  const [newDesig, setNewDesig] = useState({ title: "", description: "", level: 1, grade: "", skills: "", careerPath: "" });

  useEffect(() => {
    setLoading(true);
    api.getDesignations(orgId, { page, size: 20, search: search || undefined }).then((res) => {
      setDesignations(res.content);
      setTotal(res.totalElements);
    }).finally(() => setLoading(false));
  }, [page, search, orgId]);

  const handleCreate = async () => {
    try {
      await api.createDesignation(orgId, {
        ...newDesig,
        skills: newDesig.skills ? newDesig.skills.split(",").map((s: string) => s.trim()) : [],
      });
      toast({ title: "Designation created" });
      setCreateDialog(false);
      setNewDesig({ title: "", description: "", level: 1, grade: "", skills: "", careerPath: "" });
      setLoading(true);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleUpdate = async () => {
    try {
      await api.updateDesignation(editingDesig.id, {
        ...editingDesig,
        skills: editingDesig.skills ? editingDesig.skills.split(",").map((s: string) => s.trim()) : [],
      });
      toast({ title: "Designation updated" });
      setEditDialog(false);
      setEditingDesig(null);
      setLoading(true);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteDesignation(id);
      toast({ title: "Designation deleted" });
      setLoading(true);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Loading designations...</div>;
  }

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input placeholder="Search designations..." className="pl-10" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Button onClick={() => setCreateDialog(true)}><Plus className="w-4 h-4 mr-2" /> Add Designation</Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {designations.map((desig: any) => (
          <Card key={desig.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-4">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold">{desig.title}</h3>
                <Badge variant={desig.active ? "success" : "secondary"}>{desig.active ? "Active" : "Inactive"}</Badge>
              </div>
              {desig.grade && <Badge variant="outline" className="mt-2">Grade: {desig.grade}</Badge>}
              {desig.description && <p className="text-sm text-muted-foreground mt-2">{desig.description}</p>}
              <div className="flex items-center gap-4 mt-3 text-xs text-muted-foreground">
                {desig.level != null && <span>Level: {desig.level}</span>}
                {desig.skills?.length > 0 && <span>Skills: {desig.skills.join(", ")}</span>}
              </div>
              <div className="flex items-center gap-2 mt-3">
                <Button variant="ghost" size="sm" onClick={() => { setEditingDesig(desig); setEditDialog(true); }}><Edit2 className="w-3 h-3 mr-1" /> Edit</Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild><Button variant="ghost" size="sm"><MoreVertical className="w-3 h-3" /></Button></DropdownMenuTrigger>
                  <DropdownMenuContent>
                    <DropdownMenuItem onClick={() => handleDelete(desig.id)} className="text-destructive">Delete</DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardContent>
          </Card>
        ))}
        {designations.length === 0 && <div className="col-span-full text-center py-8 text-muted-foreground">No designations found</div>}
      </div>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{total} total</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <Button variant="outline" size="sm" disabled={designations.length < 20} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      </div>

      <Dialog open={createDialog} onOpenChange={setCreateDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add Designation</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2"><Label>Title *</Label><Input value={newDesig.title} onChange={(e) => setNewDesig({ ...newDesig, title: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Level</Label><Input type="number" value={newDesig.level} onChange={(e) => setNewDesig({ ...newDesig, level: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2"><Label>Grade</Label><Input value={newDesig.grade} onChange={(e) => setNewDesig({ ...newDesig, grade: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Description</Label><Input value={newDesig.description} onChange={(e) => setNewDesig({ ...newDesig, description: e.target.value })} /></div>
            <div className="space-y-2"><Label>Skills (comma-separated)</Label><Input value={newDesig.skills} onChange={(e) => setNewDesig({ ...newDesig, skills: e.target.value })} /></div>
            <div className="space-y-2"><Label>Career Path</Label><Input value={newDesig.careerPath} onChange={(e) => setNewDesig({ ...newDesig, careerPath: e.target.value })} /></div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialog(false)}>Cancel</Button>
            <Button onClick={handleCreate}>Create</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={editDialog} onOpenChange={setEditDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Edit Designation</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2"><Label>Title *</Label><Input value={editingDesig?.title || ""} onChange={(e) => setEditingDesig({ ...editingDesig, title: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Level</Label><Input type="number" value={editingDesig?.level || 1} onChange={(e) => setEditingDesig({ ...editingDesig, level: parseInt(e.target.value) || 1 })} /></div>
              <div className="space-y-2"><Label>Grade</Label><Input value={editingDesig?.grade || ""} onChange={(e) => setEditingDesig({ ...editingDesig, grade: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Description</Label><Input value={editingDesig?.description || ""} onChange={(e) => setEditingDesig({ ...editingDesig, description: e.target.value })} /></div>
            <div className="space-y-2"><Label>Skills (comma-separated)</Label><Input value={editingDesig?.skills?.join(", ") || ""} onChange={(e) => setEditingDesig({ ...editingDesig, skills: e.target.value })} /></div>
            <div className="space-y-2"><Label>Career Path</Label><Input value={editingDesig?.careerPath || ""} onChange={(e) => setEditingDesig({ ...editingDesig, careerPath: e.target.value })} /></div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialog(false)}>Cancel</Button>
            <Button onClick={handleUpdate}>Update</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}