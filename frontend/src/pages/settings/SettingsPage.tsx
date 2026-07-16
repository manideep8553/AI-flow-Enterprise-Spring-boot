import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Bell, Palette, Globe, Shield, Moon, Sun, Save } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Label } from "../../components/ui/label";
import { Switch } from "../../components/ui/switch";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { Separator } from "../../components/ui/separator";
import { useToast } from "../../components/ui/use-toast";

export function SettingsPage() {
  const { toast } = useToast();
  const [preferences, setPreferences] = useState<any>({});
  const [notifPrefs, setNotifPrefs] = useState<any>({});
  const [darkMode, setDarkMode] = useState(() => document.documentElement.classList.contains("dark"));

  useEffect(() => {
    Promise.all([
      api.getUserPreferences(),
      api.getNotificationPreferences(),
    ]).then(([p, n]) => {
      setPreferences(p);
      setNotifPrefs(n);
    }).catch(() => {});
  }, []);

  const savePreferences = async () => {
    try {
      await api.updateUserPreferences(preferences);
      toast({ title: "Preferences saved" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const saveNotifPrefs = async () => {
    try {
      await api.updateNotificationPreferences(notifPrefs);
      toast({ title: "Notification preferences saved" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  const toggleDarkMode = () => {
    const newMode = !darkMode;
    setDarkMode(newMode);
    document.documentElement.classList.toggle("dark", newMode);
    localStorage.setItem("theme", newMode ? "dark" : "light");
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
        <p className="text-muted-foreground mt-1">Customize your platform experience</p>
      </div>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Palette className="w-5 h-5" /> Appearance</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <div><Label>Dark Mode</Label><p className="text-sm text-muted-foreground">Toggle dark mode theme</p></div>
            <Switch checked={darkMode} onCheckedChange={toggleDarkMode} />
          </div>
          <div className="flex items-center justify-between">
            <div><Label>Language</Label><p className="text-sm text-muted-foreground">Select your preferred language</p></div>
            <Select defaultValue={preferences.language || "en"}>
              <SelectTrigger className="w-36"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="en">English</SelectItem>
                <SelectItem value="es">Spanish</SelectItem>
                <SelectItem value="fr">French</SelectItem>
                <SelectItem value="de">German</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="flex items-center justify-between">
            <div><Label>Timezone</Label><p className="text-sm text-muted-foreground">Set your local timezone</p></div>
            <Select defaultValue={preferences.timezone || "UTC"}>
              <SelectTrigger className="w-36"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="UTC">UTC</SelectItem>
                <SelectItem value="US/Eastern">US/Eastern</SelectItem>
                <SelectItem value="US/Pacific">US/Pacific</SelectItem>
                <SelectItem value="Europe/London">Europe/London</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={savePreferences}><Save className="w-4 h-4 mr-2" /> Save Preferences</Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="flex items-center gap-2"><Bell className="w-5 h-5" /> Notifications</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {[
            { key: "email", label: "Email Notifications", desc: "Receive notifications via email" },
            { key: "push", label: "Push Notifications", desc: "Receive push notifications in browser" },
            { key: "inApp", label: "In-App Notifications", desc: "Show notifications within the app" },
            { key: "workflowUpdates", label: "Workflow Updates", desc: "Get notified about workflow changes" },
            { key: "requestUpdates", label: "Request Updates", desc: "Get notified about request updates" },
            { key: "approvalReminders", label: "Approval Reminders", desc: "Reminders for pending approvals" },
            { key: "fraudAlerts", label: "Fraud Alerts", desc: "Immediate fraud detection alerts" },
          ].map((item) => (
            <div key={item.key} className="flex items-center justify-between">
              <div><Label>{item.label}</Label><p className="text-sm text-muted-foreground">{item.desc}</p></div>
              <Switch
                checked={(notifPrefs as any)[item.key] ?? true}
                onCheckedChange={(v) => setNotifPrefs({ ...notifPrefs, [item.key]: v })}
              />
            </div>
          ))}
          <Button onClick={saveNotifPrefs}><Save className="w-4 h-4 mr-2" /> Save Notification Settings</Button>
        </CardContent>
      </Card>
    </motion.div>
  );
}
