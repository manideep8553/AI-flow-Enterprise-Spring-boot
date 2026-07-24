import { useState, useEffect, useCallback } from "react";
import { motion } from "framer-motion";
import { Plus, Search, MoreVertical, Edit2, Trash2, ArrowUpDown } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Badge } from "../ui/badge";
import { Card, CardContent } from "../ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog";
import { Label } from "../ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../ui/dropdown-menu";
import { useToast } from "../ui/use-toast";
import { getStatusColor } from "../../lib/utils";

export function EmployeeManagement({ orgId }: { orgId: string }) {
  const { toast } = useToast();
  const [employees, setEmployees] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [designations, setDesignations] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [editDialog, setEditDialog] = useState(false);
  const [editingEmp, setEditingEmp] = useState<any>(null);
  const [newEmp, setNewEmp] = useState({ userId: "", firstName: "", lastName: "", workEmail: "", departmentId: "", designationId: "", status: "ACTIVE", employmentType: "FULL_TIME", joiningDate: "" });

  const loadAll = useCallback(() => {
    setLoading(true);
    Promise.all([
      api.getEmployees(orgId, { page, size: 20, search: search || undefined, departmentId: deptFilter || undefined, status: statusFilter || undefined }),
      api.getDepartments(orgId, { size: 100 }),
      api.getDesignations(orgId, { size: 100 }),
    ]).then(([empRes, deptRes, desigRes]) => {
      setEmployees(empRes.content);
      setTotal(empRes.totalElements);
      setDepartments(deptRes.content);
      setDesignations(desigRes.content);
    }).finally(() => setLoading(false));
  }, [page, search, deptFilter, statusFilter, orgId]);

  useEffect(() => { loadAll(); }, [loadAll]);

  const handleCreate = async () => {
    try {
      await api.createEmployee(orgId, newEmp);
      toast({ title: "Employee created" });
      setCreateDialog(false);
      setNewEmp({ userId: "", firstName: "", lastName: "", workEmail: "", departmentId: "", designationId: "", status: "ACTIVE", employmentType: "FULL_TIME", joiningDate: "" });
      loadAll();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleUpdate = async () => {
    try {
      await api.updateEmployee(orgId, editingEmp.id, editingEmp);
      toast({ title: "Employee updated" });
      setEditDialog(false);
      setEditingEmp(null);
      loadAll();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteEmployee(id);
      toast({ title: "Employee deleted" });
      loadAll();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleStatusChange = async (id: string, newStatus: string) => {
    try {
      await api.updateEmployeeStatus(id, newStatus);
      setEmployees((prev) => prev.map((e: any) => (e.id === id ? { ...e, status: newStatus } : e)));
      toast({ title: `Employee status changed to ${newStatus}` });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Loading employees...</div>;
  }

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4 flex-wrap">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input placeholder="Search employees..." className="pl-10" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
          </div>
          <Select value={deptFilter} onValueChange={(v) => { setDeptFilter(v); setPage(0); }}>
            <SelectTrigger className="w-44"><SelectValue placeholder="Department" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="">All Departments</SelectItem>
              {departments.map((d: any) => (
                <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select value={statusFilter} onValueChange={(v) => { setStatusFilter(v); setPage(0); }}>
            <SelectTrigger className="w-36"><SelectValue placeholder="Status" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="">All Status</SelectItem>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="INACTIVE">Inactive</SelectItem>
              <SelectItem value="TERMINATED">Terminated</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <Button onClick={() => setCreateDialog(true)}><Plus className="w-4 h-4 mr-2" /> Add Employee</Button>
      </div>

      <Card>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b">
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Employee</th>
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Department</th>
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Designation</th>
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Status</th>
                  <th className="text-left p-3 text-xs font-medium text-muted-foreground">Joined</th>
                  <th className="text-right p-3 text-xs font-medium text-muted-foreground">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {employees.map((emp: any) => (
                  <tr key={emp.id} className="hover:bg-muted/50 transition-colors">
                    <td className="p-3">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-xs font-medium text-primary">
                          {(emp.firstName?.[0] || "")}{(emp.lastName?.[0] || "")}
                        </div>
                        <div>
                          <p className="font-medium text-sm">{emp.firstName} {emp.lastName}</p>
                          <p className="text-xs text-muted-foreground">{emp.workEmail}</p>
                        </div>
                      </div>
                    </td>
                    <td className="p-3 text-sm">{emp.departmentName || "-"}</td>
                    <td className="p-3 text-sm">{emp.designationTitle || "-"}</td>
                    <td className="p-3">
                      <select className="text-xs border rounded px-2 py-1 bg-transparent" value={emp.status} onChange={(e) => handleStatusChange(emp.id, e.target.value)}>
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                        <option value="TERMINATED">Terminated</option>
                      </select>
                    </td>
                    <td className="p-3 text-sm text-muted-foreground">{emp.joiningDate ? new Date(emp.joiningDate).toLocaleDateString() : "-"}</td>
                    <td className="p-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button variant="ghost" size="icon" onClick={() => { setEditingEmp(emp); setEditDialog(true); }}>
                          <Edit2 className="w-3 h-3" />
                        </Button>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild><Button variant="ghost" size="icon"><MoreVertical className="w-3 h-3" /></Button></DropdownMenuTrigger>
                          <DropdownMenuContent>
                            <DropdownMenuItem onClick={() => handleDelete(emp.id)} className="text-destructive">Delete</DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {employees.length === 0 && <div className="text-center py-8 text-muted-foreground">No employees found</div>}
        </CardContent>
      </Card>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>{total} total employees</p>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
          <Button variant="outline" size="sm" disabled={employees.length < 20} onClick={() => setPage(page + 1)}>Next</Button>
        </div>
      </div>

      <Dialog open={createDialog} onOpenChange={setCreateDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add Employee</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>First Name *</Label><Input value={newEmp.firstName} onChange={(e) => setNewEmp({ ...newEmp, firstName: e.target.value })} /></div>
              <div className="space-y-2"><Label>Last Name *</Label><Input value={newEmp.lastName} onChange={(e) => setNewEmp({ ...newEmp, lastName: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Work Email *</Label><Input type="email" value={newEmp.workEmail} onChange={(e) => setNewEmp({ ...newEmp, workEmail: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Department</Label><Select value={newEmp.departmentId} onValueChange={(v) => setNewEmp({ ...newEmp, departmentId: v })}><SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger><SelectContent>{departments.map((d: any) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}</SelectContent></Select></div>
              <div className="space-y-2"><Label>Designation</Label><Select value={newEmp.designationId} onValueChange={(v) => setNewEmp({ ...newEmp, designationId: v })}><SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger><SelectContent>{designations.map((d: any) => <SelectItem key={d.id} value={d.id}>{d.title}</SelectItem>)}</SelectContent></Select></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Status</Label><Select value={newEmp.status} onValueChange={(v) => setNewEmp({ ...newEmp, status: v })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ACTIVE">Active</SelectItem><SelectItem value="INACTIVE">Inactive</SelectItem><SelectItem value="TERMINATED">Terminated</SelectItem></SelectContent></Select></div>
              <div className="space-y-2"><Label>Employment Type</Label><Select value={newEmp.employmentType} onValueChange={(v) => setNewEmp({ ...newEmp, employmentType: v })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="FULL_TIME">Full Time</SelectItem><SelectItem value="PART_TIME">Part Time</SelectItem><SelectItem value="CONTRACT">Contract</SelectItem></SelectContent></Select></div>
            </div>
            <div className="space-y-2"><Label>Joining Date</Label><Input type="date" value={newEmp.joiningDate} onChange={(e) => setNewEmp({ ...newEmp, joiningDate: e.target.value })} /></div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialog(false)}>Cancel</Button>
            <Button onClick={handleCreate}>Create</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={editDialog} onOpenChange={setEditDialog}>
        <DialogContent>
          <DialogHeader><DialogTitle>Edit Employee</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>First Name</Label><Input value={editingEmp?.firstName || ""} onChange={(e) => setEditingEmp({ ...editingEmp, firstName: e.target.value })} /></div>
              <div className="space-y-2"><Label>Last Name</Label><Input value={editingEmp?.lastName || ""} onChange={(e) => setEditingEmp({ ...editingEmp, lastName: e.target.value })} /></div>
            </div>
            <div className="space-y-2"><Label>Work Email</Label><Input value={editingEmp?.workEmail || ""} onChange={(e) => setEditingEmp({ ...editingEmp, workEmail: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Department</Label><Select value={editingEmp?.departmentId || ""} onValueChange={(v) => setEditingEmp({ ...editingEmp, departmentId: v })}><SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger><SelectContent>{departments.map((d: any) => <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>)}</SelectContent></Select></div>
              <div className="space-y-2"><Label>Designation</Label><Select value={editingEmp?.designationId || ""} onValueChange={(v) => setEditingEmp({ ...editingEmp, designationId: v })}><SelectTrigger><SelectValue placeholder="Select" /></SelectTrigger><SelectContent>{designations.map((d: any) => <SelectItem key={d.id} value={d.id}>{d.title}</SelectItem>)}</SelectContent></Select></div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2"><Label>Status</Label><Select value={editingEmp?.status || ""} onValueChange={(v) => setEditingEmp({ ...editingEmp, status: v })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ACTIVE">Active</SelectItem><SelectItem value="INACTIVE">Inactive</SelectItem><SelectItem value="TERMINATED">Terminated</SelectItem></SelectContent></Select></div>
              <div className="space-y-2"><Label>Employment Type</Label><Select value={editingEmp?.employmentType || ""} onValueChange={(v) => setEditingEmp({ ...editingEmp, employmentType: v })}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="FULL_TIME">Full Time</SelectItem><SelectItem value="PART_TIME">Part Time</SelectItem><SelectItem value="CONTRACT">Contract</SelectItem></SelectContent></Select></div>
            </div>
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