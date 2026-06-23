package com.bank.docgen.authorization.management.persistence;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestManagementUserSeeder implements ApplicationRunner {

    private static final UUID GLOBAL_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");
    private static final UUID GROUP_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111102");
    private static final UUID TEMPLATE_AUTHOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111103");
    private static final UUID AUDIT_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111104");
    private static final UUID MASTER_DESIGNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111105");
    private static final UUID TEMPLATE_TESTER_ID = UUID.fromString("11111111-1111-1111-1111-111111111106");
    private static final UUID TEMPLATE_APPROVER_ID = UUID.fromString("11111111-1111-1111-1111-111111111107");

    private final ManagementUserRepository managementUserRepository;
    private final PasswordHashService passwordHashService;

    public TestManagementUserSeeder(
            ManagementUserRepository managementUserRepository,
            PasswordHashService passwordHashService
    ) {
        this.managementUserRepository = managementUserRepository;
        this.passwordHashService = passwordHashService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (managementUserRepository.count() > 0) {
            return;
        }
        String passwordHash = passwordHashService.hash("ChangeMe123!");
        managementUserRepository.save(new ManagementUserEntity(
                GLOBAL_ADMIN_ID,
                "10000001",
                "Global Admin",
                "global.admin@example.com",
                passwordHash,
                AuthSource.LOCAL,
                Set.of(ManagementRole.GLOBAL_ADMIN),
                Set.of()
        ));
        managementUserRepository.save(new ManagementUserEntity(
                GROUP_ADMIN_ID,
                "10000002",
                "Group Admin",
                "group.admin@example.com",
                passwordHash,
                AuthSource.LOCAL,
                Set.of(ManagementRole.GROUP_ADMIN),
                Set.of("RETAIL", "CORP")
        ));
        managementUserRepository.save(new ManagementUserEntity(
                TEMPLATE_AUTHOR_ID,
                "10000003",
                "Template Author",
                "author@example.com",
                passwordHash,
                AuthSource.LOCAL,
                Set.of(ManagementRole.TEMPLATE_AUTHOR),
                Set.of("RETAIL")
        ));
        managementUserRepository.save(new ManagementUserEntity(
                AUDIT_ADMIN_ID,
                "10000004",
                "Audit Admin",
                "audit.admin@example.com",
                passwordHash,
                AuthSource.LOCAL,
                Set.of(ManagementRole.AUDIT_ADMIN),
                Set.of()
        ));
        managementUserRepository.save(new ManagementUserEntity(
                MASTER_DESIGNER_ID,
                "10000005",
                "Master Designer",
                "master.designer@example.com",
                passwordHash,
                AuthSource.LOCAL,
                Set.of(ManagementRole.MASTER_DESIGNER),
                Set.of("RETAIL")
        ));
        managementUserRepository.save(new ManagementUserEntity(
                TEMPLATE_TESTER_ID,
                "10000006",
                "Template Tester",
                "template.tester@example.com",
                passwordHash,
                AuthSource.LOCAL,
                Set.of(ManagementRole.TEMPLATE_TESTER),
                Set.of("RETAIL")
        ));
        managementUserRepository.save(new ManagementUserEntity(
                TEMPLATE_APPROVER_ID,
                "10000007",
                "Template Approver",
                "template.approver@example.com",
                passwordHash,
                AuthSource.LOCAL,
                Set.of(ManagementRole.TEMPLATE_APPROVER),
                Set.of("RETAIL")
        ));
    }
}
