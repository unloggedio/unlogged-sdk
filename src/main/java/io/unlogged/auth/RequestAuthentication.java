package io.unlogged.auth;

import java.util.Collection;

public class RequestAuthentication {
    private Object principal;
    private Object credential;
    private String principalClassName;
    private String credentialClassName;
    private boolean authenticated;
    private Object details;
    private Collection<SimpleAuthority> authorities;
    private String name;

    public String getPrincipalClassName() {
        return principalClassName;
    }

    public void setPrincipalClassName(String principalClassName) {
        this.principalClassName = principalClassName;
    }

    public String getCredentialClassName() {
        return credentialClassName;
    }

    public void setCredentialClassName(String credentialClassName) {
        this.credentialClassName = credentialClassName;
    }

    public Object getPrincipal() {
        return principal;
    }

    public void setPrincipal(Object principal) {
        this.principal = principal;
    }

    public Object getCredential() {
        return credential;
    }

    public void setCredential(Object credential) {
        this.credential = credential;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }

    public Collection<SimpleAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<SimpleAuthority> authorities) {
        this.authorities = authorities;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
