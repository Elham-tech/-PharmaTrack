/*
 * ARCHITECTURE: Makes authority names CONFIGURABLE rather than hard-coded.
 * @PreAuthorize expressions reference this bean (e.g. hasAuthority(@appAuthorities.admin)),
 * so the required authority can be changed in application.properties without touching code.
 * The names themselves are stored in the database (authorities table) and read at runtime.
 */
package com.example.PharmaTrack.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppAuthorities {

    // Configurable authority names (defaults match the seeded database authorities)
    @Value("${app.security.authorities.admin:ADMIN}")
    private String admin;

    @Value("${app.security.authorities.pharmacist:PHARMACIST}")
    private String pharmacist;

    @Value("${app.security.authorities.cashier:CASHIER}")
    private String cashier;

    @Value("${app.security.authorities.inventory-manager:INVENTORY_MANAGER}")
    private String inventoryManager;

    @Value("${app.security.authorities.auditor:AUDITOR}")
    private String auditor;

    public String getAdmin() { return admin; }
    public String getPharmacist() { return pharmacist; }
    public String getCashier() { return cashier; }
    public String getInventoryManager() { return inventoryManager; }
    public String getAuditor() { return auditor; }
}
