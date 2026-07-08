package svaga.tgbottest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final String uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
        createDirectories();
    }

    private void createDirectories() {
        Path photos = Paths.get(uploadDir, "doctors/photos");
        Path videos = Paths.get(uploadDir, "doctors/videos");
        try {
            Files.createDirectories(photos);
            Files.createDirectories(videos);
        } catch (IOException e) {
            log.error("Не удалось создать директории для загрузки", e);
        }
    }

    public String saveFile(MultipartFile file, String subDir, String entityType) throws IOException {
        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String fileName = entityType + "-" + UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir, "doctors", subDir, fileName);
        Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/doctors/" + subDir + "/" + fileName;  // относительный URL
    }

    public Path getFilePath(String relativeUrl) {
        return Paths.get(uploadDir).resolve(relativeUrl.replaceFirst("^/uploads/", ""));
    }
}
