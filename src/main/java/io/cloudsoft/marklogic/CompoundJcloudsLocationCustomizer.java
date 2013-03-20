package io.cloudsoft.marklogic;

import java.util.List;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;

import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;

import com.google.common.collect.ImmutableList;

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
