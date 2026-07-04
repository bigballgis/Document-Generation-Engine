package com.bank.docgen.apimgmt.service;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "docgen.ad-group-resolver")
public class AdGroupResolverProperties {

    private String type = "config";
    private Map<String, List<String>> accountGroups = Map.of();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, List<String>> getAccountGroups() {
        if (accountGroups == null || accountGroups.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : accountGroups.entrySet()) {
            copy.put(entry.getKey(), DefensiveCopies.copyStringList(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    public void setAccountGroups(Map<String, List<String>> accountGroups) {
        this.accountGroups = accountGroups;
    }
}
