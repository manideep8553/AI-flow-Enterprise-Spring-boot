import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { User, Mail, Shield, Calendar, Lock, Camera, Save } from "lucide-react";
import { api } from "../../lib/api";
import { useAuth } from "../../hooks/useAuth";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "../../components/ui/avatar";
import { Separator } from "../../components/ui/separator";
import { Badge } from "../../components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { getInitials, formatDateTime, formatRelativeTime } from "../../lib/utils";
import { useToast } from "../../components/ui/use-toast";

export function ProfilePage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [profile, setProfile] = useState<any>(null);
  const [sessions, setSessions] = useState<any[]>([]);

  useEffect(() => {
    Promise.all([
      api.getUser(user?.id || ""),
      api.getSessions(),
    ]).then(([u, s]) => {
      setProfile(u);
      setSessions(s);
    }).catch(() => {});
  }, [user?.id]);

  const revokeSession = async (sessionId: string) => {
    try {
      await api.revokeSession(sessionId);
      setSessions((prev) => prev.filter((s) => s.id !== sessionId));
      toast({ title: "Session revoked" });
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Profile</h1>
        <p className="text-muted-foreground mt-1">Manage your account settings and preferences</p>
      </div>

      <Card>
        <CardContent className="p-6">
          <div className="flex items-center gap-6">
            <div className="relative">
              <Avatar className="w-20 h-20">
                <AvatarImage src={profile?.avatarUrl} />
                <AvatarFallback className="text-2xl bg-primary/10 text-primary">
                  {getInitials(profile?.firstName, profile?.lastName) || "U"}
                </AvatarFallback>
              </Avatar>
              <Button variant="outline" size="icon" className="absolute -bottom-1 -right-1 h-7 w-7 rounded-full">
                <Camera className="w-3 h-3" />
              </Button>
            </div>
            <div>
              <h2 className="text-xl font-bold">{profile?.firstName} {profile?.lastName}</h2>
              <div className="flex items-center gap-2 mt-1">
                <Mail className="w-4 h-4 text-muted-foreground" />
                <span className="text-muted-foreground">{profile?.email}</span>
              </div>
              <div className="flex items-center gap-2 mt-2">
                <Badge>{profile?.role}</Badge>
                <Badge variant={profile?.active ? "success" : "secondary"}>{profile?.active ? "Active" : "Inactive"}</Badge>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Tabs defaultValue="details">
        <TabsList>
          <TabsTrigger value="details">Personal Details</TabsTrigger>
          <TabsTrigger value="security">Security</TabsTrigger>
          <TabsTrigger value="sessions">Active Sessions ({sessions.length})</TabsTrigger>
        </TabsList>

        <TabsContent value="details">
          <Card>
            <CardContent className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>First Name</Label>
                  <Input defaultValue={profile?.firstName} />
                </div>
                <div className="space-y-2">
                  <Label>Last Name</Label>
                  <Input defaultValue={profile?.lastName} />
                </div>
                <div className="space-y-2">
                  <Label>Username</Label>
                  <Input defaultValue={profile?.username} />
                </div>
                <div className="space-y-2">
                  <Label>Email</Label>
                  <Input defaultValue={profile?.email} />
                </div>
                <div className="space-y-2">
                  <Label>Department</Label>
                  <Input defaultValue={profile?.department} />
                </div>
              </div>
              <Button><Save className="w-4 h-4 mr-2" /> Save Changes</Button>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security">
          <Card>
            <CardContent className="p-6 space-y-6">
              <div className="space-y-4">
                <h3 className="font-semibold">Change Password</h3>
                <div className="grid grid-cols-1 gap-4 max-w-md">
                  <div className="space-y-2">
                    <Label>Current Password</Label>
                    <Input type="password" />
                  </div>
                  <div className="space-y-2">
                    <Label>New Password</Label>
                    <Input type="password" />
                  </div>
                  <div className="space-y-2">
                    <Label>Confirm New Password</Label>
                    <Input type="password" />
                  </div>
                  <Button><Lock className="w-4 h-4 mr-2" /> Update Password</Button>
                </div>
              </div>
              <Separator />
              <div className="space-y-2">
                <h3 className="font-semibold">Two-Factor Authentication</h3>
                <p className="text-sm text-muted-foreground">Add an extra layer of security to your account.</p>
                <Button variant="outline">Enable 2FA</Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="sessions">
          <Card>
            <CardContent className="p-0">
              <div className="divide-y">
                {sessions.map((session: any) => (
                  <div key={session.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-2 h-2 rounded-full bg-green-500" />
                      <div>
                        <p className="font-medium text-sm">{session.deviceName || "Unknown Device"}</p>
                        <p className="text-xs text-muted-foreground">{session.ipAddress} • Last active {formatRelativeTime(session.lastActivityAt)}</p>
                      </div>
                    </div>
                    <Button variant="outline" size="sm" className="text-destructive" onClick={() => revokeSession(session.id)}>Revoke</Button>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </motion.div>
  );
}
