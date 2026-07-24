import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Plus, Search, Filter, MoreVertical, Edit2, Trash2, ToggleLeft, ToggleRight, ArrowRight, Building2, MapPin, Globe, Users, Activity } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent } from "../../components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "../../components/ui/dropdown-menu";
import { useToast } from "../../components/ui/use-toast";
import { formatNumber, formatDate, formatRelativeTime } from "../../lib/utils";
import { useNavigate } from "react-router-dom";

export function OrganizationsPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [orgs, setOrgs] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [search, setSearch] = useState("");
  const [industry, setIndustry] = useState("");
  const [activeFilter, setActiveFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [newOrg, setNewOrg] = useState({ name: "", legalName: "", registrationNumber: "", taxId: "", email: "", phone: "", website: "", city: "", state: "", country: "", industry: "", description: "" });

  const loadOrgs = () => {
    setLoading(true);
    api.getOrganizations({
      page,
      size,
      search: search || undefined,
      industry: industry || undefined,
      active: activeFilter ? activeFilter === "active" : undefined,
    }).then((res) => {
      setOrgs(res.content);
      setTotal(res.totalElements);
    }).catch(() => {}).finally(() => setLoading(false));
  };

  useEffect(() => { loadOrgs(); }, [page, search, industry, activeFilter]);

  const handleCreate = async () => {
    try {
      await api.createOrganization(newOrg);
      toast({ title: "Organization created" });
      setCreateDialog(false);
      setNewOrg({ name: "", legalName: "", registrationNumber: "", taxId: "", email: "", phone: "", website: "", city: "", state: "", country: "", industry: "", description: "" });
      loadOrgs();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.deleteOrganization(id);
      toast({ title: "Organization deleted" });
      loadOrgs();
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleToggle = async (id: string, currentActive: boolean) => {
    try {
      await api.toggleOrganization(id);
      setOrgs((prev) => prev.map((org: any) => (org.id === id ? { ...org, active: !currentActive } : org)));
      toast({ title: currentActive ? "Organization deactivated" : "Organization activated" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Organizations</h1>
          <p className="text-muted-foreground mt-1">Manage organizational structure, departments, and teams</p>
        </div>
        <Button onClick={() => setCreateDialog(true)}><Plus className="w-4 h-4 mr-2" /> Add Organization</Button>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input placeholder="Search organizations..." className="pl-10" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} />
        </div>
        <Select value={industry} onValueChange={(v) => { setIndustry(v); setPage(0); }}>
          <SelectTrigger className="w-44"><SelectValue placeholder="Industry" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="">All Industries</SelectItem>
            <SelectItem value="Technology">Technology</SelectItem>
            <SelectItem value="Financial Services">Financial Services</SelectItem>
            <SelectItem value="Healthcare">Healthcare</SelectItem>
            <SelectItem value="Manufacturing">Manufacturing</SelectItem>
            <SelectItem value="Retail">Retail</SelectItem>
            <SelectItem value="Education">Education</SelectItem>
            <SelectItem value="Government">Government</SelectItem>
            <SelectItem value="Other">Other</SelectItem>
          </SelectContent>
        </Select>
        <Select value={activeFilter} onValueChange={setActiveFilter}>
          <SelectTrigger className="w-36"><SelectValue placeholder="Status" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="">All</SelectItem>
            <SelectItem value="active">Active</SelectItem>
            <SelectItem value="inactive">Inactive</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {loading ? (
        <div className="text-center py-12 text-muted-foreground">Loading organizations...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {orgs.map((org: any) => (
            <Card key={org.id} className="hover:shadow-md transition-shadow cursor-pointer group" onClick={() => navigate(`/organizations/${org.id}`)}>
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="p-3 rounded-lg bg-primary/10 group-hover:bg-primary/20 transition-colors">
                    <Building2 className="w-6 h-6 text-primary" />
                  </div>
                  <Badge variant={org.active ? "success" : "secondary"}>{org.active ? "Active" : "Inactive"}</Badge>
                </div>
                <h3 className="font-semibold mt-4">{org.name}</h3>
                {org.industry && <p className="text-sm text-muted-foreground mt-1">{org.industry}</p>}
                {org.description && <p className="text-sm text-muted-foreground mt-1 line-clamp-2">{org.description}</p>}
                <div className="mt-3 space-y-1 text-sm text-muted-foreground">
                  {org.email && <div className="flex items-center gap-2"><Globe className="w-3 h-3" />{org.email}</div>}
                  {org.phone && <div className="flex items-center gap-2"><MapPin className="w-3 h-3" />{org.phone}</div>}
                  {org.employeeCount != null && <div className="flex items-center gap-2"><Users className="w-3 h-3" />{formatNumber(org.employeeCount)} employees</div>}
                  {org.city && <div className="flex items-center gap-2"><MapPin className="w-3 h-3" />{org.city}{org.country ? `, ${org.country}` : ""}</div>}
                </div>
                <div className="flex items-center justify-between mt-4 pt-4 border-t">
                  <span className="text-xs text-muted-foreground">{formatRelativeTime(org.createdAt)}</span>
                  <MoreVertical className="w-4 h-4 text-muted-foreground" />
                </div>
              </CardContent>
            </Card>
          ))}
          {orgs.length === 0 && (
            <div className="col-span-full text-center py-12">
              <p className="text-muted-foreground">No organizations found</p>
            </div>
          )}
        </div>
      )}

      {!loading && total > 0 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <p>{total} total organizations</p>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
            <span className="px-3 py-1">Page {page + 1}</span>
            <Button variant="outline" size="sm" disabled={orgs.length < size} onClick={() => setPage(page + 1)}>Next</Button>
          </div>
        </div>
      )}

      <Dialog open={createDialog} onOpenChange={setCreateDialog}>
        <DialogContent className="max-w-3xl">
          <DialogHeader><DialogTitle>Create Organization</DialogTitle></DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Name *</Label>
              <Input value={newOrg.name} onChange={(e) => setNewOrg({ ...newOrg, name: e.target.value })} placeholder="Organization name" />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Legal Name</Label>
                <Input value={newOrg.legalName} onChange={(e) => setNewOrg({ ...newOrg, legalName: e.target.value })} placeholder="Legal entity name" />
              </div>
              <div className="space-y-2">
                <Label>Registration Number</Label>
                <Input value={newOrg.registrationNumber} onChange={(e) => setNewOrg({ ...newOrg, registrationNumber: e.target.value })} placeholder="Registration number" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Tax ID</Label>
                <Input value={newOrg.taxId} onChange={(e) => setNewOrg({ ...newOrg, taxId: e.target.value })} placeholder="Tax ID" />
              </div>
              <div className="space-y-2">
                <Label>Email</Label>
                <Input type="email" value={newOrg.email} onChange={(e) => setNewOrg({ ...newOrg, email: e.target.value })} placeholder="Contact email" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Phone</Label>
                <Input value={newOrg.phone} onChange={(e) => setNewOrg({ ...newOrg, phone: e.target.value })} placeholder="Phone number" />
              </div>
              <div className="space-y-2">
                <Label>Website</Label>
                <Input value={newOrg.website} onChange={(e) => setNewOrg({ ...newOrg, website: e.target.value })} placeholder="https://company.com" />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Industry</Label>
              <Select value={newOrg.industry} onValueChange={(v) => setNewOrg({ ...newOrg, industry: v })}>
                <SelectTrigger><SelectValue placeholder="Select industry" /></SelectTrigger>
                <SelectContent>
                  {["Technology", "Financial Services", "Healthcare", "Manufacturing", "Retail", "Education", "Government", "Other"].map((ind) => (
                    <SelectItem key={ind} value={ind}>{ind}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>City</Label>
                <Input value={newOrg.city} onChange={(e) => setNewOrg({ ...newOrg, city: e.target.value })} placeholder="City" />
              </div>
              <div className="space-y-2">
                <Label>Country</Label>
                <Input value={newOrg.country} onChange={(e) => setNewOrg({ ...newOrg, country: e.target.value })} placeholder="Country" />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <textarea className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" value={newOrg.description} onChange={(e) => setNewOrg({ ...newOrg, description: e.target.value })} placeholder="Organization description" rows={3} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateDialog(false)}>Cancel</Button>
            <Button onClick={handleCreate}>Create Organization</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </motion.div>
  );
}