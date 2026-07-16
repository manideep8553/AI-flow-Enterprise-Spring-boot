import { useState, useRef, useEffect } from "react";
import { motion } from "framer-motion";
import { Send, Bot, User, Sparkles, Lightbulb, TrendingUp, AlertTriangle } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { ScrollArea } from "../../components/ui/scroll-area";
import { Badge } from "../../components/ui/badge";
import { Separator } from "../../components/ui/separator";

interface Message {
  role: "user" | "assistant";
  content: string;
  timestamp: Date;
}

export function AICopilotPage() {
  const [messages, setMessages] = useState<Message[]>([
    { role: "assistant", content: "Hello! I'm your AI Copilot. I can help you with workflow optimization, request analysis, fraud detection, trend insights, and more. How can I assist you today?", timestamp: new Date() },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim()) return;
    const userMsg: Message = { role: "user", content: input, timestamp: new Date() };
    setMessages((prev) => [...prev, userMsg]);
    setInput("");
    setLoading(true);
    try {
      const res = await api.aiQuery({ query: input });
      setMessages((prev) => [...prev, { role: "assistant", content: res.response, timestamp: new Date() }]);
    } catch (err: any) {
      setMessages((prev) => [...prev, { role: "assistant", content: `Error: ${err.message}`, timestamp: new Date() }]);
    } finally {
      setLoading(false);
    }
  };

  const quickActions = [
    { icon: Lightbulb, label: "Optimize Workflow", action: "Show me how to optimize my workflows" },
    { icon: TrendingUp, label: "Analyze Trends", action: "What are the current workflow trends?" },
    { icon: AlertTriangle, label: "Detect Bottlenecks", action: "Are there any bottlenecks in my workflows?" },
    { icon: Sparkles, label: "Generate Report", action: "Generate a performance report for my workflows" },
  ];

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="h-full flex flex-col">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">AI Copilot</h1>
        <p className="text-muted-foreground mt-1">Your intelligent assistant for workflow automation</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 flex-1">
        <div className="lg:col-span-3 flex flex-col">
          <Card className="flex-1 flex flex-col">
            <CardHeader className="border-b pb-3">
              <div className="flex items-center gap-2">
                <Bot className="w-5 h-5 text-primary" />
                <CardTitle className="text-lg">Conversation</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="flex-1 p-0">
              <ScrollArea ref={scrollRef} className="h-[500px] p-4">
                <div className="space-y-4">
                  {messages.map((msg, idx) => (
                    <div key={idx} className={`flex gap-3 ${msg.role === "user" ? "justify-end" : ""}`}>
                      {msg.role === "assistant" && (
                        <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                          <Bot className="w-4 h-4 text-primary" />
                        </div>
                      )}
                      <div className={`max-w-[80%] rounded-lg p-3 ${msg.role === "user" ? "bg-primary text-primary-foreground" : "bg-muted"}`}>
                        <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                        <p className="text-xs mt-1 opacity-60">{msg.timestamp.toLocaleTimeString()}</p>
                      </div>
                      {msg.role === "user" && (
                        <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
                          <User className="w-4 h-4 text-primary-foreground" />
                        </div>
                      )}
                    </div>
                  ))}
                  {loading && (
                    <div className="flex gap-3">
                      <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                        <Bot className="w-4 h-4 text-primary" />
                      </div>
                      <div className="bg-muted rounded-lg p-3">
                        <div className="flex gap-1">
                          <div className="w-2 h-2 rounded-full bg-primary animate-bounce" />
                          <div className="w-2 h-2 rounded-full bg-primary animate-bounce delay-100" />
                          <div className="w-2 h-2 rounded-full bg-primary animate-bounce delay-200" />
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </ScrollArea>
            </CardContent>
            <div className="p-4 border-t">
              <form onSubmit={(e) => { e.preventDefault(); sendMessage(); }} className="flex gap-2">
                <Input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ask the AI Copilot anything..." className="flex-1" disabled={loading} />
                <Button type="submit" disabled={loading || !input.trim()}>
                  <Send className="w-4 h-4" />
                </Button>
              </form>
            </div>
          </Card>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader><CardTitle className="text-sm">Quick Actions</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {quickActions.map((action) => (
                <Button key={action.label} variant="outline" className="w-full justify-start text-sm h-auto py-2" onClick={() => { setInput(action.action); }}>
                  <action.icon className="w-4 h-4 mr-2 shrink-0" />
                  {action.label}
                </Button>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle className="text-sm">AI Capabilities</CardTitle></CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex items-center gap-2"><Badge variant="outline" className="w-2 h-2 p-0 rounded-full bg-green-500" /> Approval Recommendations</div>
              <div className="flex items-center gap-2"><Badge variant="outline" className="w-2 h-2 p-0 rounded-full bg-green-500" /> Workflow Optimization</div>
              <div className="flex items-center gap-2"><Badge variant="outline" className="w-2 h-2 p-0 rounded-full bg-green-500" /> Bottleneck Detection</div>
              <div className="flex items-center gap-2"><Badge variant="outline" className="w-2 h-2 p-0 rounded-full bg-green-500" /> Trend Analysis</div>
              <div className="flex items-center gap-2"><Badge variant="outline" className="w-2 h-2 p-0 rounded-full bg-green-500" /> Fraud Detection</div>
              <div className="flex items-center gap-2"><Badge variant="outline" className="w-2 h-2 p-0 rounded-full bg-green-500" /> Outcome Prediction</div>
            </CardContent>
          </Card>
        </div>
      </div>
    </motion.div>
  );
}
