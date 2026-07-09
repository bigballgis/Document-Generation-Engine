package com.bank.docgen.sharedkernel.health;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComponentCheck(String status, String detail) {
}
