import { Outlet } from "react-router-dom";
import { Workflow } from "lucide-react";

export function AuthLayout() {
  return (
    <div className="min-h-screen flex">
      <div className="flex-1 flex items-center justify-center bg-background">
        <div className="w-full max-w-md px-8">
          <div className="flex items-center gap-2 mb-8 justify-center">
            <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
              <Workflow className="w-6 h-6 text-primary-foreground" />
            </div>
            <span className="font-bold text-2xl">AI-Flow Enterprise</span>
          </div>
          <Outlet />
        </div>
      </div>
      <div className="hidden lg:flex flex-1 bg-gradient-to-br from-primary/20 via-primary/10 to-background items-center justify-center p-12">
        <div className="max-w-md">
          <h2 className="text-3xl font-bold mb-4">Intelligent Workflow Automation Platform</h2>
          <p className="text-muted-foreground text-lg leading-relaxed">
            Automate, analyze, and optimize your enterprise workflows with AI-powered decision making, real-time monitoring, and comprehensive compliance management.
          </p>
        </div>
      </div>
    </div>
  );
}
