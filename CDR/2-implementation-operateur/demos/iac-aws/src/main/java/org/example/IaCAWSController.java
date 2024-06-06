package org.example;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class IaCAWSController {

    public static void main(String[] args) {
        Config config = new Config();
        config.setRequestTimeout(60000);  // 60 seconds
        config.setConnectionTimeout(30000);  // 30 seconds

        List<Watch> watches = new ArrayList<>();

        try (KubernetesClient client = new DefaultKubernetesClient(config)) {

            CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                    .withGroup("example.com")
                    .withVersion("v1")
                    .withScope("Namespaced")
                    .withPlural("iacaws")
                    .build();

            MixedOperation<IaCAWS, IaCAWSList, Resource<IaCAWS>> customResources = client.customResources(crdContext, IaCAWS.class, IaCAWSList.class);

            int corePoolSize = 10;  // Minimum number of threads to keep in the pool
            int maximumPoolSize = 50;  // Maximum number of threads in the pool
            long keepAliveTime = 60L;  // Time to keep excess idle threads alive
            LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
            RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

            ExecutorService executorService = new ThreadPoolExecutor(
                    corePoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    TimeUnit.SECONDS,
                    queue,
                    rejectedExecutionHandler
            );

            // Création et stockage des watchers
            watches.add(customResources.watch(new IaCAWSWatcher(executorService, customResources)));
            // Ajoutez d'autres watchers si nécessaire, par exemple pour les pods, services, etc.
            // watches.add(client.pods().watch(new PodWatcher()));
            // watches.add(client.services().watch(new ServiceWatcher()));
            // watches.add(client.apps().deployments().watch(new DeploymentWatcher()));

            // Le contrôleur continue de fonctionner
            while (true) {
                Thread.sleep(10000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Fermeture propre des watchers
            watches.forEach(Watch::close);
        }
    }

    private static class IaCAWSWatcher implements Watcher<IaCAWS> {

        private final ExecutorService executorService;
        private final MixedOperation<IaCAWS, IaCAWSList, Resource<IaCAWS>> customResources;

        public IaCAWSWatcher(ExecutorService executorService, MixedOperation<IaCAWS, IaCAWSList, Resource<IaCAWS>> customResources) {
            this.executorService = executorService;
            this.customResources = customResources;
        }

        @Override
        public void eventReceived(Action action, IaCAWS resource) {
            System.out.printf("Event received: %s%n", action.name());
            if (action == Action.ADDED || action == Action.MODIFIED) {
                executorService.submit(() -> reconcile(resource));
            }
        }

        @Override
        public void onClose(WatcherException cause) {
            System.err.println("Watcher closed due to error: " + cause);
            if (cause != null) {
                cause.printStackTrace();
            }

            // Tentative de reconnexion
            reconnectWatcher();
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

            System.out.printf("Spec - VPC CIDR Block: %s, EC2 Instance Type: %s, EC2 Instance Name: %s, RDS Instance Type: %s, RDS Instance Name: %s, DB Username: %s%n",
                    vpcCidrBlock, ec2InstanceType, ec2InstanceName, rdsInstanceType, rdsInstanceName, dbUsername);

            // Set up AWS clients with EnvironmentVariableCredentialsProvider
            try (Ec2Client ec2Client = Ec2Client.builder()
                    .region(Region.US_EAST_1)  // Specify your region
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build();
                 RdsClient rdsClient = RdsClient.builder()
                         .region(Region.US_EAST_1)  // Specify your region
                         .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                         .build()) {

                // Create VPC
                try {
                    CreateVpcRequest vpcRequest = CreateVpcRequest.builder()
                            .cidrBlock(vpcCidrBlock)
                            .build();
                    System.out.printf("Creating VPC with CIDR Block: %s%n", vpcCidrBlock);
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
                    System.out.printf("Creating EC2 Instance with Type: %s, Name: %s%n", ec2InstanceType, ec2InstanceName);
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
                    System.out.printf("Creating RDS Instance with Identifier: %s, Type: %s%n", rdsInstanceName, rdsInstanceType);
                    CreateDbInstanceResponse rdsResponse = rdsClient.createDBInstance(rdsRequest);
                    System.out.printf("Created RDS Instance with ID: %s%n", rdsResponse.dbInstance().dbInstanceIdentifier());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.printf("Failed to reconcile IaCAWS resource: %s%n", iaCAWS.getMetadata().getName());
                }
            }
        }

        private void reconnectWatcher() {
            // Tentative de reconnexion avec un backoff exponentiel
            int maxRetries = 10;
            long backoff = 1000; // 1 seconde de backoff initial
            for (int i = 0; i < maxRetries; i++) {
                try {
                    System.out.println("Reconnecting watcher...");
                    customResources.watch(this);
                    System.out.println("Watcher reconnected successfully");
                    return;
                } catch (Exception e) {
                    System.err.println("Failed to reconnect watcher: " + e.getMessage());
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    backoff *= 2; // Backoff exponentiel
                }
            }
            System.err.println("Max retries reached, could not reconnect watcher");
        }
    }
}
