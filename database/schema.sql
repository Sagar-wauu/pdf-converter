-- Run this once, or just let Spring Boot auto-create tables (ddl-auto=update).
-- This file is provided for reference / manual setup if you prefer.

CREATE DATABASE pdf_converter_db;

\c pdf_converter_db;

CREATE TABLE IF NOT EXISTS conversion_history (
    id BIGSERIAL PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    converted_file_name VARCHAR(255) NOT NULL,
    conversion_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cover_pages (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255),
    university_name VARCHAR(255),
    faculty_name VARCHAR(255),
    report_label VARCHAR(255),
    subject_name VARCHAR(255),
    submitted_to_label VARCHAR(255),
    department_name VARCHAR(255),
    campus_name VARCHAR(255),
    campus_address VARCHAR(255),
    fulfillment_line VARCHAR(1000),
    student_name VARCHAR(255),
    registration_no VARCHAR(255),
    semester VARCHAR(50),
    internal_examiner_name VARCHAR(255),
    internal_examiner_label VARCHAR(255),
    external_examiner_label VARCHAR(255),
    logo_path VARCHAR(500),
    subject_color VARCHAR(20) DEFAULT '#FF0000',
    campus_name_color VARCHAR(20) DEFAULT '#2E75B6',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
