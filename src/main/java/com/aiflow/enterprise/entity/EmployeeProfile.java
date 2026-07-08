package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.enums.EmployeeStatus;
import com.aiflow.enterprise.enums.EmploymentType;
import com.aiflow.enterprise.enums.Gender;
import com.aiflow.enterprise.enums.MaritalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "employee_profiles")
@CompoundIndex(def = "{'organizationId': 1, 'employeeId': 1}", unique = true)
public class EmployeeProfile extends BaseEntity {

    @Indexed(unique = true)
    private String userId;

    @Indexed
    private String organizationId;

    @Indexed
    private String employeeId;

    @Indexed
    private String departmentId;

    private String departmentName;

    @Indexed
    private List<String> teamIds;

    private List<String> teamNames;

    @Indexed
    private List<String> businessUnitIds;

    @Indexed
    private String designationId;

    private String designationTitle;

    @Indexed
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

    private String workstation;

    /* Personal info */
    private String firstName;

    private String lastName;

    private String phone;

    private String alternatePhone;

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

    /* Emergency contact */
    private String emergencyContactName;

    private String emergencyContactPhone;

    private String emergencyContactRelation;

    /* Professional */
    private String profileImageUrl;

    private String bio;

    private List<String> skills;

    private List<Education> education;

    private List<Certification> certifications;

    private List<String> previousEmployers;

    private Map<String, Object> customFields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String degree;
        private String institution;
        private String fieldOfStudy;
        private LocalDate startDate;
        private LocalDate endDate;
        private Double grade;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Certification {
        private String name;
        private String issuer;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private String credentialUrl;
    }
}
