package com.dev.sbbooknetwork.upload;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadedFileService {
    private final UploadedFileRepository fileRepository;

    public UploadedFile saveUploadedFile(UploadedFile uploadedFile) {
        return fileRepository.save(uploadedFile);
    }


    public UploadedFile getUploadedFile(String publicId) {
        return fileRepository.findByPublicId(publicId);
    }

    public List<UploadedFile> getAllUploadedFiles() {
        return fileRepository.findAll();
    }

    public void deleteUploadedFile(String publicId) {
        fileRepository.deleteByPublicId(publicId);
    }
}
