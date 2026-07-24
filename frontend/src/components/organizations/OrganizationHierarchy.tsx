import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Building2, Building, Users, FolderGit2 } from "lucide-react";
import { api } from "../../lib/api";
import { Badge } from "../ui/badge";
import { Card, CardContent } from "../ui/card";
import { ScrollArea } from "../ui/scroll-area";

export function OrganizationHierarchy({ orgId }: { orgId: string }) {
  const [org, setOrg] = useState<any>(null);
  const [businessUnits, setBusinessUnits] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [teams, setTeams] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.getOrganization(orgId),
      api.getBusinessUnits(orgId, { size: 100 }),
      api.getDepartments(orgId, { size: 100 }),
      api.getTeams("", { size: 100 }).catch(() => ({ content: [] })),
    ]).then(([orgRes, buRes, deptRes]) => {
      setOrg(orgRes);
      setBusinessUnits(buRes.content);
      setDepartments(deptRes.content);
    }).finally(() => setLoading(false));
  }, [orgId]);

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Loading hierarchy...</div>;
  }

  if (!org) {
    return <div className="text-center py-8 text-muted-foreground">Organization not found</div>;
  }

  const rootDepts = departments.filter((d: any) => !d.parentDepartmentId);
  const childDepts = departments.filter((d: any) => d.parentDepartmentId);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center gap-4 mb-6">
            <div className="p-3 rounded-lg bg-primary/10"><Building2 className="w-8 h-8 text-primary" /></div>
            <div>
              <h2 className="text-xl font-bold">{org.name}</h2>
              <p className="text-sm text-muted-foreground">{org.industry || "Organization"}</p>
            </div>
            <Badge variant={org.active ? "success" : "secondary"} className="ml-auto">{org.active ? "Active" : "Inactive"}</Badge>
          </div>
        </CardContent>
      </Card>

      <div className="space-y-6">
        {businessUnits.length > 0 && (
          <div>
            <h3 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wide">Business Units</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {businessUnits.map((bu: any) => (
                <Card key={bu.id} className="hover:shadow-md transition-shadow border-l-4 border-l-blue-500">
                  <CardContent className="p-4">
                    <div className="flex items-center gap-2 mb-1">
                      <FolderGit2 className="w-4 h-4 text-blue-500" />
                      <h4 className="font-semibold">{bu.name}</h4>
                    </div>
                    {bu.description && <p className="text-sm text-muted-foreground">{bu.description}</p>}
                    {bu.budgetCode && <p className="text-xs text-muted-foreground mt-1">Budget: {bu.budgetCode}</p>}
                    <Badge variant={bu.active ? "success" : "secondary"} className="mt-2">{bu.active ? "Active" : "Inactive"}</Badge>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        )}

        <div>
          <h3 className="text-sm font-semibold text-muted-foreground mb-3 uppercase tracking-wide">Departments</h3>
          <div className="space-y-4">
            {rootDepts.map((dept: any) => (
              <div key={dept.id}>
                <Card className="hover:shadow-md transition-shadow border-l-4 border-l-green-500">
                  <CardContent className="p-4">
                    <div className="flex items-center gap-2 mb-1">
                      <Building className="w-4 h-4 text-green-500" />
                      <h4 className="font-semibold">{dept.name}</h4>
                    </div>
                    {dept.description && <p className="text-sm text-muted-foreground">{dept.description}</p>}
                    <div className="flex items-center gap-4 mt-2 text-xs text-muted-foreground">
                      {dept.code && <span>Code: {dept.code}</span>}
                      {dept.location && <span>Location: {dept.location}</span>}
                    </div>
                    <Badge variant={dept.active ? "success" : "secondary"} className="mt-2">{dept.active ? "Active" : "Inactive"}</Badge>
                  </CardContent>
                </Card>
                {childDepts.filter((c: any) => c.parentDepartmentId === dept.id).map((childDept: any) => (
                  <Card key={childDept.id} className="ml-6 mt-2 border-l-4 border-l-emerald-400">
                    <CardContent className="p-3">
                      <div className="flex items-center gap-2">
                        <Building className="w-3 h-3 text-emerald-400" />
                        <h5 className="font-medium text-sm">{childDept.name}</h5>
                      </div>
                      {childDept.description && <p className="text-xs text-muted-foreground mt-1">{childDept.description}</p>}
                      <Badge variant={childDept.active ? "success" : "secondary"} className="mt-1">{childDept.active ? "Active" : "Inactive"}</Badge>
                    </CardContent>
                  </Card>
                ))}
              </div>
            ))}
            {rootDepts.length === 0 && <p className="text-sm text-muted-foreground">No departments yet</p>}
          </div>
        </div>
      </div>
    </motion.div>
  );
}