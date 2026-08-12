package com.portfolio.legacycore.migration;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class ChecksumService {
    public String checksum(Iterable<LegacyCustomer> rows) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (var row : rows) {
                var canonical = row.id() + "|" + row.fullName().strip() + "|"
                        + row.email().strip().toLowerCase(Locale.ROOT) + "|" + row.updatedAt() + "\n";
                digest.update(canonical.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
