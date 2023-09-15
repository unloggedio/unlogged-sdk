package io.unlogged.test;

import java.util.Optional;

public class JpaFieldUserService {
    private AuthorityRepository authorityRepository;

    public Optional<Authority> callToTest(String a_val, SimplePojoB pojoB) {
        return authorityRepository.getReferenceById(a_val + pojoB.getStrValue());
    }
}

