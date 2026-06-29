package com.bank.docgen.apimgmt.mapping;

import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ApiPolicyMappingSupport.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ApiPolicyViewMapper {

    @Mapping(target = "templateId", source = "templateId", qualifiedByName = "uuidToString")
    @Mapping(target = "allowedAdGroups", source = "allowedAdGroupsJson", qualifiedByName = "readStringList")
    @Mapping(target = "outputFormats", source = "outputFormatsJson", qualifiedByName = "readStringList")
    @Mapping(target = "outputModes", source = "outputModesJson", qualifiedByName = "readStringList")
    ApiPolicyView toPolicyView(ApiPolicyEntity policy);

    @Mapping(target = "credentialId", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "status", source = "status", qualifiedByName = "enumToName")
    ApiCredentialSummaryView toCredentialSummary(ApiCredentialEntity credential);

    @Mapping(target = "credentialExternalId", source = "externalId")
    @Mapping(target = "status", source = "status", qualifiedByName = "enumToName")
    @Mapping(target = "fingerprintSummary", source = "externalId", qualifiedByName = "runtimeFingerprint")
    RuntimeCredentialSummaryView toRuntimeCredentialSummary(ApiCredentialEntity credential);
}
