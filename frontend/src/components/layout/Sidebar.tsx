import { useState } from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard, Workflow, TicketCheck, FileText, PieChart,
  Bot, Shield, ScrollText, Bell, Calendar, Users, Settings,
  ChevronLeft, ChevronRight, Search, Building2, FileSearch,
  BarChart3, Fingerprint
} from "lucide-react";
import { cn } from "../../lib/utils";
import { Button } from "../ui/button";
import { ScrollArea } from "../ui/scroll-area";

const navItems = [
  { section: "Workspace", items: [
    { to: "/", icon: LayoutDashboard, label: "Dashboard" },
    { to: "/workflows", icon: Workflow, label: "Workflows" },
    { to: "/requests", icon: TicketCheck, label: "Requests" },
    { to: "/documents", icon: FileText, label: "Documents" },
  ]},
  { section: "Intelligence", items: [
    { to: "/ai/copilot", icon: Bot, label: "AI Copilot" },
    { to: "/analytics", icon: BarChart3, label: "Analytics" },
    { to: "/fraud", icon: Fingerprint, label: "Fraud Detection" },
    { to: "/reports", icon: FileSearch, label: "Reports" },
  ]},
  { section: "Management", items: [
    { to: "/audit-logs", icon: ScrollText, label: "Audit Logs" },
    { to: "/notifications", icon: Bell, label: "Notifications" },
    { to: "/scheduler", icon: Calendar, label: "Scheduler" },
    { to: "/organizations", icon: Building2, label: "Organizations" },
  ]},
  { section: "Administration", items: [
    { to: "/users", icon: Users, label: "User Management" },
    { to: "/compliance", icon: Shield, label: "Compliance" },
    { to: "/settings", icon: Settings, label: "Settings" },
  ]},
];

export function Sidebar() {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <aside className={cn(
      "relative flex flex-col bg-sidebar text-sidebar-foreground transition-all duration-300",
      collapsed ? "w-16" : "w-64"
    )}>
      <div className="flex items-center h-16 px-4 border-b border-sidebar-muted">
        {!collapsed && (
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-sidebar-accent flex items-center justify-center">
              <Workflow className="w-5 h-5" />
            </div>
            <span className="font-bold text-lg">AI-Flow</span>
          </div>
        )}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setCollapsed(!collapsed)}
          className={cn("ml-auto text-sidebar-foreground/60 hover:text-sidebar-foreground", collapsed && "mx-auto")}
        >
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
        </Button>
      </div>

      <ScrollArea className="flex-1 px-2 py-4">
        {navItems.map((section) => (
          <div key={section.section} className="mb-4">
            {!collapsed && (
              <p className="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/40">
                {section.section}
              </p>
            )}
            <div className="space-y-1">
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => cn(
                    "flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors",
                    isActive
                      ? "bg-sidebar-accent text-white"
                      : "text-sidebar-foreground/70 hover:text-sidebar-foreground hover:bg-sidebar-muted",
                    collapsed && "justify-center px-2"
                  )}
                >
                  <item.icon className="w-5 h-5 shrink-0" />
                  {!collapsed && <span>{item.label}</span>}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </ScrollArea>
    </aside>
  );
}
