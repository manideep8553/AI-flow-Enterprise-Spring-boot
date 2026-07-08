package com.aiflow.enterprise.org.dto.response;

import com.aiflow.enterprise.entity.EmployeeProfile;
import com.aiflow.enterprise.enums.EmployeeStatus;
import com.aiflow.enterprise.enums.EmploymentType;
import com.aiflow.enterprise.enums.Gender;
import com.aiflow.enterprise.enums.MaritalStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeProfileResponse {
    private String id;
    private String userId;
    private String organizationId;
    private String employeeId;
    private String departmentId;
    private String departmentName;
    private List<String> teamIds;
    private List<String> teamNames;
    private List<String> businessUnitIds;
    private String designationId;
    private String designationTitle;
    private String reportingManagerId;
    private String reportingManagerName;
    private EmployeeStatus status;
    private EmploymentType employmentType;
    private LocalDate joiningDate;
    private LocalDate exitDate;
    private String exitReason;
    private String workEmail;
    private String workPhone;
    private String extension;
    private String officeLocation;
    private String firstName;
    private String lastName;
    private String phone;
    private String personalEmail;
    private LocalDate dateOfBirth;
    private Gender gender;
    private MaritalStatus maritalStatus;
    private String nationality;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelation;
    private String profileImageUrl;
    private String bio;
    private List<String> skills;
    private List<EmployeeProfile.Education> education;
    private List<EmployeeProfile.Certification> certifications;
    private Map<String, Object> customFields;
    private Instant createdAt;
    private Instant updatedAt;
}
