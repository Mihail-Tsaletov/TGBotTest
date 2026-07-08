package svaga.tgbottest.service;

import org.apache.catalina.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import svaga.tgbottest.model.Doctor;
import svaga.tgbottest.repository.DoctorRepository;

@Service
public class DoctorService {
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private DoctorRepository doctorRepository;

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
}
