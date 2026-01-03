package com.yourpkg; // keep your existing package

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HealthzAggregationConfig.HealthzProperties.class)
public class HealthzAggregationConfig {

  @Bean
  public HealthEndpointWebExtension healthEndpointWebExtension(
      HealthContributorRegistry registry,
      HealthEndpointGroups groups,
      HealthzProperties props
  ) {
    // keep your Duration / threshold as you already had (3 sec etc.)
    return new CriticalOnlyAggregationHealthWebExtension(registry, groups, Duration.ofSeconds(3), props);
  }

  @ConfigurationProperties(prefix = "healthz")
  public static class HealthzProperties {
    private Set<String> critical = Set.of();

    public Set<String> getCritical() {
      return critical;
    }

    public void setCritical(Set<String> critical) {
      this.critical = critical;
    }
  }

  static final class CriticalOnlyAggregationHealthWebExtension extends HealthEndpointWebExtension {

    private final HealthzProperties props;

    CriticalOnlyAggregationHealthWebExtension(
        HealthContributorRegistry registry,
        HealthEndpointGroups groups,
        Duration slowIndicatorLoggingThreshold,
        HealthzProperties props
    ) {
      super(registry, groups, slowIndicatorLoggingThreshold);
      this.props = props;
    }

    @Override
    protected HealthComponent aggregateContributions(
        ApiVersion apiVersion,
        Map<String, HealthComponent> contributions,
        StatusAggregator statusAggregator,
        boolean showComponents,
        Set<String> groupNames
    ) {
      // 1) overall status ONLY from critical components
      Set<Status> criticalStatuses = contributions.entrySet().stream()
          .filter(e -> props.getCritical() != null && props.getCritical().contains(e.getKey()))
          .map(e -> e.getValue().getStatus())
          .collect(Collectors.toSet());

      Status overall = criticalStatuses.isEmpty()
          ? statusAggregator.getAggregateStatus(
              contributions.values().stream().map(HealthComponent::getStatus).collect(Collectors.toSet())
            )
          : statusAggregator.getAggregateStatus(criticalStatuses);

      // 2) still return ALL components with their real statuses (UP/DOWN)
      Map<String, HealthComponent> components = showComponents ? contributions : Map.of();

      // IMPORTANT: do NOT use Health.withDetail(...) or it will become "details"
      // Return a component that serializes to { "status": ..., "components": ... }
      return new HealthzAggregate(overall, components);
    }
  }

  static final class HealthzAggregate implements HealthComponent {

    private final Status status;
    private final Map<String, HealthComponent> components;

    HealthzAggregate(Status status, Map<String, HealthComponent> components) {
      this.status = status;
      this.components = components;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    // Jackson will serialize this as top-level "components"
    public Map<String, HealthComponent> getComponents() {
      return components;
    }
  }
}
