package com.example.backend;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.backend.repository.maintenance.MaintenanceInformationRepository;
import com.example.backend.repository.maintenance.MaintenanceStaffRepository;

@SpringBootTest
class DbSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(DbSmokeTest.class);

    @Autowired MaintenanceStaffRepository staffRepo;
    @Autowired MaintenanceInformationRepository infoRepo;

    @Test
    void db_print_all() {
        log.info("staff size={}", staffRepo.count());
        staffRepo.findAll().forEach(s ->
            log.info("staff => id={}, name={}, company={}, phone={}, email={}, active={}",
                s.getStaffId(), s.getStaffName(), s.getStaffCompany(),
                s.getStaffPhone(), s.getStaffEmail(), s.getIsActive()
            )
        );

        log.info("ticket size={}", infoRepo.count());
        infoRepo.findAll().forEach(t ->
            log.info("ticket => id={}, spotId={}, type={}, status={}, priority={}, assignedStaffId={}",
                t.getTicketId(), t.getSpotId(), t.getIssueType(),
                t.getIssueStatus(), t.getIssuePriority(), t.getAssignedStaffId()
            )
        );
    }
}
