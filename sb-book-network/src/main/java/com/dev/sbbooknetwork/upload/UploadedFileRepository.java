package com.dev.sbbooknetwork.upload;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile,Integer> {
    @Query("SELECT uf FROM UploadedFile uf WHERE uf.publicId = :publicId")
    UploadedFile findByPublicId(String publicId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UploadedFile uf WHERE uf.publicId = :publicId")
    void deleteByPublicId(String publicId);

    @Query("SELECT uf FROM UploadedFile uf WHERE uf.originalFileName = :originalFileName")
    UploadedFile findByOriginalFileName(String originalFileName);
}
