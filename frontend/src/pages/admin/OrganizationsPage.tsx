import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Plus, Search, Building2, MapPin, Globe, Users, MoreVertical } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent } from "../../components/ui/card";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import { useToast } from "../../components/ui/use-toast";

export function OrganizationsPage() {
  const { toast } = useToast();
  const [orgs, setOrgs] = useState<any[]>([]);
  const [search, setSearch] = useState("");

  useEffect(() => {
    api.getOrganizations({ page: 0, size: 20, search: search || undefined }).then((res) => {
      setOrgs(res.content);
    }).catch(() => {});
  }, [search]);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Organizations</h1>
          <p className="text-muted-foreground mt-1">Manage organizational structure</p>
        </div>
        <Button><Plus className="w-4 h-4 mr-2" /> Add Organization</Button>
      </div>

      <div className="relative max-w-sm">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <Input placeholder="Search organizations..." className="pl-10" value={search} onChange={(e) => setSearch(e.target.value)} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {orgs.map((org: any) => (
          <Card key={org.id} className="hover:shadow-md transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-start justify-between">
                <div className="p-3 rounded-lg bg-primary/10"><Building2 className="w-6 h-6 text-primary" /></div>
                <Badge variant={org.active ? "success" : "secondary"}>{org.active ? "Active" : "Inactive"}</Badge>
              </div>
              <h3 className="font-semibold mt-3">{org.name}</h3>
              {org.industry && <p className="text-sm text-muted-foreground">{org.industry}</p>}
              <div className="mt-3 space-y-1 text-sm text-muted-foreground">
                {org.email && <div className="flex items-center gap-2"><Globe className="w-3 h-3" />{org.email}</div>}
                {org.phone && <div className="flex items-center gap-2"><MapPin className="w-3 h-3" />{org.phone}</div>}
                {org.employeeCount != null && <div className="flex items-center gap-2"><Users className="w-3 h-3" />{org.employeeCount} employees</div>}
              </div>
            </CardContent>
          </Card>
        ))}
        {orgs.length === 0 && (
          <div className="col-span-full text-center py-12"><p className="text-muted-foreground">No organizations found</p></div>
        )}
      </div>
    </motion.div>
  );
}
