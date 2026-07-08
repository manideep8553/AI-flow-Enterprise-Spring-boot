package com.aiflow.enterprise.org.dto.request;

import com.aiflow.enterprise.enums.EmployeeStatus;
import com.aiflow.enterprise.enums.EmploymentType;
import com.aiflow.enterprise.enums.Gender;
import com.aiflow.enterprise.enums.MaritalStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeProfileRequest {
    @NotBlank private String userId;
    @NotBlank private String organizationId;
    private String employeeId;
    private String departmentId;
    private List<String> teamIds;
    private List<String> businessUnitIds;
    private String designationId;
    private String reportingManagerId;
    private EmployeeStatus status;
    private EmploymentType employmentType;
    private LocalDate joiningDate;
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
    private String bio;
    private List<String> skills;
    private Map<String, Object> customFields;
}
