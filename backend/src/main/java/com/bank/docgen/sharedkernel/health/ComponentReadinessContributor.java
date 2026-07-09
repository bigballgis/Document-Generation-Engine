package com.bank.docgen.sharedkernel.health;

public interface ComponentReadinessContributor {

    String componentName();

    ComponentCheck check();
}
