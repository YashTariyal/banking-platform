package com.banking.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("securityToggle")
public class SecurityToggle {

    private final boolean securityEnabled;

    public SecurityToggle(@Value("${account.security.enabled:false}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public boolean isDisabled() {
        return !securityEnabled;
    }

    public boolean isEnabled() {
        return securityEnabled;
    }
}


