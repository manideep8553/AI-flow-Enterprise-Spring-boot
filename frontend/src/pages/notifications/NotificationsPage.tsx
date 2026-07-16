import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Bell, CheckCheck, Info, AlertTriangle, AlertCircle, CheckCircle2, Trash2 } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { formatRelativeTime } from "../../lib/utils";

const typeIcons: Record<string, any> = {
  INFO: Info, WARNING: AlertTriangle, ERROR: AlertCircle, SUCCESS: CheckCircle2,
};

export function NotificationsPage() {
  const [notifications, setNotifications] = useState<any[]>([]);
  const [tab, setTab] = useState("all");
  const [stats, setStats] = useState<any>({});

  useEffect(() => {
    Promise.all([
      api.getNotifications({ page: 0, size: 50, read: tab === "unread" ? false : undefined }),
      api.getNotificationStats(),
    ]).then(([res, s]) => {
      setNotifications(res.content);
      setStats(s);
    }).catch(() => {});
  }, [tab]);

  const markAsRead = async (id: string) => {
    await api.markNotificationRead(id);
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Notifications</h1>
          <p className="text-muted-foreground mt-1">Stay updated with platform activity</p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="secondary" className="text-sm">{stats.unreadCount || 0} unread</Badge>
          <Button variant="outline" size="sm"><CheckCheck className="w-4 h-4 mr-2" /> Mark All Read</Button>
        </div>
      </div>

      <Tabs defaultValue="all" onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="all">All</TabsTrigger>
          <TabsTrigger value="unread">Unread ({stats.unreadCount || 0})</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="space-y-2">
        {notifications.map((notif: any) => {
          const Icon = typeIcons[notif.type] || Bell;
          return (
            <Card key={notif.id} className={`hover:shadow-md transition-shadow ${!notif.read ? "border-l-4 border-l-primary bg-primary/5" : ""}`}>
              <CardContent className="p-4 flex items-start gap-4" onClick={() => markAsRead(notif.id)}>
                <div className={`p-2 rounded-full ${!notif.read ? "bg-primary/10" : "bg-muted"}`}>
                  <Icon className={`w-5 h-5 ${!notif.read ? "text-primary" : "text-muted-foreground"}`} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <p className={`font-medium ${!notif.read ? "text-foreground" : "text-muted-foreground"}`}>{notif.title}</p>
                    <span className="text-xs text-muted-foreground shrink-0">{formatRelativeTime(notif.createdAt)}</span>
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">{notif.message}</p>
                </div>
              </CardContent>
            </Card>
          );
        })}
        {notifications.length === 0 && (
          <div className="text-center py-12">
            <Bell className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No notifications</p>
          </div>
        )}
      </div>
    </motion.div>
  );
}
