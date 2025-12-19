package com.yourcompany.health;

import org.springframework.boot.actuate.health.*;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@EnableConfigurationProperties(HealthzController.HealthzProperties.class)
public class HealthzController {

    private final HealthEndpoint healthEndpoint;
    private final HealthzProperties props;

    public HealthzController(HealthEndpoint healthEndpoint, HealthzProperties props) {
        this.healthEndpoint = healthEndpoint;
        this.props = props;
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        HealthComponent root = healthEndpoint.health();

        if (!(root instanceof SystemHealth sys)) {
            // fallback (rare)
            return Map.of("status", root.getStatus().getCode());
        }

        Map<String, HealthComponent> components = sys.getComponents();

        // Build response components (actuator-like, includes db)
        Map<String, Object> outComponents = new LinkedHashMap<>();
        for (Map.Entry<String, HealthComponent> e : components.entrySet()) {
            String id = e.getKey();
            HealthComponent hc = e.getValue();

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("status", hc.getStatus().getCode());

            // Include details if available
            if (hc instanceof Health h && h.getDetails() != null && !h.getDetails().isEmpty()) {
                node.put("details", h.getDetails());
            }

            outComponents.put(id, node);
        }

        // Overall: DOWN only if ANY critical is not UP
        boolean criticalDown = props.getCriticalServices().stream()
                .map(components::get)
                .filter(Objects::nonNull)
                .anyMatch(hc -> !Status.UP.equals(hc.getStatus()));

        String overall = criticalDown ? Status.DOWN.getCode() : Status.UP.getCode();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", overall);
        response.put("components", outComponents);
        return response;
    }

    // ---------- static config / properties (nested) ----------

    @ConfigurationProperties(prefix = "healthz")
    public static class HealthzProperties {
        /**
         * Must match component IDs in the actuator health "components" map.
         * Examples: db, admin, money-movement, platform, diskSpace, ping, etc.
         */
        private List<String> criticalServices = new ArrayList<>();

        public List<String> getCriticalServices() {
            return criticalServices;
        }

        public void setCriticalServices(List<String> criticalServices) {
            this.criticalServices = criticalServices;
        }
    }
}
