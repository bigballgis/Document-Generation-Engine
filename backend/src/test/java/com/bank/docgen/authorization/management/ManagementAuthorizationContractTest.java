package com.bank.docgen.authorization.management;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

class ManagementAuthorizationContractTest {

    private static final Path BACKEND_MAIN = Path.of("src", "main", "java", "com", "bank", "docgen");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+);", Pattern.MULTILINE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
    private static final int MAX_DELEGATION_DEPTH = 4;

    @Test
    void registryCoversAllDiscoveredManagementControllers() throws IOException {
        Set<String> discovered = discoverManagementControllers();
        Set<String> registered = ManagementAuthorizationRegistry.MANAGEMENT_CONTROLLERS;

        Set<String> missingFromRegistry = new TreeSet<>(discovered);
        missingFromRegistry.removeAll(registered);

        Set<String> staleInRegistry = new TreeSet<>(registered);
        staleInRegistry.removeAll(discovered);

        assertThat(missingFromRegistry)
                .as("Add missing controllers to ManagementAuthorizationRegistry.MANAGEMENT_CONTROLLERS: %s",
                        missingFromRegistry)
                .isEmpty();
        assertThat(staleInRegistry)
                .as("Remove stale controllers from ManagementAuthorizationRegistry.MANAGEMENT_CONTROLLERS: %s",
                        staleInRegistry)
                .isEmpty();
    }

    @Test
    void everyRegisteredControllerHasPrimaryServiceMapping() {
        Set<String> missingMappings = new TreeSet<>();
        for (String controller : ManagementAuthorizationRegistry.MANAGEMENT_CONTROLLERS) {
            if (!ManagementAuthorizationRegistry.CONTROLLER_PRIMARY_SERVICES.containsKey(controller)) {
                missingMappings.add(controller);
            }
        }
        assertThat(missingMappings)
                .as("Add primary service mappings for controllers: %s", missingMappings)
                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("managementControllers")
    void managementControllersDoNotReferenceManagementRoute(String controllerName) throws IOException {
        String content = readControllerSource(controllerName);
        assertThat(content)
                .as("%s must not use ManagementRoute for API authorization", controllerName)
                .doesNotContain("ManagementRoute");
    }

    @ParameterizedTest
    @MethodSource("managementControllers")
    void managementControllersDoNotUsePreAuthorize(String controllerName) throws IOException {
        String content = readControllerSource(controllerName);
        assertThat(content)
                .as("%s must not use @PreAuthorize; service-layer GroupAccessService is authoritative", controllerName)
                .doesNotContain("@PreAuthorize");
    }

    @ParameterizedTest
    @MethodSource("managementAuthorizationAnchors")
    void managementAuthorizationAnchorsInjectGroupAccessService(String anchorClassName) {
        Class<?> anchorClass = loadClass(anchorClassName);
        assertThat(isSpringManagedType(anchorClass))
                .as("%s should be a Spring @Service or @Component", anchorClassName)
                .isTrue();
        assertThat(hasGroupAccessServiceField(anchorClass))
                .as("%s should inject GroupAccessService for capability checks", anchorClass.getSimpleName())
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("managementAuthorizationDelegates")
    void managementAuthorizationDelegatesResolveToGroupAccessAnchor(String delegateClassName) {
        assertThat(resolveAuthorizationAnchor(delegateClassName))
                .as("%s should delegate to a GroupAccessService anchor", delegateClassName)
                .isNotNull();
    }

    @Test
    void documentedAuthorizationExceptionsAreStable() {
        assertThat(ManagementAuthorizationRegistry.AUTHORIZATION_EXCEPTIONS)
                .containsOnlyKeys(
                        "com.bank.docgen.authorization.management.service.BusinessGroupService",
                        "com.bank.docgen.authorization.management.service.ManagementAuthService",
                        "com.bank.docgen.authorization.management.service.SecurityAuditSummaryService",
                        "com.bank.docgen.authorization.management.service.UserManagementService",
                        "com.bank.docgen.template.service.RiskPromptConfigService"
                );
        for (String exceptionService : ManagementAuthorizationRegistry.AUTHORIZATION_EXCEPTIONS.keySet()) {
            assertThat(loadClass(exceptionService).getAnnotation(Service.class)).isNotNull();
        }
    }

    @Test
    void everyControllerPrimaryServiceHasAuthorizationContract() {
        List<String> violations = new ArrayList<>();
        for (var entry : ManagementAuthorizationRegistry.CONTROLLER_PRIMARY_SERVICES.entrySet()) {
            for (String serviceName : entry.getValue()) {
                if (ManagementAuthorizationRegistry.AUTHORIZATION_EXCEPTIONS.containsKey(serviceName)) {
                    continue;
                }
                if (ManagementAuthorizationRegistry.GROUP_ACCESS_ANCHORS.contains(serviceName)) {
                    continue;
                }
                if (ManagementAuthorizationRegistry.AUTHORIZATION_DELEGATES.containsKey(serviceName)) {
                    continue;
                }
                violations.add(entry.getKey() + " -> " + serviceName
                        + " (add to GROUP_ACCESS_ANCHORS, AUTHORIZATION_DELEGATES, or AUTHORIZATION_EXCEPTIONS)");
            }
        }
        assertThat(violations)
                .as("Primary services missing authorization contract: %s", violations)
                .isEmpty();
    }

    @Test
    void groupAccessServiceExposesManagementCapabilities() {
        assertThat(GroupAccessService.class.getDeclaredMethods())
                .extracting(method -> method.getName())
                .contains(
                        "canManageMasters",
                        "canPublishTemplates",
                        "canManageApiPolicy",
                        "canReadAudit",
                        "canAccessGroup"
                );
    }

    private static Stream<String> managementControllers() {
        return ManagementAuthorizationRegistry.MANAGEMENT_CONTROLLERS.stream().sorted();
    }

    private static Stream<String> managementAuthorizationAnchors() {
        return ManagementAuthorizationRegistry.GROUP_ACCESS_ANCHORS.stream().sorted();
    }

    private static Stream<String> managementAuthorizationDelegates() {
        return ManagementAuthorizationRegistry.AUTHORIZATION_DELEGATES.keySet().stream().sorted();
    }

    private static Set<String> discoverManagementControllers() throws IOException {
        Set<String> discovered = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(BACKEND_MAIN)) {
            paths.filter(path -> path.toString().endsWith("Controller.java"))
                    .forEach(path -> readControllerCandidate(path).ifPresent(discovered::add));
        }
        return discovered;
    }

    private static java.util.Optional<String> readControllerCandidate(Path path) {
        try {
            String content = Files.readString(path);
            if (!content.contains("@RestController")) {
                return java.util.Optional.empty();
            }
            if (!content.contains(ManagementAuthorizationRegistry.MANAGEMENT_API_PREFIX)) {
                return java.util.Optional.empty();
            }
            Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
            if (!packageMatcher.find()) {
                return java.util.Optional.empty();
            }
            Matcher classMatcher = CLASS_PATTERN.matcher(content);
            if (!classMatcher.find()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(packageMatcher.group(1) + "." + classMatcher.group(1));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read controller candidate: " + path, ex);
        }
    }

    private static String readControllerSource(String controllerName) throws IOException {
        String relativePath = controllerName.replace("com.bank.docgen.", "").replace('.', '/')
                + ".java";
        return Files.readString(BACKEND_MAIN.resolve(relativePath));
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Missing management authorization class: " + className, ex);
        }
    }

    private static boolean isSpringManagedType(Class<?> type) {
        return type.getAnnotation(Service.class) != null || type.getAnnotation(Component.class) != null;
    }

    private static boolean hasGroupAccessServiceField(Class<?> serviceClass) {
        return Stream.of(serviceClass.getDeclaredFields())
                .anyMatch(field -> GroupAccessService.class.equals(field.getType()));
    }

    private static String resolveAuthorizationAnchor(String serviceName) {
        Set<String> visited = new HashSet<>();
        String current = serviceName;
        for (int depth = 0; depth < MAX_DELEGATION_DEPTH; depth++) {
            if (!visited.add(current)) {
                return null;
            }
            if (ManagementAuthorizationRegistry.GROUP_ACCESS_ANCHORS.contains(current)) {
                Class<?> anchorClass = loadClass(current);
                return hasGroupAccessServiceField(anchorClass) ? current : null;
            }
            if (ManagementAuthorizationRegistry.AUTHORIZATION_EXCEPTIONS.containsKey(current)) {
                return current;
            }
            String delegate = ManagementAuthorizationRegistry.AUTHORIZATION_DELEGATES.get(current);
            if (delegate == null) {
                return null;
            }
            current = delegate;
        }
        return null;
    }
}
