package com.portfolio.legacycore.web;

import com.portfolio.legacycore.migration.CustomerMigrationService;
import com.portfolio.legacycore.migration.MigrationModels.MigrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MigrationController {
    private final CustomerMigrationService service;

    public MigrationController(CustomerMigrationService service) { this.service = service; }

    @PostMapping("/migrations/customers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Object migrate(@Valid @RequestBody MigrationRequest request) { return service.migrate(request); }

    @GetMapping("/migrations/customers/status")
    Object status() { return service.status(); }

    @GetMapping("/reconciliation/customers")
    Object reconcile() { return service.reconcile(); }
}
