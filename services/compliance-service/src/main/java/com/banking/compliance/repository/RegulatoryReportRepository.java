package com.banking.compliance.repository;

import com.banking.compliance.domain.ReportStatus;
import com.banking.compliance.domain.ReportType;
import com.banking.compliance.domain.RegulatoryReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RegulatoryReportRepository extends JpaRepository<RegulatoryReport, UUID> {

    Page<RegulatoryReport> findByReportType(ReportType reportType, Pageable pageable);

    Page<RegulatoryReport> findByStatus(ReportStatus status, Pageable pageable);

    @Query("SELECT rr FROM RegulatoryReport rr WHERE rr.reportType = :reportType " +
           "AND rr.reportPeriodStart >= :startDate AND rr.reportPeriodEnd <= :endDate")
    List<RegulatoryReport> findByTypeAndPeriod(
            @Param("reportType") ReportType reportType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<RegulatoryReport> findByReportTypeAndReportPeriodStartAndReportPeriodEnd(
            ReportType reportType,
            LocalDate reportPeriodStart,
            LocalDate reportPeriodEnd
    );
}

