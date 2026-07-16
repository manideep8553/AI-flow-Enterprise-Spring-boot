import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Plus, Search, Filter, Download, FileText, Image, Archive, File } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Badge } from "../../components/ui/badge";
import { Card, CardContent } from "../../components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { formatRelativeTime, formatNumber } from "../../lib/utils";

const fileIcons: Record<string, any> = { PDF: FileText, IMAGE: Image, DOC: FileText, default: File };

export function DocumentListPage() {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState<any[]>([]);
  const [search, setSearch] = useState("");
  const [tab, setTab] = useState("all");

  useEffect(() => {
    api.getDocuments({ page: 0, size: 20, search: search || undefined, archived: tab === "archived" ? true : undefined }).then((res) => {
      setDocuments(res.content);
    }).catch(() => {});
  }, [search, tab]);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Documents</h1>
          <p className="text-muted-foreground mt-1">Upload, manage, and process documents</p>
        </div>
        <Button onClick={() => navigate("/documents/upload")}>
          <Plus className="w-4 h-4 mr-2" /> Upload Document
        </Button>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input placeholder="Search documents..." className="pl-10" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <Button variant="outline" size="icon"><Filter className="w-4 h-4" /></Button>
      </div>

      <Tabs defaultValue="all" onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="all">All Documents</TabsTrigger>
          <TabsTrigger value="recent">Recent</TabsTrigger>
          <TabsTrigger value="archived">Archived</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {documents.map((doc: any) => {
          const Icon = fileIcons[doc.documentType] || fileIcons.default;
          return (
            <Card key={doc.id} className="hover:shadow-md transition-shadow cursor-pointer" onClick={() => navigate(`/documents/${doc.id}`)}>
              <CardContent className="p-4">
                <div className="flex items-start gap-3">
                  <div className="p-2 rounded-lg bg-primary/10">
                    <Icon className="w-8 h-8 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium truncate">{doc.fileName}</p>
                    <div className="flex items-center gap-2 mt-1">
                      <Badge variant="outline" className="text-xs">{doc.documentType}</Badge>
                      <span className="text-xs text-muted-foreground">{formatNumber(doc.fileSize)} bytes</span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {formatRelativeTime(doc.uploadedAt)}
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge variant={doc.processingStatus === "COMPLETED" ? "success" : doc.processingStatus === "FAILED" ? "destructive" : "warning"} className="text-xs">
                        {doc.processingStatus}
                      </Badge>
                      {doc.archived && <Badge variant="secondary" className="text-xs">Archived</Badge>}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
        {documents.length === 0 && (
          <div className="col-span-full text-center py-12">
            <p className="text-muted-foreground">No documents found</p>
          </div>
        )}
      </div>
    </motion.div>
  );
}
