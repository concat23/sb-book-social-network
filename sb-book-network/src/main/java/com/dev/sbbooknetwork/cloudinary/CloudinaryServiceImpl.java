package com.dev.sbbooknetwork.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import com.dev.sbbooknetwork.config.CloudinaryConfig;
import com.dev.sbbooknetwork.upload.UploadType;
import com.dev.sbbooknetwork.upload.UploadedFile;
import com.dev.sbbooknetwork.upload.UploadedFileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Logger log = LoggerFactory.getLogger(CloudinaryServiceImpl.class);
    private final UploadedFileService uploadedFileService;
    private final CloudinaryConfig cloudinaryConfig;
    private Cloudinary cloudinary;

    @Override
    public UploadedFile uploadFileMultipartCloudinary(MultipartFile multipartFile) throws IOException {
        initializeCloudinaryIfNeeded();
        File file = convertMultipartFileToFile(multipartFile);
        return uploadFileToCloudinary(file);
    }

    @Override
    public List<UploadedFile> listUploadedFilesFromCloudinary() {
        if (cloudinary == null) {
            throw new RuntimeException("Cloudinary initialization failed");
        }

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("type", "upload");

            ApiResponse response = cloudinary.api().resources(options);
            List<UploadedFile> uploadedFiles = new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");

            for (Map<String, Object> resource : resources) {
                UploadedFile uploadedFile = new UploadedFile();
                uploadedFile.setOriginalFileName((String) resource.get("filename"));
                uploadedFile.setUrlFile((String) resource.get("secure_url"));
                uploadedFile.setFileSize(formatFileSize((Long) resource.get("bytes")));
                uploadedFile.setFileType((String) resource.get("format"));
                uploadedFile.setPublicId((String) resource.get("public_id"));
                uploadedFiles.add(uploadedFile);
            }

            return uploadedFiles;
        } catch (Exception e) {
            log.error("Failed to list uploaded files from Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Failed to list uploaded files from Cloudinary", e);
        }
    }

    @Override
    public List<UploadedFile> listUploadedFilesFromDBOfCloudinary() {
        return uploadedFileService.getAllUploadedFiles();
    }

    @Override
    public UploadedFile getFileUploaded(String publicId) {
        return uploadedFileService.getUploadedFile(publicId);
    }

    @Override
    public void deleteFileFromCloudinary(String publicId) {
        try {
            initializeCloudinaryIfNeeded();

            if (!publicId.startsWith("uploads/")) {
                publicId = "uploads/" + publicId;
            }

            ApiResponse apiResponse = cloudinary.api().deleteResources(
                    Collections.singletonList(publicId),
                    ObjectUtils.asMap("type", "upload")
            );

            log.info("Cloudinary API Delete Response: {}", apiResponse.toString());

            UploadedFile uploadedFile = uploadedFileService.getUploadedFile(publicId);
            if (uploadedFile != null) {
                uploadedFileService.deleteUploadedFile(uploadedFile.getPublicId());
                log.info("Deleted UploadedFile from database for publicId: {}", publicId);
            } else {
                log.warn("UploadedFile not found for publicId: {}", publicId);
            }

            log.info("File deleted successfully from Cloudinary and database for publicId: {}", publicId);
        } catch (Exception exception) {
            log.error("Failed to delete file from Cloudinary: {}", exception.getMessage());
            throw new RuntimeException("Failed to delete file from Cloudinary: " + exception.getMessage());
        }
    }

    private void initializeCloudinaryIfNeeded() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(cloudinaryConfig.toMap());
        }
    }

    private UploadedFile uploadFileToCloudinary(File file) throws IOException {
        String originalFilename = file.getName();
        String modifiedFilename = customizeFilename(originalFilename);

        Map<String, Object> params = new HashMap<>();
        params.put("folder", "uploads");
        params.put("public_id", modifiedFilename);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file, params);
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            UploadedFile uploadedFile = UploadedFile.builder()
                    .originalFileName(originalFilename)
                    .modifiedFileName(modifiedFilename)
                    .urlFile((String) uploadResult.get("secure_url"))
                    .fileSize(formatFileSize(file.length()))
                    .fileType(getFileType(modifiedFilename))
                    .fileBytes(fileBytes)
                    .uploadType(UploadType.CLOUDINARY)
                    .publicId((String) uploadResult.get("public_id"))
                    .build();

            return uploadedFileService.saveUploadedFile(uploadedFile);
        } catch (IOException e) {
            log.error("Error uploading file to Cloudinary: {}", e.getMessage());
            throw e;
        }
    }

    private String customizeFilename(String originalFilename) {
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
            originalFilename = originalFilename.substring(0, lastDotIndex);
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        return originalFilename + "_" + timestamp + fileExtension;
    }

    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);
        return file;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String getFileType(String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length > 1) {
            String extension = parts[parts.length - 1];
            return switch (extension.toLowerCase()) {
                case "jpg", "jpeg", "png", "gif" -> "image";
                case "mp4", "avi", "mov", "wmv" -> "video";
                case "pdf" -> "pdf";
                case "csv" -> "csv";
                default -> "other";
            };
        }
        return "other";
    }
}
