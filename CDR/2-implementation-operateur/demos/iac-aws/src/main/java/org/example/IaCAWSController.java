package org.example;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceRequest;
import software.amazon.awssdk.services.rds.model.CreateDbInstanceResponse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IaCAWSController {

    public static void main(String[] args) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {

            CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                    .withGroup("example.com")
                    .withVersion("v1")
                    .withScope("Namespaced")
                    .withPlural("iacaws")
                    .build();

            MixedOperation<IaCAWS, IaCAWSList, Resource<IaCAWS>> customResources = client.customResources(crdContext, IaCAWS.class, IaCAWSList.class);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                customResources.watch(new Watcher<IaCAWS>() {
                    @Override
                    public void eventReceived(Action action, IaCAWS resource) {
                        System.out.printf("Event received: %s%n", action.name());
                        if (action == Action.ADDED || action == Action.MODIFIED) {
                            reconcile(resource);
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        System.err.println("Watcher closed due to error: " + cause);
                    }

                    private void reconcile(IaCAWS iaCAWS) {
                        System.out.printf("Reconciling IaCAWS resource: %s%n", iaCAWS.getMetadata().getName());

                        // Extract spec fields
                        IaCAWS.Spec spec = iaCAWS.getSpec();
                        String vpcCidrBlock = spec.getVpcCidrBlock();
                        String ec2InstanceType = spec.getEc2InstanceType();
                        String ec2InstanceName = spec.getEc2InstanceName();
                        String rdsInstanceType = spec.getRdsInstanceType();
                        String rdsInstanceName = spec.getRdsInstanceName();
                        String dbUsername = spec.getDbUsername();
                        String dbPassword = spec.getDbPassword();

                        // Set up AWS clients with EnvironmentVariableCredentialsProvider
                        Ec2Client ec2Client = Ec2Client.builder()
                                .region(Region.US_EAST_1)  // Specify your region
                                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                .build();

                        RdsClient rdsClient = RdsClient.builder()
                                .region(Region.US_EAST_1)  // Specify your region
                                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                .build();

                        // Create VPC
                        CreateVpcRequest vpcRequest = CreateVpcRequest.builder()
                                .cidrBlock(vpcCidrBlock)
                                .build();
                        CreateVpcResponse vpcResponse = ec2Client.createVpc(vpcRequest);
                        Vpc vpc = vpcResponse.vpc();
                        System.out.printf("Created VPC with ID: %s%n", vpc.vpcId());

                        // Create EC2 Instance
                        RunInstancesRequest ec2Request = RunInstancesRequest.builder()
                                .instanceType(ec2InstanceType)
                                .imageId("ami-0abcdef1234567890") // Replace with your AMI ID
                                .minCount(1)
                                .maxCount(1)
                                .build();
                        RunInstancesResponse ec2Response = ec2Client.runInstances(ec2Request);
                        System.out.printf("Created EC2 Instance with ID: %s%n", ec2Response.instances().get(0).instanceId());

                        // Create RDS Instance
                        CreateDbInstanceRequest rdsRequest = CreateDbInstanceRequest.builder()
                                .dbInstanceIdentifier(rdsInstanceName)
                                .dbInstanceClass(rdsInstanceType)
                                .engine("mysql")
                                .masterUsername(dbUsername)
                                .masterUserPassword(dbPassword)
                                .allocatedStorage(20)
                                .build();
                        CreateDbInstanceResponse rdsResponse = rdsClient.createDBInstance(rdsRequest);
                        System.out.printf("Created RDS Instance with ID: %s%n", rdsResponse.dbInstance().dbInstanceIdentifier());
                    }
                });
            });

            executorService.shutdown();
        }
    }
}