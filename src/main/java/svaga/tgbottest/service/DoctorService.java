package svaga.tgbottest.service;

import jakarta.transaction.Transactional;
import org.apache.catalina.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import svaga.tgbottest.model.Doctor;
import svaga.tgbottest.repository.DoctorRepository;

import java.io.IOException;

@Service
public class DoctorService {
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private DoctorRepository doctorRepository;

    @Transactional
    public void createDoctor(
            String fullName,
            MultipartFile photo,
            @RequestParam(required = false) MultipartFile video,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = new Doctor();
        doctor.setFullName(fullName);

        try {
            // Фото — обязательно
            if (photo.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Фото врача обязательно!");
                return;
            }
            String photoUrl = fileStorageService.saveFile(photo, "photos", "doctor");
            doctor.setPhotoUrl(photoUrl);

            if (video != null && !video.isEmpty()) {
                String videoUrl = fileStorageService.saveFile(video, "videos", "doctor");
                doctor.setVideoUrl(videoUrl);
            }

            doctorRepository.save(doctor);
            redirectAttributes.addFlashAttribute("success", "Врач успешно добавлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при сохранении файлов");
        }
    }

    @Transactional
    public void updateDoctor(Long id,
                                           String fullName,
                                           @RequestParam(required = false) MultipartFile photo,
                                           @RequestParam(required = false) MultipartFile video,
                                           RedirectAttributes redirectAttributes) throws IOException {

        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        doctor.setFullName(fullName);

        if (photo != null && !photo.isEmpty()) {
            String photoUrl = fileStorageService.saveFile(photo, "photos", "doctor");
            doctor.setPhotoUrl(photoUrl);
        }
        if (video != null && !video.isEmpty()) {
            String videoUrl = fileStorageService.saveFile(video, "videos", "doctor");
            doctor.setVideoUrl(videoUrl);
        }
        doctorRepository.save(doctor);
    }
}
