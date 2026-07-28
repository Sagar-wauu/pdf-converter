package com.pdfconverter.repository;

import com.pdfconverter.model.CoverPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoverPageRepository extends JpaRepository<CoverPage, Long> {
    List<CoverPage> findAllByOrderByCreatedAtDesc();
}
