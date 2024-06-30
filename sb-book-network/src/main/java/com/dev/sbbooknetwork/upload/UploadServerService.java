package com.dev.sbbooknetwork.upload;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class UploadServerService {

    @Value("${application.file.uploads.directory}")
    private String uploadDirectory;

    private final UploadedFileService uploadedFileService;


    private static final Logger logger = LoggerFactory.getLogger(UploadServerService.class);

    @Transactional
    public UploadedFile uploadFile(MultipartFile file) throws IOException {
        logger.info("Starting file upload process...");

        byte[] bytes = file.getBytes();
        logger.debug("File size: {}", bytes.length);

        // Tạo đường dẫn lưu trữ file
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename(); // Sử dụng UUID để đảm bảo tên file duy nhất
        Path path = Paths.get(uploadDirectory).resolve(fileName);
        logger.debug("Saving file to path: {}", path);

        // Lưu file vào đường dẫn đã tạo
        try {
            Files.write(path, bytes);
            logger.info("File saved successfully at path: {}", path);
        } catch (IOException e) {
            logger.error("Failed to save file at path: {}", path);
            throw new IOException("Failed to store file " + fileName, e);
        }

        // Xây dựng đường dẫn URL của file trên server
        String serverDomain = getServerDomain();
        String urlFile = serverDomain + "/api/v1/uploads/" + fileName;
        logger.debug("File URL: {}", urlFile);

        // Lưu thông tin metadata của file vào cơ sở dữ liệu
        UploadedFile uploadedFile = UploadedFile.builder()
                .originalFileName(file.getOriginalFilename())
                .modifiedFileName(fileName) // Lưu tên file đã sửa đổi để đảm bảo duy nhất
                .urlFile(urlFile)
                .serverFilePath(path.toString())
                .uploadType(UploadType.SERVER)
                .fileType(file.getContentType())
                .fileSize(String.valueOf(file.getSize()))
                .publicId("/api/v1/uploads/" + fileName)
                .build();

        // Lưu metadata của file vào cơ sở dữ liệu và trả về đối tượng UploadedFile đã lưu
        uploadedFile = uploadedFileService.saveUploadedFile(uploadedFile);
        logger.info("File metadata saved to database with ID: {}", uploadedFile.getId());

        logger.info("File upload process completed successfully.");
        return uploadedFile;
    }
    public UploadedFile getFile(String fileName) throws IOException {

        Path path = Paths.get(uploadDirectory + File.separator + fileName);

        byte[] fileBytes = Files.readAllBytes(path);

        UploadedFile uploadedFile = uploadedFileService.findByOriginalFileName(fileName);

        if (uploadedFile == null) {
            throw new FileNotFoundException("File with name " + fileName + " not found in database.");
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String serverDomain = "";
        if (attributes != null) {
            serverDomain = attributes.getRequest().getScheme() + "://" +
                    attributes.getRequest().getServerName() +
                    (attributes.getRequest().getServerPort() != 80 ? ":" + attributes.getRequest().getServerPort() : "");
        }

        uploadedFile.setUrlFile(serverDomain + "/api/v1/uploads/" + fileName);

        uploadedFile.setFileBytes(fileBytes);

        return uploadedFile;
    }


    @Transactional
    public UploadedFile updateFile(String publicId, MultipartFile file) throws IOException {
        UploadedFile uploadedFile = uploadedFileService.getUploadedFile(publicId);

        // Update file content
        byte[] bytes = file.getBytes();
        Path path = Paths.get(uploadDirectory + File.separator + uploadedFile.getOriginalFileName());
        Files.write(path, bytes);

        // Update file metadata if needed
        uploadedFile.setModifiedFileName(file.getOriginalFilename()); // Modify as needed
        uploadedFile.setFileType(file.getContentType());
        uploadedFile.setFileSize(String.valueOf(file.getSize()));

        return uploadedFileService.saveUploadedFile(uploadedFile);
    }

    @Transactional
    public void deleteFile(String publicId) throws IOException {
        UploadedFile uploadedFile = uploadedFileService.getUploadedFile(publicId);
        // Delete file from filesystem
        Path path = Paths.get(uploadDirectory + File.separator + uploadedFile.getOriginalFileName());
        Files.deleteIfExists(path);

        // Delete file metadata from database
        uploadedFileService.deleteUploadedFile(uploadedFile.getPublicId());
    }

    private String getServerDomain() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest().getScheme() + "://" +
                    attributes.getRequest().getServerName() +
                    (attributes.getRequest().getServerPort() != 80 ? ":" + attributes.getRequest().getServerPort() : "");
        }
        return "";
    }
}
