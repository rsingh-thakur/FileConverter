package com.nrt.convert.serviceImpl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nrt.convert.entity.UploadedFile;
import com.nrt.convert.repository.UploadedFileRepository;
import com.nrt.convert.services.FileUploadService;

@Service
public class FileUploadServiceImpl implements FileUploadService {

	@Autowired
	private UploadedFileRepository fileRepository;

	public String uploadFile(MultipartFile file) {
		try {
			UploadedFile uploadedFile = new UploadedFile();
			uploadedFile.setFileName(file.getOriginalFilename());
			uploadedFile.setFileContent(file.getBytes()); // Save file content as byte array

			fileRepository.save(uploadedFile);

			return "File uploaded successfully: " + file.getOriginalFilename();
		} catch (IOException e) {
			return "Failed to upload file: " + e.getMessage();
		}
	}

	public List<UploadedFile> getAllFiles() {
		return fileRepository.findAll();
	}

	@Override
	public ResponseEntity<ByteArrayResource> downloadFile(Long fileId) {
		// Retrieve the file from the database
		UploadedFile file = fileRepository.findById(fileId).orElse(null);

		if (file == null) {
			return ResponseEntity.notFound().build();
		}

		// Prepare the file for download
		ByteArrayResource resource = new ByteArrayResource(file.getFileContent());

		// Specify the filename with the desired extension
		String filename = file.getFileName().replace(".cosmo", ".dem");
		System.out.print(filename);

		// Set the headers for the response
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDispositionFormData("attachment", file.getFileName());
		headers.setContentLength(file.getFileContent().length);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(resource);
	}

	@Override
	public ResponseEntity<String> convertFile(MultipartFile inputFile) {
		String sampleFilePath = "C:\\Users\\lenovo\\Desktop\\Files\\sampleFile\\starch_c000.orcacosmo";
		String destinationFilePath = "C:\\Users\\lenovo\\Desktop\\Files\\convertedFiles\\"; // Specify the destination
																							// file path

		try {
			// Read the content from the sample file
			BufferedReader sampleReader = new BufferedReader(new FileReader(sampleFilePath));
			StringBuilder sampleContent = new StringBuilder();
			String line;
			boolean firstLine = true; // Track if it's the first line
			while ((line = sampleReader.readLine()) != null) {
				// Update the first line with the input file name replacing the word before
				// colon
				if (firstLine) {
					line = line.replaceFirst("^[^:]+", inputFile.getOriginalFilename().replace(".cosmo", ""));
					firstLine = false; // Update the flag
				}
				sampleContent.append(line).append("\n");
			}
			// Close the sample file reader
			sampleReader.close();

			// Find the line containing "Total energy [a.u.]" in the input file
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputFile.getInputStream()));
			String energyLine = null;
			String dielectricEnergyLine = null; // Line containing Dielectric energy
			while ((line = inputReader.readLine()) != null) {
				if (line.contains("Total energy [a.u.]")) {
					energyLine = line.trim();
				} else if (line.contains("Dielectric energy [a.u.]")) {
					dielectricEnergyLine = line.trim();
				}
				if (energyLine != null && dielectricEnergyLine != null) {
					break;
				}
			}
			// Close the input file reader
			inputReader.close();

			// Extract the numbers from the energy lines
			String energyNumber = energyLine.split("=")[1].trim();
			String dielectricEnergyNumber = dielectricEnergyLine.split("=")[1].trim();

			// Replace the line containing "FINAL SINGLE POINT ENERGY" in the sample file
			// with the extracted number
			sampleContent = new StringBuilder(sampleContent.toString().replaceAll("FINAL SINGLE POINT ENERGY.+",
					"FINAL SINGLE POINT ENERGY     " + energyNumber));

			// Replace the line containing "CPCM dielectric energy" in the sample file with
			// the extracted number
			sampleContent = new StringBuilder(sampleContent.toString().replaceAll(".+# CPCM dielectric energy",
					"\s\s\s\s\s" + dielectricEnergyNumber + "\t\s# CPCM dielectric energy"));

			// Find the line containing "area= 1944.23" in the input file
			BufferedReader inputReader3 = new BufferedReader(new InputStreamReader(inputFile.getInputStream()));
			String areaLine = null;
			while ((line = inputReader3.readLine()) != null) {
				if (line.contains("area=")) {
					areaLine = line.trim();
					break;
				}
			}
			// Close the input file reader
			inputReader3.close();

			// If the area line is found, extract the area value and replace it in the
			// sample file
			if (areaLine != null) {
				String areaValue = areaLine.split("=")[1].trim();
				// Replace the line containing the area value in the sample file
				sampleContent = new StringBuilder(sampleContent.toString().replaceAll(".+# Area",
						"\s\s\s\s\s" + areaValue + " \t\s" + " # Area"));
			}

			BufferedReader inputReader4 = new BufferedReader(new InputStreamReader(inputFile.getInputStream()));
			String volumeLine = null;
			while ((line = inputReader4.readLine()) != null) {
				if (line.contains("volume=")) {
					volumeLine = line.trim();
					break;
				}
			}
			// Close the input file reader
			inputReader4.close();

			// If the area line is found, extract the area value and replace it in the
			// sample file
			if (volumeLine != null) {
				String volumeValue = volumeLine.split("=")[1].trim();
				// Replace the line containing the area value in the sample file
				sampleContent = new StringBuilder(sampleContent.toString().replaceAll(".+# Volume",
						"\s\s\s\s\s" + volumeValue + " \t\s" + " # Volume"));
			}

			// Find the lines containing coordinates from COSMO calculation in the input
			// file
			BufferedReader inputReader5 = new BufferedReader(new InputStreamReader(inputFile.getInputStream()));
			StringBuilder coordinatesContent = new StringBuilder();
			boolean foundCoordinatesStart = false;
			boolean endReached = false;
			while ((line = inputReader5.readLine()) != null && !endReached) {
				if (!foundCoordinatesStart) {
					if (line.contains("coordinates from COSMO calculation")) {
						foundCoordinatesStart = true;
					}
				} else {
					// Check if line starts with a specific string indicating the end
					if (line.startsWith("end")) {
						endReached = true;
						// We've reached the end, so don't append this line
						continue;
					}
					coordinatesContent.append(line).append("\n");
				}
			}
			// Close the input file reader
			inputReader5.close();

			// Replace the coordinates section placeholder in the sample content with the
			// extracted coordinates
			sampleContent = new StringBuilder(
					sampleContent.toString().replaceAll("CORDINATESTOREPLACE", coordinatesContent.toString()));

			// Create the destination file with the same filename as the input file
			String originalFilename = inputFile.getOriginalFilename();
			File destinationFile = new File(destinationFilePath + originalFilename.replace(".cosmo", ".orcacosmo"));

			// Write the modified content to the destination file
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile))) {
				writer.write(sampleContent.toString());
			}

			// Create a FileSystemResource for the destination file and return it
			String fileName = new FileSystemResource(destinationFile).getFilename();

			// save to db
			UploadedFile uploadedFile = new UploadedFile();
			uploadedFile.setFileName(fileName);
			uploadedFile.setFileContent(readFileToByteArray(destinationFile)); // Save file content as byte array
			uploadedFile.setLocalPath(destinationFilePath);
			fileRepository.save(uploadedFile);

			return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + ".dem").body("File converted successfully "+fileName);

		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body("Error occurred while modifying the file.");
		}
	}

	private byte[] readFileToByteArray(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		try {
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum);
			}
		} finally {
			if (fis != null) {
				fis.close();
			}
			bos.close();
		}
		return bos.toByteArray();
	}
	
	

	@Override
	public ResponseEntity<String> deleteFile(Long fileId) {
		if (fileRepository.existsById(fileId)) {
			// Attempt to delete the file
			try {
				fileRepository.deleteById(fileId);
			} catch (Exception e) {
				e.getStackTrace();
				return ResponseEntity.badRequest().body("Failed to delete file with ID " + fileId + ".");
			}
			// File deletion successful
			return ResponseEntity.ok().body("File with ID " + fileId + " has been deleted successfully.");
		} else {
			// File not found
			return ((BodyBuilder) ResponseEntity.notFound()).body("File with ID " + fileId + " not found.");
		}
	}

}
