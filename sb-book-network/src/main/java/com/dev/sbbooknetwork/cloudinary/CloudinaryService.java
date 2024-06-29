package com.dev.sbbooknetwork.cloudinary;

import com.dev.sbbooknetwork.upload.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface CloudinaryService {
    UploadedFile uploadFileMultipartCloudinary(MultipartFile multipartFile) throws IOException;

    void deleteFileFromCloudinary(String publicIds) throws Exception;

    List<UploadedFile> listUploadedFilesFromCloudinary();
    List<UploadedFile> listUploadedFilesFromDBOfCloudinary();

    UploadedFile getFileUploaded(String publicId);
}
