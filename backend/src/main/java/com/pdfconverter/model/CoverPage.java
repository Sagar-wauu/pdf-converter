package com.pdfconverter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores every editable field that appears on the cover (front) page,
 * modeled after the Tribhuvan University lab report cover page sample.
 * Saving these lets a person re-open and re-generate the same cover
 * page later without retyping everything.
 */
@Entity
@Table(name = "cover_pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoverPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A friendly name so the user can find this saved cover page again
    private String templateName;

    private String universityName;
    private String facultyName;
    private String reportLabel;      // e.g. "LAB REPORT ON"
    private String subjectName;      // e.g. "Cloud Computing (CACS402)"
    private String submittedToLabel; // e.g. "Submitted to"
    private String departmentName;
    private String campusName;
    private String campusAddress;

    @Column(length = 1000)
    private String fulfillmentLine;

    private String studentName;
    private String registrationNo;
    private String semester;

    private String internalExaminerName;
    private String internalExaminerLabel; // e.g. "Internal Examineer"

    private String externalExaminerName;
    private String externalExaminerLabel; // e.g. "External Examineer"

    // Optional: base64 or file path of a logo image the user uploaded
    private String logoPath;

    // Hex colors so the person can theme it (defaults mimic the sample)
    private String subjectColor = "#FF0000";
    private String campusNameColor = "#2E75B6";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
