package com.bank.docgen.master.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MasterAnchorRepository extends JpaRepository<MasterAnchorEntity, MasterAnchorEntity.MasterAnchorId> {

    @Query("SELECT a.masterId, COUNT(a) FROM MasterAnchorEntity a WHERE a.masterId IN :masterIds GROUP BY a.masterId")
    List<Object[]> countByMasterIdIn(@Param("masterIds") Collection<UUID> masterIds);
}
