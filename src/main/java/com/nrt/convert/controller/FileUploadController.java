package com.nrt.convert.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.nrt.convert.entity.UploadedFile;
import com.nrt.convert.repository.UploadedFileRepository;
import com.nrt.convert.services.FileUploadService;

@RestController
@RequestMapping("/api")
public class FileUploadController {

	@Autowired
	private FileUploadService fileUploadService;

	@RequestMapping("/upload")
	public String homePage() {
		return "upload";
	}

	@GetMapping("/")
	public ModelAndView getUploadPage(ModelAndView modelAndView) {
		modelAndView.setViewName("html/upload");
		return modelAndView;

	}

	@GetMapping("/filesListPage")
	public ModelAndView getListOfFiles(ModelAndView modelAndView) {
		modelAndView.setViewName("html/FilesList");
		return modelAndView;

	}

	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
		String message = fileUploadService.uploadFile(file);
		return ResponseEntity.ok().body(message);
	}

	@GetMapping("/files")
	public List<UploadedFile> listFiles() {
		return fileUploadService.getAllFiles();
	}

	@GetMapping("/{fileId}/download")
	public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long fileId) {
		return fileUploadService.downloadFile(fileId);

	}

	@PostMapping("/readFile")
	public ResponseEntity<String> readFile(@RequestParam("file") MultipartFile inputFile) {
		return fileUploadService.convertFile(inputFile);
	}
	
	 @GetMapping("/delete/{fileId}")
	    public ResponseEntity<String> deleteFile(@PathVariable Long fileId) {
		 return fileUploadService.deleteFile(fileId);
	           
	 }

}
