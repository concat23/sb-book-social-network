package com.dev.sbbooknetwork.upload;

import com.dev.sbbooknetwork.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@SuperBuilder
@RequiredArgsConstructor
@Entity
public class UploadedFile extends BaseEntity {

    private String originalFileName;
    private String modifiedFileName;
    private String urlFile;
    private String serverFilePath;
    private UploadType uploadType;
    private String fileType;
    @Lob
    @Column(columnDefinition="LONGBLOB")
    private byte[] fileBytes;

    private String fileSize;
    private String publicId;

}
