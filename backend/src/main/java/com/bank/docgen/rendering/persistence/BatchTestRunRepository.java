package com.bank.docgen.rendering.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchTestRunRepository extends JpaRepository<BatchTestRunEntity, UUID> {
}
