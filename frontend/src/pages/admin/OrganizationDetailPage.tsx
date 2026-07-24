import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { useParams, useNavigate } from "react-router-dom";
import { Building2, Users, FolderGit2, Shield, Settings, GraduationCap, Briefcase, ArrowLeft } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "../../components/ui/tabs";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent } from "../../components/ui/card";
import { useToast } from "../../components/ui/use-toast";
import { OrganizationSettings } from "../../components/organizations/OrganizationSettings";
import { DepartmentManagement } from "../../components/organizations/DepartmentManagement";
import { TeamManagement } from "../../components/organizations/TeamManagement";
import { BusinessUnitManagement } from "../../components/organizations/BusinessUnitManagement";
import { DesignationManagement } from "../../components/organizations/DesignationManagement";
import { EmployeeManagement } from "../../components/organizations/EmployeeManagement";
import { OrganizationHierarchy } from "../../components/organizations/OrganizationHierarchy";
import { OrganizationForm } from "../../components/organizations/OrganizationForm";
import { formatRelativeTime } from "../../lib/utils";

export function OrganizationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const orgId = id || "";
  const { toast } = useToast();
  const [org, setOrg] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [editDialog, setEditDialog] = useState(false);

  const loadOrg = () => {
    if (!orgId) return;
    setLoading(true);
    api.getOrganization(orgId).then((res) => {
      setOrg(res);
      setLoading(false);
    }).catch(() => setLoading(false));
  };

  useEffect(() => { loadOrg(); }, [orgId]);

  const handleDelete = async () => {
    try {
      await api.deleteOrganization(orgId);
      toast({ title: "Organization deleted" });
      navigate("/organizations");
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const handleToggle = async () => {
    try {
      await api.toggleOrganization(orgId);
      setOrg((prev: any) => ({ ...prev, active: !prev.active }));
      toast({ title: org?.active ? "Organization deactivated" : "Organization activated" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  if (!orgId) {
    return (
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Organizations</h1>
          <p className="text-muted-foreground mt-1">Select an organization to manage</p>
        </div>
      </motion.div>
    );
  }

  if (loading) {
    return <div className="text-center py-12 text-muted-foreground">Loading organization...</div>;
  }

  if (!org) {
    return <div className="text-center py-12 text-muted-foreground">Organization not found</div>;
  }

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="outline" onClick={() => navigate("/organizations")}><ArrowLeft className="w-4 h-4 mr-2" />Back</Button>
          <div className="p-3 rounded-lg bg-primary/10"><Building2 className="w-6 h-6 text-primary" /></div>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">{org.name}</h1>
            <p className="text-muted-foreground">{org.industry || "Organization"}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant={org.active ? "success" : "secondary"} className="text-sm px-3 py-1">{org.active ? "Active" : "Inactive"}</Badge>
          <Button variant="outline" onClick={() => setEditDialog(true)}>Edit</Button>
          <Button variant="destructive" onClick={handleDelete}>Delete</Button>
        </div>
      </div>

      <Tabs defaultValue="departments">
        <TabsList className="w-full justify-start">
          <TabsTrigger value="departments"><Users className="w-4 h-4 mr-2" />Departments</TabsTrigger>
          <TabsTrigger value="teams"><Briefcase className="w-4 h-4 mr-2" />Teams</TabsTrigger>
          <TabsTrigger value="business-units"><FolderGit2 className="w-4 h-4 mr-2" />Business Units</TabsTrigger>
          <TabsTrigger value="designations"><GraduationCap className="w-4 h-4 mr-2" />Designations</TabsTrigger>
          <TabsTrigger value="employees"><Users className="w-4 h-4 mr-2" />Employees</TabsTrigger>
          <TabsTrigger value="hierarchy"><Shield className="w-4 h-4 mr-2" />Hierarchy</TabsTrigger>
          <TabsTrigger value="settings"><Settings className="w-4 h-4 mr-2" />Settings</TabsTrigger>
        </TabsList>

        <TabsContent value="departments" className="mt-4">
          <DepartmentManagement orgId={orgId} />
        </TabsContent>

        <TabsContent value="teams" className="mt-4">
          <TeamManagement orgId={orgId} />
        </TabsContent>

        <TabsContent value="business-units" className="mt-4">
          <BusinessUnitManagement orgId={orgId} />
        </TabsContent>

        <TabsContent value="designations" className="mt-4">
          <DesignationManagement orgId={orgId} />
        </TabsContent>

        <TabsContent value="employees" className="mt-4">
          <EmployeeManagement orgId={orgId} />
        </TabsContent>

        <TabsContent value="hierarchy" className="mt-4">
          <OrganizationHierarchy orgId={orgId} />
        </TabsContent>

        <TabsContent value="settings" className="mt-4">
          <OrganizationSettings orgId={orgId} />
        </TabsContent>
      </Tabs>

      <OrganizationForm open={editDialog} onOpenChange={setEditDialog} organization={org} onSuccess={() => loadOrg()} />
    </motion.div>
  );
}