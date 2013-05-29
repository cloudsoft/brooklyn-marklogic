package brooklyn.location.volumes;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.Attachment;
import org.jclouds.ec2.domain.Volume;
import org.jclouds.ec2.features.TagApi;
import org.jclouds.ec2.options.DetachVolumeOptions;
import org.jclouds.ec2.services.ElasticBlockStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.util.internal.Repeater;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * For managing EBS volumes via EC2-compatible APIs.
 */
public class EbsVolumeManager extends AbstractVolumeManager {

    private static final Logger LOG = LoggerFactory.getLogger(EbsVolumeManager.class);

    @Override
    public String createVolume(JcloudsLocation location, String availabilityZone, int size, Map<String,String> tags) {
        LOG.info("Creating volume: location={}; availabilityZone={}; size={}", new Object[] {location, availabilityZone, size});
        
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        TagApi tagClient = ec2Client.getTagApi().get();
        
        Volume volume = ebsClient.createVolumeInAvailabilityZone(availabilityZone, size);
        if (tags != null && tags.size() > 0) {
        	tagClient.applyToResources(tags, ImmutableList.of(volume.getId()));
        }
        
        return volume.getId();
    }
    
    @Override
    public void attachVolume(JcloudsSshMachineLocation machine, String volumeId, String ec2DeviceName) {
        LOG.info("Attaching volume: machine={}; volume={}; ec2DeviceName={}", new Object[] {machine, volumeId, ec2DeviceName});
        
    	JcloudsLocation location = machine.getParent();
    	String region = location.getRegion();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        Attachment attachment = ebsClient.attachVolumeInRegion(region, volumeId, machine.getNode().getProviderId(), ec2DeviceName);
        // TODO return attachment.getId();
    }

    @Override
    public void detachVolume(JcloudsSshMachineLocation machine, final String volumeId, String ec2DeviceName) {
        LOG.info("Detaching volume: machine={}; volume={}; ec2DeviceName={}", new Object[] {machine, volumeId, ec2DeviceName});
        
        final JcloudsLocation location = machine.getParent();
    	String region = location.getRegion();
    	String instanceId = machine.getNode().getProviderId();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
		ebsClient.detachVolumeInRegion(region, volumeId, true, DetachVolumeOptions.Builder.fromDevice(ec2DeviceName).fromInstance(instanceId));
		
		// Wait for detached
		boolean detached = Repeater.create("wait for detached "+volumeId+" from "+machine)
				.every(1, TimeUnit.SECONDS)
				.limitTimeTo(60, TimeUnit.SECONDS)
				//.repeat(Callables.returning(null))
				.until(new Callable<Boolean>() {
					@Override public Boolean call() throws Exception {
						Volume volume = describeVolume(location, volumeId);
						return volume.getStatus() == Volume.Status.AVAILABLE;
					}})
				.run();
		
		if (!detached) {
			LOG.error("Volume {}->{} still not detached from {}; continuing...", new Object[] {volumeId, ec2DeviceName, machine});
		}
    }
    
    @Override
    public void deleteVolume(JcloudsLocation location, String volumeId) {
        LOG.info("Deleting volume: location={}; volume={}", new Object[] {location, volumeId});
        
    	String region = location.getRegion();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        ebsClient.deleteVolumeInRegion(region, volumeId);
    }
    
    /**
     * Describes the given volume. Or returns null if it is not found.
     */
    public Volume describeVolume(JcloudsLocation location, String volumeId) {
        if (LOG.isDebugEnabled()) LOG.debug("Describing volume: location={}; volume={}", new Object[] {location, volumeId});
        
    	String region = location.getRegion();
        EC2Client ec2Client = location.getComputeService().getContext().unwrap(EC2ApiMetadata.CONTEXT_TOKEN).getApi();
        ElasticBlockStoreClient ebsClient = ec2Client.getElasticBlockStoreServices();
        Set<Volume> volumes = ebsClient.describeVolumesInRegion(region, volumeId);
        return Iterables.getFirst(volumes, null);
    }
}
