package com.example.backend.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.backend.repository.maintenance.MaintenanceInformationRepository;
import com.example.backend.repository.maintenance.MaintenanceStaffRepository;

@Component
@ConditionalOnProperty(name = "app.tools.db-print", havingValue = "true")
public class DbPrintRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbPrintRunner.class);

    private final MaintenanceStaffRepository staffRepo;
    private final MaintenanceInformationRepository infoRepo;

    public DbPrintRunner(MaintenanceStaffRepository staffRepo,
                         MaintenanceInformationRepository infoRepo) {
        this.staffRepo = staffRepo;
        this.infoRepo = infoRepo;
    }

    @Override
    public void run(String... args) {
        log.info("===== DB PRINT START =====");

        long staffCount = staffRepo.count();
        log.info("staff size={}", staffCount);
        staffRepo.findAll().forEach(s ->
            log.info("staff => id={}, name={}, company={}, phone={}, email={}, active={}",
                s.getStaffId(), s.getStaffName(), s.getStaffCompany(),
                s.getStaffPhone(), s.getStaffEmail(), s.getIsActive())
        );

        long ticketCount = infoRepo.count();
        log.info("ticket size={}", ticketCount);
        infoRepo.findAll().forEach(t ->
            log.info("ticket => id={}, spotId={}, type={}, status={}, priority={}, assignedStaffId={}",
                t.getTicketId(), t.getSpotId(), t.getIssueType(),
                t.getIssueStatus(), t.getIssuePriority(), t.getAssignedStaffId())
        );

        log.info("===== DB PRINT END =====");
    }
}
