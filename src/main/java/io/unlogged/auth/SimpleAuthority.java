package io.unlogged.auth;

import org.springframework.security.core.GrantedAuthority;

public class SimpleAuthority implements GrantedAuthority {
    private String authority;

    public SimpleAuthority() {
    }

    public SimpleAuthority(String authority) {
        this.authority = authority;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}
