package com.bank.docgen.authorization.management.persistence;

import com.bank.docgen.authorization.management.domain.GroupDimension;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
@Order(0)
public class TestBusinessGroupSeeder implements ApplicationRunner {

    private static final UUID RETAIL_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID CORP_ID = UUID.fromString("22222222-2222-2222-2222-222222222202");

    private final BusinessGroupRepository businessGroupRepository;

    public TestBusinessGroupSeeder(BusinessGroupRepository businessGroupRepository) {
        this.businessGroupRepository = businessGroupRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (businessGroupRepository.count() > 0) {
            return;
        }
        businessGroupRepository.save(new BusinessGroupEntity(
                RETAIL_ID, "RETAIL", "Retail Banking", GroupDimension.BUSINESS_LINE));
        businessGroupRepository.save(new BusinessGroupEntity(
                CORP_ID, "CORP", "Corporate Banking", GroupDimension.BUSINESS_LINE));
    }
}
