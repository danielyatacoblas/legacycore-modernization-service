package com.portfolio.legacycore.migration;

import java.time.Instant;

public record LegacyCustomer(long id, String fullName, String email, Instant updatedAt) { }
