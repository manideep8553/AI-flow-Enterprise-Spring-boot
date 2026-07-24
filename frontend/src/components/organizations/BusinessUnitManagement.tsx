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

export function BusinessUnitManagement({ orgId }: { orgId: string }) {
  const { toast } = useToast();
  const [bus, setBus] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [editDialog, setEditDialog] = useState(false);
  const [editingBu, setEditingBu] = useState<any>(null);
  const [newBu, setNewBu] = useState({ name: "", code: "", description: "", headEmployeeId: "", budgetCode: "" });

useEffect(() => {
    setLoading(true);
    api.getBusinessUnits(orgId, { page, size: 20 } as any).then((res) => {
      setBus(res.content);
      setTotal(res.totalElements);
    }).finally(() => setLoading(false));
  }, [page, orgId]);

  const handleCreate = async () => {
    try {
      await api.createBusinessUnit(orgId, newBu);
      toast({ title: "Business unit created" });
      setCreateDialog(false);
      setNewBu({ name: "", code: "", description: "", headEmployeeId: "", budgetCode: "" });
      setLoading(true);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleUpdate = async () => {
    try {
      await api.updateBusinessUnit(editingBu.id, editingBu);
      toast({ title: "Business unit updated" });
      setEditDialog(false);
      setEditingBu(null);
      setLoading(true);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteBusinessUnit(id);
      toast({ title: "Business unit deleted" });
      setLoading(true);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Loading business units...</div>;
  }

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input placeholder="Search business units..." className="pl-10" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Button onClick={() => setCreateDialog(true)}><Plus className="w-4 h-4 mr-2" /> Add Business Unit</Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {bus.map((bu: any) => (
          <Card key={bu.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-4">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold">{bu.name}</h3>
                <Badge variant={bu.active ? "success" : "secondary"}>{bu.active ? "Active" : "Inactive"}</Badge>
              </div>
              {bu.code && <p className="text-xs text-muted-foreground mt-1">Code: {bu.code}</p>}
              {bu.description && <p className="text-sm text-muted-foreground mt-1">{bu.description}</p>}
              <div className="flex items-center gap-2 mt-3">
                <Button variant="ghost" size="sm" onClick={() => { setEditingBu(bu); setEditDialog(true); }}><Edit2 className="w-3 h-3 mr-1" /> Edit</Button>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild><Button variant="ghost" size="sm"><MoreVertical className="w-3 h-3" /></Button></DropdownMenuTrigger>
                  <DropdownMenuContent>
                    <DropdownMenuItem onClick={() => handleDelete(bu.id)} className="text-destructive">Delete</DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardContent>
          </Card>
        ))}
        {bus.length === 0 && <div className="col-span-full text-center py-8 text-muted-foreground">No business units found</div>}
      </div>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{total} total</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <Button variant="outline" size="sm" disabled={bus.length < 20} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      </div>

      <Dialog open={createDialog} onOpenChange={setCreateDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add Business Unit</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2"><Label>Name *</Label><Input value={newBu.name} onChange={(e) => setNewBu({ ...newBu, name: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Code</Label><Input value={newBu.code} onChange={(e) => setNewBu({ ...newBu, code: e.target.value })} /></div>
              <div className="space-y-2"><Label>Budget Code</Label><Input value={newBu.budgetCode} onChange={(e) => setNewBu({ ...newBu, budgetCode: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Description</Label><Input value={newBu.description} onChange={(e) => setNewBu({ ...newBu, description: e.target.value })} /></div>
            <div className="space-y-2"><Label>Head Employee ID</Label><Input value={newBu.headEmployeeId} onChange={(e) => setNewBu({ ...newBu, headEmployeeId: e.target.value })} /></div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialog(false)}>Cancel</Button>
            <Button onClick={handleCreate}>Create</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={editDialog} onOpenChange={setEditDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Edit Business Unit</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2"><Label>Name *</Label><Input value={editingBu?.name || ""} onChange={(e) => setEditingBu({ ...editingBu, name: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Code</Label><Input value={editingBu?.code || ""} onChange={(e) => setEditingBu({ ...editingBu, code: e.target.value })} /></div>
              <div className="space-y-2"><Label>Budget Code</Label><Input value={editingBu?.budgetCode || ""} onChange={(e) => setEditingBu({ ...editingBu, budgetCode: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Description</Label><Input value={editingBu?.description || ""} onChange={(e) => setEditingBu({ ...editingBu, description: e.target.value })} /></div>
            <div className="space-y-2"><Label>Head Employee ID</Label><Input value={editingBu?.headEmployeeId || ""} onChange={(e) => setEditingBu({ ...editingBu, headEmployeeId: e.target.value })} /></div>
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