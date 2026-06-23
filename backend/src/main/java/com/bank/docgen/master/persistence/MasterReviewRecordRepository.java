package com.bank.docgen.master.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterReviewRecordRepository extends JpaRepository<MasterReviewRecordEntity, UUID> {

    List<MasterReviewRecordEntity> findByMasterIdOrderByCreatedAtDesc(UUID masterId);
}
