package com.dev.sbbooknetwork.upload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("files")
@Tag(name = "Uploaded Files To Server", description = "Endpoints for managing uploaded files to server")
public class UploadedFileServerController {

    private final UploadServerService uploadServerService;

    @Autowired
    public UploadedFileServerController(UploadServerService uploadServerService) {
        this.uploadServerService = uploadServerService;
    }


    @Operation(summary = "Upload a file", description = "Uploads a file to the server")
    @ApiResponse(responseCode = "200", description = "Successfully uploaded file")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PostMapping(value = "/upload", consumes = {  "multipart/form-data" })
    public ResponseEntity<UploadedFile> uploadFile(@Parameter(description = "File to upload", required = true)
                                                       @RequestParam("file") MultipartFile file) {
        try {
            UploadedFile uploadedFile = uploadServerService.uploadFile(file);
            return ResponseEntity.ok(uploadedFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "Download a file", description = "Downloads a file from the server")
    @ApiResponse(responseCode = "200", description = "Successfully downloaded file")
    @ApiResponse(responseCode = "404", description = "File not found")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        try {
            UploadedFile uploadedFile = uploadServerService.getFile(fileName);
            return ResponseEntity.ok(uploadedFile.getFileBytes());
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/update/{publicId}")
    @Operation(summary = "Update a file", description = "Updates a file on the server")
    @ApiResponse(responseCode = "200", description = "Successfully updated file")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<UploadedFile> updateFile(
            @PathVariable String publicId,
            @RequestParam("file") MultipartFile file) {
        try {
            UploadedFile updatedFile = uploadServerService.updateFile(publicId, file);
            return ResponseEntity.ok(updatedFile);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/delete/{publicId}")
    @Operation(summary = "Delete a file", description = "Deletes a file from the server")
    @ApiResponse(responseCode = "204", description = "Successfully deleted file")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<Void> deleteFile(@PathVariable String publicId) {
        try {
            uploadServerService.deleteFile(publicId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
