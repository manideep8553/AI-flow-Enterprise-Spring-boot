import { useState, useCallback, useRef, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ReactFlow, {
  addEdge, Background, Controls, MiniMap, useNodesState, useEdgesState,
  type Connection, type Edge, type Node, type NodeTypes, MarkerType
} from "reactflow";
import "reactflow/dist/style.css";
import { motion } from "framer-motion";
import { Save, Play, ArrowLeft, Settings, Plus, Trash2 } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent } from "../../components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { useToast } from "../../components/ui/use-toast";

const stepTypeColors: Record<string, string> = {
  APPROVAL: "#3b82f6", NOTIFICATION: "#8b5cf6", CONDITION: "#f59e0b",
  DELAY: "#6b7280", TASK: "#10b981", EMAIL: "#ec4899",
  WEBHOOK: "#6366f1", AI_DECISION: "#a855f7", DOCUMENT_PROCESSING: "#14b8a6",
  CALCULATION: "#f97316", TRANSFORMATION: "#84cc16", TERMINATE: "#ef4444",
};

function WorkflowStepNode({ data }: { data: any }) {
  return (
    <div className="px-4 py-2 shadow-md rounded-lg border-2 min-w-[160px]" style={{ borderColor: stepTypeColors[data.stepType] || "#6b7280", background: "white" }}>
      <div className="flex items-center gap-2">
        <div className="w-3 h-3 rounded-full" style={{ background: stepTypeColors[data.stepType] || "#6b7280" }} />
        <span className="text-sm font-medium">{data.label}</span>
      </div>
      <p className="text-xs text-gray-500 mt-1">{data.stepType}</p>
    </div>
  );
}

const nodeTypes: NodeTypes = { workflowStep: WorkflowStepNode };

export function WorkflowBuilderPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [saving, setSaving] = useState(false);
  const reactFlowWrapper = useRef<HTMLDivElement>(null);

  const onConnect = useCallback((params: Connection) => {
    setEdges((eds) => addEdge({ ...params, markerEnd: { type: MarkerType.ArrowClosed } }, eds));
  }, [setEdges]);

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }, []);

  const onDrop = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    const type = event.dataTransfer.getData("application/reactflow");
    if (!type || !reactFlowWrapper.current) return;
    const bounds = reactFlowWrapper.current.getBoundingClientRect();
    const position = { x: event.clientX - bounds.left - 80, y: event.clientY - bounds.top - 20 };
    const newId = `step-${Date.now()}`;
    const newNode: Node = {
      id: newId,
      type: "workflowStep",
      position,
      data: { label: `New ${type}`, stepType: type },
    };
    setNodes((nds) => nds.concat(newNode));
  }, [setNodes]);

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
  }, []);

  const addStep = (type: string) => {
    const id = `step-${Date.now()}`;
    const newNode: Node = {
      id,
      type: "workflowStep",
      position: { x: 100 + (nodes.length * 20), y: 100 + (nodes.length * 20) },
      data: { label: `New ${type}`, stepType: type },
    };
    setNodes((nds) => nds.concat(newNode));
  };

  const saveWorkflow = async () => {
    setSaving(true);
    try {
      const workflowData = {
        name,
        description,
        steps: nodes.map((n) => ({
          stepId: n.id,
          name: n.data.label,
          type: n.data.stepType,
          order: 0,
        })),
        status: "DRAFT",
      };
      if (id) {
        await api.updateWorkflow(id, workflowData);
        toast({ title: "Workflow updated" });
      } else {
        await api.createWorkflow(workflowData);
        toast({ title: "Workflow created" });
        navigate("/workflows");
      }
    } catch (err: any) {
      toast({ title: "Error saving workflow", description: err.message, variant: "destructive" });
    } finally {
      setSaving(false);
    }
  };

  const stepTypes = ["APPROVAL", "NOTIFICATION", "CONDITION", "DELAY", "TASK", "EMAIL", "WEBHOOK", "AI_DECISION", "DOCUMENT_PROCESSING", "CALCULATION", "TRANSFORMATION", "TERMINATE"];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="h-full flex flex-col">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate("/workflows")}><ArrowLeft className="w-5 h-5" /></Button>
          <div className="space-y-1">
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Workflow Name" className="text-lg font-bold border-none bg-transparent px-0 h-auto w-64" />
            <Input value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Description (optional)" className="text-sm text-muted-foreground border-none bg-transparent px-0 h-auto w-96" />
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={saveWorkflow} disabled={saving}>
            <Save className="w-4 h-4 mr-2" /> {saving ? "Saving..." : "Save"}
          </Button>
          <Button onClick={() => {}}>
            <Play className="w-4 h-4 mr-2" /> Execute
          </Button>
        </div>
      </div>

      <div className="flex flex-1 gap-4">
        <div className="w-48 space-y-2">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">Step Types</p>
          {stepTypes.map((type) => (
            <div
              key={type}
              draggable
              onDragStart={(e) => e.dataTransfer.setData("application/reactflow", type)}
              className="flex items-center gap-2 p-2 rounded-lg bg-card border cursor-grab hover:bg-accent transition-colors text-sm"
            >
              <div className="w-2 h-2 rounded-full" style={{ background: stepTypeColors[type] }} />
              {type.replace(/_/g, " ")}
            </div>
          ))}
        </div>

        <div ref={reactFlowWrapper} className="flex-1 rounded-lg border bg-card" onDrop={onDrop} onDragOver={onDragOver}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            nodeTypes={nodeTypes}
            fitView
            attributionPosition="bottom-left"
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        </div>

        {selectedNode && (
          <Card className="w-72">
            <CardContent className="p-4 space-y-4">
              <div className="flex items-center justify-between">
                <p className="font-medium text-sm">Step Settings</p>
                <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => {
                  setNodes((nds) => nds.filter((n) => n.id !== selectedNode.id));
                  setEdges((eds) => eds.filter((e) => e.source !== selectedNode.id && e.target !== selectedNode.id));
                  setSelectedNode(null);
                }}>
                  <Trash2 className="w-3 h-3 text-destructive" />
                </Button>
              </div>
              <div className="space-y-2">
                <Label>Name</Label>
                <Input value={selectedNode.data.label} onChange={(e) => {
                  setNodes((nds) => nds.map((n) => n.id === selectedNode.id ? { ...n, data: { ...n.data, label: e.target.value } } : n));
                  setSelectedNode({ ...selectedNode, data: { ...selectedNode.data, label: e.target.value } });
                }} />
              </div>
              <div className="space-y-2">
                <Label>Type</Label>
                <Select value={selectedNode.data.stepType} onValueChange={(val) => {
                  setNodes((nds) => nds.map((n) => n.id === selectedNode.id ? { ...n, data: { ...n.data, stepType: val } } : n));
                  setSelectedNode({ ...selectedNode, data: { ...selectedNode.data, stepType: val } });
                }}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {stepTypes.map((t) => <SelectItem key={t} value={t}>{t.replace(/_/g, " ")}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </motion.div>
  );
}
