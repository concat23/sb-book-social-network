package com.dev.sbbooknetwork.cloudinary;

import com.dev.sbbooknetwork.upload.UploadedFile;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("cloudinary")
@RequiredArgsConstructor
@Tag(name = "Cloudinary API", description = "Endpoints for user upload to Cloudinary")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    @Operation(summary = "Upload a file using MultipartFile to Cloudinary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/upload/multipart", consumes = { "multipart/form-data" })
    public ResponseEntity<UploadedFile> uploadMultipartFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            UploadedFile uploadedFile = cloudinaryService.uploadFileMultipartCloudinary(file);
            return ResponseEntity.ok(uploadedFile);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @Operation(summary = "Get list of uploaded files from Cloudinary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of uploaded files retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/files/cloudinary")
    public ResponseEntity<List<UploadedFile>> listUploadedFiles() {
        try {
            List<UploadedFile> uploadedFiles = cloudinaryService.listUploadedFilesFromCloudinary();
            return ResponseEntity.ok(uploadedFiles);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }


    @Operation(summary = "Get list of uploaded files DB of Cloudinary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of uploaded files retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/files")
    public ResponseEntity<List<UploadedFile>> listUploadedFilesFromDBOfCloudinary() {
        List<UploadedFile> uploadedFiles = cloudinaryService.listUploadedFilesFromDBOfCloudinary();
        return new ResponseEntity<>(uploadedFiles, HttpStatus.OK);
    }

    @Operation(summary = "Delete file from Cloudinary by publicId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/file")
    public ResponseEntity<String> deleteFileFromCloudinary(@RequestBody PublicIDRequest request) {
        String publicId = request.getPublicId();
        try {
            cloudinaryService.deleteFileFromCloudinary(publicId);

            Gson gson = new Gson();
            // Create a JSON message object
            String jsonResponse = gson.toJson(new MessageResponse("File deleted successfully from Cloudinary: " + publicId));

            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete file from Cloudinary: " + e.getMessage());
        }
    }

    @Operation(summary = "Retrieve file information from Cloudinary by publicId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/file/{nameOfFile}")
    public ResponseEntity<UploadedFile> getFileUploaded(@PathVariable String nameOfFile) {
        try {
            String publicId = "uploads/" + nameOfFile;
            System.out.println(publicId);
            UploadedFile uploadedFile = cloudinaryService.getFileUploaded(publicId);
            if (uploadedFile != null) {
                return ResponseEntity.ok(uploadedFile);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    static class MessageResponse {
        private final String message;

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }


    // Helper method to convert MultipartFile to File
    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);
        return file;
    }
}
