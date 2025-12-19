package com.yourapp.health;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "healthz")
class HealthzCriticalProperties {
    private List<String> critical = new ArrayList<>();
    public List<String> getCritical() { return critical; }
    public void setCritical(List<String> critical) { this.critical = critical; }
}

@Configuration
@EnableConfigurationProperties(HealthzCriticalProperties.class)
public class HealthzAggregationConfig {

    /**
     * This replaces Boot’s default HealthEndpointWebExtension (Boot auto-config backs off if you provide one).
     */
    @Bean
    public HealthEndpointWebExtension healthEndpointWebExtension(
            HealthContributorRegistry registry,
            HealthEndpointGroups groups,
            HealthzCriticalProperties props
    ) {
        // same constructor signature shown in Boot’s javadoc for 3.x
        Duration slowLogThreshold = Duration.ofSeconds(10);

        Set<String> criticalIds = props.getCritical().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new CriticalAwareHealthEndpointWebExtension(registry, groups, slowLogThreshold, criticalIds);
    }

    static final class CriticalAwareHealthEndpointWebExtension extends HealthEndpointWebExtension {

        private final Set<String> criticalIds;

        CriticalAwareHealthEndpointWebExtension(
                HealthContributorRegistry registry,
                HealthEndpointGroups groups,
                Duration slowIndicatorLoggingThreshold,
                Set<String> criticalIds
        ) {
            super(registry, groups, slowIndicatorLoggingThreshold);
            this.criticalIds = criticalIds;
        }

        @Override
        protected HealthComponent aggregateContributions(
                ApiVersion apiVersion,
                Map<String, HealthComponent> contributions,
                StatusAggregator statusAggregator,
                boolean showComponents,
                Set<String> groupNames
        ) {
            // First build the normal composite (keeps full "components" tree + details)
            HealthComponent base = super.aggregateContributions(
                    apiVersion, contributions, statusAggregator, showComponents, groupNames
            );

            // Compute overall based ONLY on critical component statuses (if present)
            List<Status> criticalStatuses = contributions.entrySet().stream()
                    .filter(e -> criticalIds.contains(e.getKey()))
                    .map(e -> e.getValue().getStatus())
                    .toList();

            // If none of the configured critical ids exist, fall back to normal aggregation
            if (criticalStatuses.isEmpty()) {
                return base;
            }

            Status overall = statusAggregator.aggregate(criticalStatuses);

            // Rewrite ONLY the top-level status, keep all components as-is
            if (base instanceof CompositeHealth composite) {
                return new CompositeHealth(overall, composite.getComponents());
            }

            // If actuator ever returns a non-composite, just return a simple Health with computed status
            return Health.status(overall).build();
        }
    }
}
