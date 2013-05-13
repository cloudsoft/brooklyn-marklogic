package io.cloudsoft.marklogic.nodes;

import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import com.google.common.collect.ImmutableList;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;

import java.util.List;

/**
 * A customizer that calls a sequence of other customizers.
 */
public class CompoundJcloudsLocationCustomizer implements JcloudsLocationCustomizer {

    private final List<JcloudsLocationCustomizer> delegates;

    public CompoundJcloudsLocationCustomizer(List<JcloudsLocationCustomizer> delegates) {
        this.delegates = ImmutableList.copyOf(delegates);
    }

    public void customize(ComputeService computeService, TemplateBuilder templateBuilder) {
        for (JcloudsLocationCustomizer delegate : delegates) {
            delegate.customize(computeService, templateBuilder);
        }
    }

    public void customize(ComputeService computeService, TemplateOptions templateOptions) {
        for (JcloudsLocationCustomizer delegate : delegates) {
            delegate.customize(computeService, templateOptions);
        }
    }

    public void customize(ComputeService computeService, JcloudsSshMachineLocation machine) {
        for (JcloudsLocationCustomizer delegate : delegates) {
            delegate.customize(computeService, machine);
        }
    }
}
