package com.bank.docgen.apimgmt.service;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.ad-group-resolver")
public class AdGroupResolverProperties {

    private String type = "config";
    /**
     * LAB ONLY — when {@code true}, allows {@code type=config} on an active {@code prod} profile
     * (local docker acceptance). Must be unset/false in claimed production. Not a directory adapter.
     */
    private boolean allowConfigStubOnProdProfile = false;
    private Map<String, List<String>> accountGroups = Map.of();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isAllowConfigStubOnProdProfile() {
        return allowConfigStubOnProdProfile;
    }

    public void setAllowConfigStubOnProdProfile(boolean allowConfigStubOnProdProfile) {
        this.allowConfigStubOnProdProfile = allowConfigStubOnProdProfile;
    }

    public Map<String, List<String>> getAccountGroups() {
        return DefensiveCopies.copyAccountGroupsMap(accountGroups);
    }

    public void setAccountGroups(Map<String, List<String>> accountGroups) {
        this.accountGroups = DefensiveCopies.copyAccountGroupsMap(accountGroups);
    }
}
