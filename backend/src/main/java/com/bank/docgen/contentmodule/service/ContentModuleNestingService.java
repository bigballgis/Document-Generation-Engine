package com.bank.docgen.contentmodule.service;

import com.bank.docgen.contentmodule.api.ContentModuleNestingAncestorHit;
import com.bank.docgen.contentmodule.api.ContentModuleNestingPublishSummaryView;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleNestingEdgeEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleNestingEdgeRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IBL-E6 / ADR-0067 — governed CM↔CM nesting graph (write validation, where-used depth, publish gates).
 */
@Service
public class ContentModuleNestingService {

    public static final int MAX_NESTING_DEPTH = ComputeDslLimits.MAX_NESTING_DEPTH;

    private final ContentModuleNestingEdgeRepository edgeRepository;
    private final ContentModuleRepository moduleRepository;
    private final ContentModuleVersionRepository versionRepository;
    private final ContentModuleAccessService accessSupport;
    private final ObjectMapper objectMapper;

    public ContentModuleNestingService(
            ContentModuleNestingEdgeRepository edgeRepository,
            ContentModuleRepository moduleRepository,
            ContentModuleVersionRepository versionRepository,
            ContentModuleAccessService accessSupport,
            ObjectMapper objectMapper
    ) {
        this.edgeRepository = edgeRepository;
        this.moduleRepository = moduleRepository;
        this.versionRepository = versionRepository;
        this.accessSupport = accessSupport;
        this.objectMapper = objectMapper;
    }

    /**
     * Validate nesting for a structure write, then replace edges for {@code parentVersionId}.
     */
    @Transactional
    public void validateAndSyncEdges(
            UUID parentVersionId,
            UUID parentModuleId,
            String parentModuleCode,
            String contentStructureJson,
            ManagementSessionClaims session
    ) {
        Map<String, UUID> resolvedTargets = resolveWriteTargets(
                parentModuleId,
                parentModuleCode,
                contentStructureJson,
                session
        );
        // UNIQUE(parent_version_id, target_module_id): collapse multi-key → same target to one edge.
        Map<UUID, String> edgesByTarget = collapseToUniqueTargets(resolvedTargets);
        assertAcyclicAndWithinDepth(parentModuleId, parentVersionId, edgesByTarget.keySet());
        edgeRepository.deleteByParentVersionId(parentVersionId);
        for (Map.Entry<UUID, String> entry : edgesByTarget.entrySet()) {
            edgeRepository.save(new ContentModuleNestingEdgeEntity(
                    UUID.randomUUID(),
                    parentVersionId,
                    entry.getKey(),
                    entry.getValue()
            ));
        }
    }

    @Transactional(readOnly = true)
    public List<ContentModuleNestingEdgeEntity> listEdgesForVersion(UUID parentVersionId) {
        return edgeRepository.findByParentVersionId(parentVersionId);
    }

    /**
     * Ancestor versions that nest {@code targetModuleId} (direct or transitive), with path metadata.
     */
    @Transactional(readOnly = true)
    public List<ContentModuleNestingAncestorHit> findNestingAncestors(UUID targetModuleId) {
        Map<UUID, String> moduleCodes = new HashMap<>();
        moduleRepository.findByIdAndDeletedAtIsNull(targetModuleId)
                .ifPresent(module -> moduleCodes.put(module.getId(), module.getModuleCode()));

        record PathState(UUID moduleId, int depth, String pathSummary) {
        }

        Map<UUID, ContentModuleNestingAncestorHit> bestByAncestorVersion = new LinkedHashMap<>();
        Deque<PathState> queue = new ArrayDeque<>();
        queue.add(new PathState(targetModuleId, 0, moduleCodes.getOrDefault(targetModuleId, "")));
        Set<UUID> visitedModules = new HashSet<>();
        visitedModules.add(targetModuleId);

        while (!queue.isEmpty()) {
            PathState current = queue.removeFirst();
            List<ContentModuleNestingEdgeEntity> inbound = edgeRepository.findByTargetModuleId(current.moduleId());
            for (ContentModuleNestingEdgeEntity edge : inbound) {
                Optional<ContentModuleVersionEntity> parentVersion =
                        versionRepository.findById(edge.getParentVersionId());
                if (parentVersion.isEmpty()) {
                    continue;
                }
                UUID ancestorModuleId = parentVersion.get().getModuleId();
                String ancestorCode = moduleCodes.computeIfAbsent(ancestorModuleId, id ->
                        moduleRepository.findByIdAndDeletedAtIsNull(id)
                                .map(ContentModuleEntity::getModuleCode)
                                .orElse(id.toString()));
                int nextDepth = current.depth() + 1;
                String nextPath = current.pathSummary().isBlank()
                        ? ancestorCode
                        : ancestorCode + ">" + current.pathSummary();
                ContentModuleNestingAncestorHit hit = new ContentModuleNestingAncestorHit(
                        edge.getParentVersionId(),
                        ancestorModuleId,
                        ancestorCode,
                        nextDepth,
                        nextPath
                );
                ContentModuleNestingAncestorHit existing = bestByAncestorVersion.get(edge.getParentVersionId());
                if (existing == null || hit.nestingDepth() < existing.nestingDepth()) {
                    bestByAncestorVersion.put(edge.getParentVersionId(), hit);
                }
                if (visitedModules.add(ancestorModuleId)) {
                    queue.addLast(new PathState(ancestorModuleId, nextDepth, nextPath));
                }
            }
        }
        return List.copyOf(bestByAncestorVersion.values());
    }

    /**
     * Evaluate publish-gate nesting closure over pinned structures (referenceKey → structure JSON).
     */
    @Transactional(readOnly = true)
    public ContentModuleNestingPublishSummaryView evaluatePublishClosure(Map<String, String> pinnedStructures) {
        if (pinnedStructures == null || pinnedStructures.isEmpty()) {
            return ContentModuleNestingPublishSummaryView.clear();
        }
        Map<String, String> pins = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : pinnedStructures.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            pins.put(entry.getKey().trim().toUpperCase(Locale.ROOT), entry.getValue());
        }

        List<String> cycleDetails = new ArrayList<>();
        List<String> depthDetails = new ArrayList<>();
        List<String> unpinnedDetails = new ArrayList<>();

        for (String rootKey : pins.keySet()) {
            Set<String> stack = new LinkedHashSet<>();
            walkPinnedClosure(
                    rootKey,
                    pins,
                    stack,
                    0,
                    cycleDetails,
                    depthDetails,
                    unpinnedDetails
            );
        }

        return new ContentModuleNestingPublishSummaryView(
                !cycleDetails.isEmpty(),
                !depthDetails.isEmpty(),
                !unpinnedDetails.isEmpty(),
                distinct(cycleDetails),
                distinct(depthDetails),
                distinct(unpinnedDetails)
        );
    }

    private void walkPinnedClosure(
            String referenceKey,
            Map<String, String> pins,
            Set<String> stack,
            int depthFromRoot,
            List<String> cycleDetails,
            List<String> depthDetails,
            List<String> unpinnedDetails
    ) {
        if (!stack.add(referenceKey)) {
            cycleDetails.add("cycleAt=" + referenceKey);
            return;
        }
        try {
            if (depthFromRoot > MAX_NESTING_DEPTH) {
                depthDetails.add("depth=" + depthFromRoot + ",at=" + referenceKey);
                return;
            }
            String structure = pins.get(referenceKey);
            if (structure == null || structure.isBlank()) {
                // Root keys are always pinned when iterating pins.keySet(); nested missing → unpinned.
                if (depthFromRoot > 0) {
                    unpinnedDetails.add("missingPin=" + referenceKey);
                }
                return;
            }
            Set<String> nestedKeys;
            try {
                nestedKeys = ContentModuleNestingStructureSupport.extractReferenceKeys(
                        objectMapper, structure);
            } catch (ContentModuleGovernanceException ex) {
                if (ApiErrorCodes.CONTENT_MODULE_NESTING_STRUCTURE_INVALID.equals(ex.errorCode())) {
                    unpinnedDetails.add("invalidStructure=" + referenceKey);
                    return;
                }
                throw ex;
            }
            for (String nestedKey : nestedKeys) {
                if (!pins.containsKey(nestedKey) || isBlank(pins.get(nestedKey))) {
                    unpinnedDetails.add("missingPin=" + nestedKey + ",from=" + referenceKey);
                    continue;
                }
                int nextDepth = depthFromRoot + 1;
                if (nextDepth > MAX_NESTING_DEPTH) {
                    depthDetails.add("depth=" + nextDepth + ",at=" + nestedKey + ",from=" + referenceKey);
                    continue;
                }
                walkPinnedClosure(
                        nestedKey,
                        pins,
                        stack,
                        nextDepth,
                        cycleDetails,
                        depthDetails,
                        unpinnedDetails
                );
            }
        } finally {
            stack.remove(referenceKey);
        }
    }

    private Map<String, UUID> resolveWriteTargets(
            UUID parentModuleId,
            String parentModuleCode,
            String contentStructureJson,
            ManagementSessionClaims session
    ) {
        Set<String> keys = ContentModuleNestingStructureSupport.extractReferenceKeys(
                objectMapper, contentStructureJson);
        Map<String, UUID> resolved = new LinkedHashMap<>();
        String normalizedParentCode = parentModuleCode == null
                ? ""
                : parentModuleCode.trim().toUpperCase(Locale.ROOT);
        for (String key : keys) {
            if (key.equals(normalizedParentCode) || key.equalsIgnoreCase(parentModuleId.toString())) {
                throw nestingException(
                        ApiErrorCodes.CONTENT_MODULE_NESTING_CYCLE,
                        "api.error.contentModule.nestingCycle"
                );
            }
            ContentModuleEntity target = accessSupport.resolveModule(key).orElse(null);
            if (target == null || !accessSupport.canAccessModule(session, target)) {
                throw nestingException(
                        ApiErrorCodes.CONTENT_MODULE_NESTING_TARGET_UNRESOLVED,
                        "api.error.contentModule.nestingTargetUnresolved"
                );
            }
            if (target.getId().equals(parentModuleId)) {
                throw nestingException(
                        ApiErrorCodes.CONTENT_MODULE_NESTING_CYCLE,
                        "api.error.contentModule.nestingCycle"
                );
            }
            resolved.putIfAbsent(key, target.getId());
        }
        return resolved;
    }

    private void assertAcyclicAndWithinDepth(
            UUID parentModuleId,
            UUID parentVersionId,
            Set<UUID> targetModuleIds
    ) {
        Map<UUID, Set<UUID>> override = Map.of(parentModuleId, Set.copyOf(targetModuleIds));
        Map<UUID, Set<UUID>> neighborCache = new HashMap<>();
        int maxDepth = longestSimplePathDepth(
                parentModuleId,
                parentVersionId,
                override,
                neighborCache,
                new LinkedHashSet<>());
        if (maxDepth < 0) {
            throw nestingException(
                    ApiErrorCodes.CONTENT_MODULE_NESTING_CYCLE,
                    "api.error.contentModule.nestingCycle"
            );
        }
        if (maxDepth > MAX_NESTING_DEPTH) {
            throw nestingException(
                    ApiErrorCodes.CONTENT_MODULE_NESTING_DEPTH_EXCEEDED,
                    "api.error.contentModule.nestingDepthExceeded"
            );
        }
    }

    /**
     * Keep first referenceKey per target module (insertion order) so UNIQUE(parent, target) holds.
     */
    private static Map<UUID, String> collapseToUniqueTargets(Map<String, UUID> resolvedByKey) {
        Map<UUID, String> byTarget = new LinkedHashMap<>();
        for (Map.Entry<String, UUID> entry : resolvedByKey.entrySet()) {
            byTarget.putIfAbsent(entry.getValue(), entry.getKey());
        }
        return byTarget;
    }

    /**
     * @return longest simple path edge count from {@code node}, or {@code -1} if a cycle is found
     */
    private int longestSimplePathDepth(
            UUID node,
            UUID excludeVersionId,
            Map<UUID, Set<UUID>> override,
            Map<UUID, Set<UUID>> neighborCache,
            Set<UUID> path
    ) {
        if (!path.add(node)) {
            return -1;
        }
        int best = 0;
        for (UUID child : neighbors(node, excludeVersionId, override, neighborCache)) {
            int childDepth = longestSimplePathDepth(
                    child, excludeVersionId, override, neighborCache, path);
            if (childDepth < 0) {
                path.remove(node);
                return -1;
            }
            best = Math.max(best, childDepth + 1);
        }
        path.remove(node);
        return best;
    }

    private Set<UUID> neighbors(
            UUID moduleId,
            UUID excludeVersionId,
            Map<UUID, Set<UUID>> override,
            Map<UUID, Set<UUID>> neighborCache
    ) {
        if (override.containsKey(moduleId)) {
            return override.get(moduleId);
        }
        Set<UUID> cached = neighborCache.get(moduleId);
        if (cached != null) {
            return cached;
        }
        List<ContentModuleVersionEntity> versions =
                versionRepository.findByModuleIdOrderBySemanticVersionDesc(moduleId);
        if (versions.isEmpty()) {
            neighborCache.put(moduleId, Set.of());
            return Set.of();
        }
        UUID versionId = versions.getFirst().getId();
        if (Objects.equals(versionId, excludeVersionId)) {
            neighborCache.put(moduleId, Set.of());
            return Set.of();
        }
        Set<UUID> targets = new LinkedHashSet<>();
        for (ContentModuleNestingEdgeEntity edge : edgeRepository.findByParentVersionId(versionId)) {
            targets.add(edge.getTargetModuleId());
        }
        Set<UUID> immutable = Set.copyOf(targets);
        neighborCache.put(moduleId, immutable);
        return immutable;
    }

    private static ContentModuleGovernanceException nestingException(String code, String messageKey) {
        return new ContentModuleGovernanceException(code, messageKey, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<String> distinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }
}
