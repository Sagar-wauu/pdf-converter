package com.pdfconverter.controller;

import com.pdfconverter.dto.CoverPageRequest;
import com.pdfconverter.model.CoverPage;
import com.pdfconverter.repository.CoverPageRepository;
import com.pdfconverter.service.CoverPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coverpage")
@RequiredArgsConstructor
public class CoverPageController {

    private final CoverPageService coverPageService;
    private final CoverPageRepository coverPageRepository;

    /** Generates a cover page PDF from the submitted fields and streams it back for download. */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@RequestBody CoverPageRequest request) throws Exception {
        byte[] pdfBytes = coverPageService.generate(request);
        String fileName = (request.getTemplateName() == null || request.getTemplateName().isBlank()
                ? "cover-page" : request.getTemplateName().replaceAll("[^a-zA-Z0-9-_]", "_")) + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(pdfBytes);
    }

    /** Saves the field values so the user can reopen and reuse this cover page later. */
    @PostMapping("/save")
    public CoverPage save(@RequestBody CoverPageRequest r) {
        CoverPage entity = new CoverPage();
        entity.setTemplateName(r.getTemplateName());
        entity.setUniversityName(r.getUniversityName());
        entity.setFacultyName(r.getFacultyName());
        entity.setReportLabel(r.getReportLabel());
        entity.setSubjectName(r.getSubjectName());
        entity.setSubmittedToLabel(r.getSubmittedToLabel());
        entity.setDepartmentName(r.getDepartmentName());
        entity.setCampusName(r.getCampusName());
        entity.setCampusAddress(r.getCampusAddress());
        entity.setFulfillmentLine(r.getFulfillmentLine());
        entity.setStudentName(r.getStudentName());
        entity.setRegistrationNo(r.getRegistrationNo());
        entity.setSemester(r.getSemester());
        entity.setInternalExaminerName(r.getInternalExaminerName());
        entity.setInternalExaminerLabel(r.getInternalExaminerLabel());
        entity.setExternalExaminerName(r.getExternalExaminerName());
        entity.setExternalExaminerLabel(r.getExternalExaminerLabel());
        entity.setSubjectColor(r.getSubjectColor());
        entity.setCampusNameColor(r.getCampusNameColor());
        return coverPageRepository.save(entity);
    }

    @GetMapping("/saved")
    public List<CoverPage> listSaved() {
        return coverPageRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/saved/{id}")
    public CoverPage getSaved(@PathVariable Long id) {
        return coverPageRepository.findById(id).orElseThrow();
    }

    @DeleteMapping("/saved/{id}")
    public void deleteSaved(@PathVariable Long id) {
        coverPageRepository.deleteById(id);
    }
}
