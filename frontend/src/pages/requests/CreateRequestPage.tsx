import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { motion } from "framer-motion";
import { ArrowLeft, Send } from "lucide-react";
import { api } from "../../lib/api";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Textarea } from "../../components/ui/textarea";
import { Card, CardContent } from "../../components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../components/ui/select";
import { useToast } from "../../components/ui/use-toast";

const schema = z.object({
  requestTypeId: z.string().min(1, "Request type is required"),
  title: z.string().min(3, "Title must be at least 3 characters"),
  description: z.string().optional(),
  priority: z.string().optional(),
});

type Form = z.infer<typeof schema>;

export function CreateRequestPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [requestTypes, setRequestTypes] = useState<any[]>([]);
  const [selectedType, setSelectedType] = useState<any>(null);

  const { register, handleSubmit, formState: { errors }, setValue, watch } = useForm<Form>({ resolver: zodResolver(schema) });
  const watchedType = watch("requestTypeId");

  useEffect(() => {
    api.getActiveRequestTypes().then(setRequestTypes).catch(() => {});
  }, []);

  useEffect(() => {
    const type = requestTypes.find((rt) => rt.id === watchedType);
    setSelectedType(type);
  }, [watchedType, requestTypes]);

  const onSubmit = async (data: Form) => {
    try {
      const res = await api.createRequest(data);
      toast({ title: "Request created" });
      navigate(`/requests/${res.id}`);
    } catch (err: any) {
      toast({ title: "Error", description: err.message, variant: "destructive" });
    }
  };

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="max-w-2xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/requests")}><ArrowLeft className="w-5 h-5" /></Button>
        <div>
          <h1 className="text-2xl font-bold">New Request</h1>
          <p className="text-muted-foreground">Submit a new service request</p>
        </div>
      </div>

      <Card>
        <CardContent className="p-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-2">
              <Label>Request Type</Label>
              <Select onValueChange={(v) => setValue("requestTypeId", v)}>
                <SelectTrigger><SelectValue placeholder="Select request type" /></SelectTrigger>
                <SelectContent>
                  {requestTypes.map((rt) => (
                    <SelectItem key={rt.id} value={rt.id}>{rt.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.requestTypeId && <p className="text-sm text-destructive">{errors.requestTypeId.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="title">Title</Label>
              <Input id="title" {...register("title")} placeholder="Brief description of your request" />
              {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" {...register("description")} placeholder="Provide details about your request" rows={4} />
            </div>

            <div className="space-y-2">
              <Label>Priority</Label>
              <Select onValueChange={(v) => setValue("priority", v)}>
                <SelectTrigger><SelectValue placeholder="Select priority" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="LOW">Low</SelectItem>
                  <SelectItem value="MEDIUM">Medium</SelectItem>
                  <SelectItem value="HIGH">High</SelectItem>
                  <SelectItem value="CRITICAL">Critical</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {selectedType?.fields?.map((field: any) => (
              <div key={field.name} className="space-y-2">
                <Label>{field.label || field.name}</Label>
                {field.type === "TEXT" && <Input {...register(`fields.${field.name}` as any)} />}
                {field.type === "SELECT" && (
                  <Select onValueChange={(v) => setValue(`fields.${field.name}` as any, v)}>
                    <SelectTrigger><SelectValue placeholder={`Select ${field.label}`} /></SelectTrigger>
                    <SelectContent>
                      {field.options?.map((opt: any) => (
                        <SelectItem key={opt.value || opt} value={opt.value || opt}>{opt.label || opt}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
                {field.type === "NUMBER" && <Input type="number" {...register(`fields.${field.name}` as any)} />}
                {field.type === "DATE" && <Input type="date" {...register(`fields.${field.name}` as any)} />}
              </div>
            ))}

            <div className="flex justify-end gap-3 pt-4">
              <Button variant="outline" type="button" onClick={() => navigate("/requests")}>Cancel</Button>
              <Button type="submit"><Send className="w-4 h-4 mr-2" /> Submit Request</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </motion.div>
  );
}
