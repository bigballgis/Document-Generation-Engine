package com.bank.docgen.apimgmt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        return accountGroups;
    }

    public void setAccountGroups(Map<String, List<String>> accountGroups) {
        this.accountGroups = accountGroups;
    }
}
