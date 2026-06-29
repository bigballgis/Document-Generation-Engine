package com.bank.docgen.apimgmt.api;

import java.util.List;

public record SaveOutputPolicyRequest(List<String> outputFormats, List<String> outputModes, boolean confirmed) {
}
