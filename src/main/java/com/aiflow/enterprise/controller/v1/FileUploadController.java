package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.org.service.EmployeeProfileService;
import com.aiflow.enterprise.security.CustomUserDetails;
import com.aiflow.enterprise.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@Tag(name = "File Upload", description = "Profile image and file upload APIs via AWS S3")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final EmployeeProfileService employeeProfileService;

    public FileUploadController(FileStorageService fileStorageService,
                                EmployeeProfileService employeeProfileService) {
        this.fileStorageService = fileStorageService;
        this.employeeProfileService = employeeProfileService;
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload profile image to AWS S3")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String imageUrl = fileStorageService.uploadProfileImage(file, userDetails.getId());
        employeeProfileService.updateProfileImage(userDetails.getId(), imageUrl);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("imageUrl", imageUrl), "Profile image uploaded successfully"));
    }

    @PostMapping(value = "/organization-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload organization logo to AWS S3")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadOrgLogo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("organizationId") String organizationId) {
        String logoUrl = fileStorageService.uploadFile(file, "logos/" + organizationId);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("logoUrl", logoUrl), "Logo uploaded successfully"));
    }
}
