package io.unlogged.auth;

//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class UnloggedSpringAuthentication {

    private final RequestAuthentication authRequest;

    public UnloggedSpringAuthentication(RequestAuthentication authRequest) {
        this.authRequest = authRequest;
    }

    public RequestAuthentication getAuthRequest() {
        return authRequest;
    }


    //    @Override
    public Collection<?> getAuthorities() {
        return authRequest.getAuthorities();
    }

    //    @Override
    public Object getCredentials() {
        return authRequest.getCredential();
    }

    //    @Override
    public Object getDetails() {
        return authRequest.getDetails();
    }

    //    @Override
    public Object getPrincipal() {
        return authRequest.getPrincipal();
    }

    // @Override
    public boolean isAuthenticated() {
        return authRequest.isAuthenticated();
    }

    //    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        // bah
    }

    //    @Override
    public String getName() {
        return authRequest.getName();
    }
}
