import { useState } from "react";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import type { Organization } from "../../types/models";
import { api } from "../../lib/api";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Textarea } from "../ui/textarea";
import { Label } from "../ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "../ui/dialog";
import { useToast } from "../ui/use-toast";

const orgSchema = z.object({
  name: z.string().min(1, "Organization name is required"),
  legalName: z.string().optional(),
  registrationNumber: z.string().optional(),
  taxId: z.string().optional(),
  email: z.string().email("Invalid email").optional().or(z.literal("")),
  phone: z.string().optional(),
  website: z.string().url("Invalid URL").optional().or(z.literal("")),
  addressLine1: z.string().optional(),
  addressLine2: z.string().optional(),
  city: z.string().optional(),
  state: z.string().optional(),
  country: z.string().optional(),
  postalCode: z.string().optional(),
  description: z.string().optional(),
  industry: z.string().optional(),
  employeeCount: z.number().min(0).optional(),
  domains: z.array(z.string()).optional(),
});

type OrgFormData = z.infer<typeof orgSchema>;

interface OrganizationFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  organization?: Organization | null;
  onSuccess: () => void;
}

const industries = [
  "Technology", "Financial Services", "Healthcare", "Manufacturing",
  "Retail", "Education", "Government", "Non-Profit", "Consulting",
  "Media & Entertainment", "Energy", "Transportation & Logistics", "Other",
];

export function OrganizationForm({ open, onOpenChange, organization, onSuccess }: OrganizationFormProps) {
  const { toast } = useToast();
  const isEdit = !!organization;

  const { register, handleSubmit, setValue, watch, formState: { errors, isSubmitting } } = useForm<OrgFormData>({
    resolver: zodResolver(orgSchema),
    defaultValues: {
      name: organization?.name || "",
      legalName: organization?.legalName || "",
      registrationNumber: organization?.registrationNumber || "",
      taxId: organization?.taxId || "",
      email: organization?.email || "",
      phone: organization?.phone || "",
      website: organization?.website || "",
      addressLine1: organization?.addressLine1 || "",
      addressLine2: organization?.addressLine2 || "",
      city: organization?.city || "",
      state: organization?.state || "",
      country: organization?.country || "",
      postalCode: organization?.postalCode || "",
      description: organization?.description || "",
      industry: organization?.industry || "",
      employeeCount: organization?.employeeCount || undefined,
      domains: organization?.domains || [],
    },
  });

  const onSubmit = async (data: OrgFormData) => {
    try {
      if (isEdit && organization) {
        await api.updateOrganization(organization.id, data);
        toast({ title: "Organization updated successfully" });
      } else {
        await api.createOrganization(data);
        toast({ title: "Organization created successfully" });
      }
      onSuccess();
      onOpenChange(false);
    } catch (err: any) {
      toast({
        title: "Error",
        description: err.message || "Failed to save organization",
        variant: "destructive",
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit Organization" : "Create Organization"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label>Name *</Label>
              <Input {...register("name")} placeholder="Organization name" />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-2">
              <Label>Legal Name</Label>
              <Input {...register("legalName")} placeholder="Legal entity name" />
            </div>
            <div className="space-y-2">
              <Label>Registration Number</Label>
              <Input {...register("registrationNumber")} placeholder="Registration number" />
            </div>
            <div className="space-y-2">
              <Label>Tax ID</Label>
              <Input {...register("taxId")} placeholder="Tax identification number" />
            </div>
            <div className="space-y-2">
              <Label>Email</Label>
              <Input type="email" {...register("email")} placeholder="contact@company.com" />
              {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
            </div>
            <div className="space-y-2">
              <Label>Phone</Label>
              <Input {...register("phone")} placeholder="+1 (555) 123-4567" />
            </div>
            <div className="space-y-2">
              <Label>Website</Label>
              <Input {...register("website")} placeholder="https://company.com" />
              {errors.website && <p className="text-sm text-destructive">{errors.website.message}</p>}
            </div>
            <div className="space-y-2">
              <Label>Industry</Label>
              <Select value={watch("industry") || ""} onValueChange={(v) => setValue("industry", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select industry" />
                </SelectTrigger>
                <SelectContent>
                  {industries.map((ind) => (
                    <SelectItem key={ind} value={ind}>{ind}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Employee Count</Label>
              <Input
                type="number"
                {...register("employeeCount", { valueAsNumber: true })}
                placeholder="0"
              />
            </div>
            <div className="space-y-2">
              <Label>Country</Label>
              <Input {...register("country")} placeholder="Country" />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Address Line 1</Label>
            <Input {...register("addressLine1")} placeholder="Street address" />
          </div>
          <div className="space-y-2">
            <Label>Address Line 2</Label>
            <Input {...register("addressLine2")} placeholder="Suite, building, etc." />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label>City</Label>
              <Input {...register("city")} placeholder="City" />
            </div>
            <div className="space-y-2">
              <Label>State / Province</Label>
              <Input {...register("state")} placeholder="State" />
            </div>
            <div className="space-y-2">
              <Label>Postal Code</Label>
              <Input {...register("postalCode")} placeholder="Postal code" />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Description</Label>
            <Textarea {...register("description")} placeholder="Organization description" rows={3} />
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => onOpenChange(false)} type="button">
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Saving..." : isEdit ? "Update Organization" : "Create Organization"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
