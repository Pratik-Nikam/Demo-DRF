@ConfigurationProperties(prefix = "healthz")
public class HealthzProperties {
  private Set<String> critical = Set.of("admin", "moneyMovement");
  public Set<String> getCritical() { return critical; }
  public void setCritical(Set<String> critical) { this.critical = critical; }
}

@Configuration
@EnableConfigurationProperties(HealthzProperties.class)
public class HealthzAggregationConfig {

  @Bean
  public HealthEndpointWebExtension healthEndpointWebExtension(
      HealthContributorRegistry registry,
      HealthEndpointGroups groups,
      HealthzProperties props
  ) {
    // keep Boot’s default slow threshold if you don’t care, or inject it via config
    return new CriticalOnlyAggregationHealthWebExtension(registry, groups, Duration.ofSeconds(3), props);
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
      // 1) compute overall status ONLY from critical components
      var criticalStatuses = contributions.entrySet().stream()
          .filter(e -> props.getCritical().contains(e.getKey()))
          .map(e -> e.getValue().getStatus())
          .toList();

      Status overall = statusAggregator.getAggregateStatus(criticalStatuses);

      // 2) still return ALL components with their real statuses
      Map<String, HealthComponent> components = showComponents ? contributions : Map.of();

      return new AggregatedHealth(overall, components);
    }
  }

  // Minimal HealthComponent that serializes like:
  // { "status": "...", "components": { ... } }
  static final class AggregatedHealth extends HealthComponent {
    private final Status status;
    private final Map<String, HealthComponent> components;

    AggregatedHealth(Status status, Map<String, HealthComponent> components) {
      this.status = status;
      this.components = components;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    public Map<String, HealthComponent> getComponents() {
      return components;
    }
  }
}
