package com.octopus.teamcity.oidc;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuanceSettingsTest {

    @Test
    public void fromBuildFeatureParamsAppliesDefaults() {
        final var settings = IssuanceSettings.fromBuildFeatureParams(
                Map.of(), "https://teamcity.example.com", 720);

        assertThat(settings.audience()).isEqualTo("https://teamcity.example.com");
        assertThat(settings.ttlMinutes()).isEqualTo(10);
        assertThat(settings.signingAlgorithm()).isEqualTo("RS256");
        assertThat(settings.subjectDimensions()).isEmpty();
    }

    @Test
    public void fromBuildFeatureParamsUsesProvidedValues() {
        final var settings = IssuanceSettings.fromBuildFeatureParams(
                Map.of(
                        "audience", "api://example",
                        "ttl_minutes", "30",
                        "algorithm", "ES256",
                        "subject_dimensions", "branch,trigger_type"),
                "https://teamcity.example.com", 720);

        assertThat(settings.audience()).isEqualTo("api://example");
        assertThat(settings.ttlMinutes()).isEqualTo(30);
        assertThat(settings.signingAlgorithm()).isEqualTo("ES256");
        assertThat(settings.subjectDimensions()).containsExactlyInAnyOrder("branch", "trigger_type");
    }

    @Test
    public void ttlClampedToServerMaxAndMin() {
        final var clampedHigh = IssuanceSettings.fromBuildFeatureParams(
                Map.of("ttl_minutes", "9999"), "https://issuer", 60);
        assertThat(clampedHigh.ttlMinutes()).isEqualTo(60);

        final var clampedLow = IssuanceSettings.fromBuildFeatureParams(
                Map.of("ttl_minutes", "-5"), "https://issuer", 720);
        assertThat(clampedLow.ttlMinutes()).isEqualTo(OidcSettings.MIN_TOKEN_LIFETIME_MINUTES);
    }

    @Test
    public void invalidTtlFallsBackToTenClampedToMax() {
        final var fallback = IssuanceSettings.fromBuildFeatureParams(
                Map.of("ttl_minutes", "not-a-number"), "https://issuer", 720);
        assertThat(fallback.ttlMinutes()).isEqualTo(10);

        final var fallbackClamped = IssuanceSettings.fromBuildFeatureParams(
                Map.of("ttl_minutes", "not-a-number"), "https://issuer", 5);
        assertThat(fallbackClamped.ttlMinutes()).isEqualTo(5);
    }

    @Test
    public void blankAudienceDefaultsToIssuerUrl() {
        final var settings = IssuanceSettings.fromBuildFeatureParams(
                Map.of("audience", ""), "https://teamcity.example.com", 720);
        assertThat(settings.audience()).isEqualTo("https://teamcity.example.com");
    }

    @Test
    public void unknownSubjectDimensionsAreFilteredOut() {
        final var settings = IssuanceSettings.fromBuildFeatureParams(
                Map.of("subject_dimensions", "branch,bogus,trigger_type"),
                "https://issuer", 720);
        assertThat(settings.subjectDimensions()).containsExactlyInAnyOrder("branch", "trigger_type");
    }
}
