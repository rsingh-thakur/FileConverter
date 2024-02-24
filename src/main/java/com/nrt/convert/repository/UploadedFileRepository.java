package com.nrt.convert.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nrt.convert.entity.UploadedFile;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
}
