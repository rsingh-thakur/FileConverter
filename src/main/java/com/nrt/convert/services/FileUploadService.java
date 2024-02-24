package com.nrt.convert.services;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.nrt.convert.entity.UploadedFile;

 
public interface FileUploadService {
	String uploadFile(MultipartFile file);

	List<UploadedFile> getAllFiles();
	
	ResponseEntity<ByteArrayResource> downloadFile(Long fileId);
	
	ResponseEntity<String> convertFile(MultipartFile file);

	ResponseEntity<String> deleteFile(Long fileId);
}
