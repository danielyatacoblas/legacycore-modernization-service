package com.portfolio.legacycore.migration;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChecksumServiceTest {
    private final ChecksumService service = new ChecksumService();

    @Test
    void producesStableChecksumWithNormalizedEmail() {
        var instant = Instant.parse("2026-08-01T10:00:00Z");
        var first = List.of(new LegacyCustomer(1, "Ana Torres", "ANA@EXAMPLE.COM", instant));
        var second = List.of(new LegacyCustomer(1, "Ana Torres", "ana@example.com", instant));
        assertThat(service.checksum(first)).isEqualTo(service.checksum(second)).hasSize(64);
    }

    @Test
    void detectsChangedBusinessData() {
        var instant = Instant.parse("2026-08-01T10:00:00Z");
        var original = List.of(new LegacyCustomer(1, "Ana Torres", "ana@example.com", instant));
        var changed = List.of(new LegacyCustomer(1, "Ana Rojas", "ana@example.com", instant));
        assertThat(service.checksum(original)).isNotEqualTo(service.checksum(changed));
    }
}
