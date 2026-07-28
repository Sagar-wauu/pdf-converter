package com.pdfconverter.dto;

import lombok.Data;

@Data
public class CoverPageRequest {

    private String templateName;

    private String universityName = "Tribhuvan University";
    private String facultyName = "Faculty of Humanities and Social Science";
    private String reportLabel = "LAB REPORT ON";
    private String subjectName = "";
    private String submittedToLabel = "Submitted to";
    private String departmentName = "Department of Computer Application";
    private String campusName = "";
    private String campusAddress = "";
    private String fulfillmentLine = "In partial fulfillment of the requirements for the Bachelors in Computer Application";

    private String studentName = "";
    private String registrationNo = "";
    private String semester = "";

    private String internalExaminerName = "";
    private String internalExaminerLabel = "Internal Examineer";

    private String externalExaminerName = "";
    private String externalExaminerLabel = "External Examineer";

    // Base64-encoded PNG/JPG of the logo (optional). If left blank, the default TU logo is used.
    private String logoBase64;

    private String subjectColor = "#FF0000";
    private String campusNameColor = "#2E75B6";
}