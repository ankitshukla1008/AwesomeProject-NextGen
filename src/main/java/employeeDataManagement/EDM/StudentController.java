package employeeDataManagement.EDM;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    @Autowired
    private StudentService studentService;

    @GetMapping("/")
    public List<Student> getAllStudents() {
        return studentService.getAllStudents();
    }

    @GetMapping("/{id}")
    public Optional<Student> getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id);
    }

    @PostMapping("/")
    public Student createStudent(@RequestBody Student student) {
        return studentService.createOrUpdateStudent(student);
    }

    @PutMapping("/{id}")
    public Student updateStudent(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        return studentService.createOrUpdateStudent(student);
    }

    @DeleteMapping("/{id}")
    public void deleteStudentById(@PathVariable Long id) {
        studentService.deleteStudentById(id);
    }

    @GetMapping("/html")
    public String getHtmlContent() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/stddata.html");
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
    
    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/{id}/upload")
    public String uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "Failed to upload file: file is empty";
        }
        try {
            String directory = "uploads";
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String filePath = dir.getAbsolutePath() + File.separator + id + "_" + file.getOriginalFilename();
            file.transferTo(new File(filePath));

            // Update originalFilename in Student entity
            Student student = studentRepository.findById(id).orElse(null);
            if (student != null) {
                student.setOriginalFilename(file.getOriginalFilename());
                studentRepository.save(student);
            }

            return "File uploaded successfully";
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to upload file: " + e.getMessage();
        }
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<byte[]> viewFile(@PathVariable Long id) throws IOException {
        Student student = studentRepository.findById(id).orElse(null);
        if (student != null && student.getOriginalFilename() != null) {
            String directory = "uploads";
            String filePath = directory + File.separator + id + "_" + student.getOriginalFilename();
            File file = new File(filePath);
            if (file.exists()) {
                byte[] fileContent = FileUtils.readFileToByteArray(file);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF); // Change to the appropriate media type
                headers.setContentDispositionFormData(student.getOriginalFilename(), student.getOriginalFilename());
                headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
                ResponseEntity<byte[]> response = new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
                return response;
            }
        }
        return ResponseEntity.notFound().build();
    }
}

