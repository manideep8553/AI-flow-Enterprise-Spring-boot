import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Shield, Clock, UserCheck, Mail, Calendar, Globe, Hash, Save } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Label } from "../ui/label";
import { Input } from "../ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { Switch } from "../ui/switch";
import { Separator } from "../ui/separator";
import { useToast } from "../ui/use-toast";

export function OrganizationSettings({ orgId }: { orgId: string }) {
  const { toast } = useToast();
  const [settings, setSettings] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    api.getOrganizationSettings(orgId).then((res) => {
      setSettings(res);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [orgId]);

  const save = async () => {
    setSaving(true);
    try {
      await api.updateOrganizationSettings(orgId, settings);
      toast({ title: "Settings saved successfully" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    } finally {
      setSaving(false);
    }
  };

  const update = (key: string, value: any) => {
    setSettings((prev: any) => ({ ...prev, [key]: value }));
  };

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Loading settings...</div>;
  }

  if (!settings) {
    return <div className="text-center py-8 text-muted-foreground">No settings found</div>;
  }

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Shield className="w-5 h-5" /> Security & Authentication</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div><Label>Self-Registration</Label><p className="text-sm text-muted-foreground">Allow users to register themselves</p></div>
            <Switch checked={settings.allowSelfRegistration} onCheckedChange={(v) => update("allowSelfRegistration", v)} />
          </div>
          <div className="flex items-center justify-between">
            <div><Label>Email Verification</Label><p className="text-sm text-muted-foreground">Require email verification for new accounts</p></div>
            <Switch checked={settings.requireEmailVerification} onCheckedChange={(v) => update("requireEmailVerification", v)} />
          </div>
          <div className="flex items-center justify-between">
            <div><Label>Admin Approval for New Users</Label><p className="text-sm text-muted-foreground">Require admin approval for new user accounts</p></div>
            <Switch checked={settings.requireAdminApprovalForNewUsers} onCheckedChange={(v) => update("requireAdminApprovalForNewUsers", v)} />
          </div>
          <div className="flex items-center justify-between">
            <div><Label>Two-Factor Authentication</Label><p className="text-sm text-muted-foreground">Require 2FA for all users</p></div>
            <Switch checked={settings.enableTwoFactorAuth} onCheckedChange={(v) => update("enableTwoFactorAuth", v)} />
          </div>
          <div className="flex items-center justify-between">
            <div><Label>Audit Logging</Label><p className="text-sm text-muted-foreground">Enable audit trail logging</p></div>
            <Switch checked={settings.enableAuditLogging} onCheckedChange={(v) => update("enableAuditLogging", v)} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Max Failed Login Attempts</Label>
              <Input type="number" value={settings.maxFailedLoginAttempts} onChange={(e) => update("maxFailedLoginAttempts", parseInt(e.target.value) || 0)} />
            </div>
            <div className="space-y-2">
              <Label>Password Expiry (Days)</Label>
              <Input type="number" value={settings.passwordExpiryDays} onChange={(e) => update("passwordExpiryDays", parseInt(e.target.value) || 0)} />
            </div>
            <div className="space-y-2">
              <Label>Session Timeout (Minutes)</Label>
              <Input type="number" value={settings.sessionTimeoutMinutes} onChange={(e) => update("sessionTimeoutMinutes", parseInt(e.target.value) || 0)} />
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Globe className="w-5 h-5" /> Localization</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Default Timezone</Label>
              <Input value={settings.defaultTimezone || ""} onChange={(e) => update("defaultTimezone", e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Default Locale</Label>
              <Input value={settings.defaultLocale || ""} onChange={(e) => update("defaultLocale", e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label>Date Format</Label>
              <Select value={settings.dateFormat || ""} onValueChange={(v) => update("dateFormat", v)}>
                <SelectTrigger><SelectValue placeholder="Select format" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="MM/DD/YYYY">MM/DD/YYYY</SelectItem>
                  <SelectItem value="DD/MM/YYYY">DD/MM/YYYY</SelectItem>
                  <SelectItem value="YYYY-MM-DD">YYYY-MM-DD</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Time Format</Label>
              <Select value={settings.timeFormat || ""} onValueChange={(v) => update("timeFormat", v)}>
                <SelectTrigger><SelectValue placeholder="Select format" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="12h">12-Hour</SelectItem>
                  <SelectItem value="24h">24-Hour</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Start of Week</Label>
              <Select value={settings.startOfWeek || ""} onValueChange={(v) => update("startOfWeek", v)}>
                <SelectTrigger><SelectValue placeholder="Select day" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="monday">Monday</SelectItem>
                  <SelectItem value="sunday">Sunday</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Clock className="w-5 h-5" /> Working Hours & Leave Policy</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>Leave Policy</Label>
            <Select value={settings.leavePolicy || ""} onValueChange={(v) => update("leavePolicy", v)}>
              <SelectTrigger><SelectValue placeholder="Select policy" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="standard">Standard (5 days/week)</SelectItem>
                <SelectItem value="flexible">Flexible Hours</SelectItem>
                <SelectItem value="compressed">Compressed (4 days/week)</SelectItem>
                <SelectItem value="remote">Remote-First</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Work Days</Label>
              <Input value={settings.workDays || ""} onChange={(e) => update("workDays", e.target.value)} placeholder="Mon-Fri" />
            </div>
            <div className="space-y-2">
              <Label>Work Hours Start</Label>
              <Input value={settings.workHoursStart || ""} onChange={(e) => update("workHoursStart", e.target.value)} placeholder="09:00" />
            </div>
            <div className="space-y-2">
              <Label>Work Hours End</Label>
              <Input value={settings.workHoursEnd || ""} onChange={(e) => update("workHoursEnd", e.target.value)} placeholder="17:00" />
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="flex justify-end">
        <Button onClick={save} disabled={saving}>
          <Save className="w-4 h-4 mr-2" /> {saving ? "Saving..." : "Save Settings"}
        </Button>
      </div>
    </motion.div>
  );
}