package com.pdfconverter.repository;

import com.pdfconverter.model.ConversionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversionHistoryRepository extends JpaRepository<ConversionHistory, Long> {
    List<ConversionHistory> findAllByOrderByCreatedAtDesc();
}
