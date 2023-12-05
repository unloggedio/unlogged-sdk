// 
// Decompiled by Procyon v0.6.0
// 

package org.unlogged.demo.controller;

import org.unlogged.demo.models.CustomerScoreCard;
import org.springframework.web.bind.annotation.PostMapping;
import org.unlogged.demo.models.CustomerProfileRequest;
import org.unlogged.demo.models.CustomerProfile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.unlogged.demo.service.CustomerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/customer" })
public class CustomerController
{
    @Autowired
    private CustomerService customerService;
    
    @RequestMapping({ "/get" })
    public CustomerProfile getCustomerProfile(@RequestParam final long customerID) {
        return this.customerService.fetchCustomerProfile(customerID);
    }
    
    @RequestMapping({ "/create" })
    @PostMapping
    public CustomerProfile saveCustomerProfile(@RequestParam final CustomerProfileRequest saveRequest) {
        final CustomerProfile customer = this.customerService.saveNewCustomer(saveRequest);
        return customer;
    }
    
    @RequestMapping({ "/remove" })
    public CustomerProfile removeCustomerProfile(@RequestParam final long customerID) {
        return this.customerService.removeCustomer(customerID);
    }
    
    @RequestMapping({ "/generateReferral" })
    public CustomerProfile generateNeReferralCode(@RequestParam final long customerID) {
        return this.customerService.generateReferralForCustomer(customerID);
    }
    
    public CustomerScoreCard isCustomerEligibleForLoyaltyProgram(@RequestParam final long customerID) {
        return this.customerService.isCustomerEligibleForPremium(customerID);
    }
    
    public float gen_sum(final float a, final float b) {
        final float val = a + b;
        return val;
    }
}
