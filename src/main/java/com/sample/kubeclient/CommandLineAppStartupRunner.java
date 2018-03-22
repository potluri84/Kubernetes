package com.sample.kubeclient;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineAppStartupRunner.class);
    @Override
    public void run(String...args) throws Exception {
        logger.info("Application started with command-line arguments: " + Arrays.toString(args));

        String master = "https://10.60.3.102:8443/";
        String image = "localhost:5000/perfchecker";
        int numberOfContainers = 2;
        int runTime = 1;
        if (args.length == 1) {
            master = args[0];
            image = args[1];
            numberOfContainers = Integer.parseInt(args[2]);
            runTime = Integer.parseInt(args[3]);
        }
        Config config = new ConfigBuilder().withMasterUrl(master).build();
        KubernetesClient client = new DefaultKubernetesClient(config);
        CreatePerfCheckerPod(client,image,numberOfContainers,runTime);

    }


    private void CreatePerfCheckerPod(KubernetesClient client,String image,int numberOfContainers,int runTime) throws InterruptedException {
        try {
            // Create a namespace for all our stuff
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName("perfchecker").addToLabels("perfchecker", "latest").endMetadata().build();
            log("Created namespace", client.namespaces().createOrReplace(ns));

            ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata().withName("fabric8").endMetadata().build();

            client.serviceAccounts().inNamespace("perfchecker").createOrReplace(fabric8);

            List<Deployment> deployments = new ArrayList<Deployment>();
            Integer firstId = 111;
            Integer secondId = 123;
            for(int i=0;i<numberOfContainers;i++) {
                String name = "perfchecker" + i;
                Deployment deployment = new DeploymentBuilder()
                        .withNewMetadata()
                        .withName(name)
                        .endMetadata()
                        .withNewSpec()
                        .withReplicas(1)
                        .withNewTemplate()
                        .withNewMetadata()
                        .addToLabels("app", name)
                        .endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        .withName(name)
                        .withImage(image)
                        .withArgs(new String[]{"5000", "50", String.valueOf(firstId), String.valueOf(secondId), String.valueOf(runTime)})
                        .endContainer()
                        .endSpec()
                        .endTemplate()
                        .endSpec()
                        .build();
                deployment = client.extensions().deployments().inNamespace("perfchecker").create(deployment);
                log("Created deployment", deployment);
                deployments.add(deployment);
                dataSourceId++;
                tenantId++;
            }
            Thread.sleep(TimeUnit.MINUTES.toMillis(runTime + 5)); //sleeping for duration plus 5 minutes

                for(int i=0;i<deployments.size();i++) {
                    System.err.println("Deleting:" + deployments.get(i).getMetadata().getName());
                    client.resource(deployments.get(i)).delete();
                }

            log("Done.");

        }finally {
            client.namespaces().withName("perfchecker").delete();
            client.close();
        }
    }




    private void CreatePod(KubernetesClient client) {

        try {
            // Create a namespace for all our stuff
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName("podcreation").addToLabels("this", "rocks").endMetadata().build();
            log("Created namespace", client.namespaces().createOrReplace(ns));

            ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata().withName("fabric8").endMetadata().build();

            client.serviceAccounts().inNamespace("podcreation").createOrReplace(fabric8);

            ContainerPort port = new ContainerPort();
            port.setContainerPort(80);
            List<ContainerPort> ports = new ArrayList<ContainerPort>();
            ports.add(port);

            Container container = new ContainerBuilder().withImage("nginx").withPorts(ports).build();

            PodSpec podSpec = new PodSpecBuilder().withContainers(container).withServiceAccountName("fabric8").build();
            Map<String, String> selector = new HashMap<String,String>();
            selector.put("app","nginx");
            Deployment deployment = new DeploymentBuilder().withNewMetadata().withName("nginx").withNamespace("podcreation")
                    .endMetadata().withNewSpec().withReplicas(1).withNewSelector().withMatchLabels(selector).endSelector().withNewTemplate().withNewMetadata().withLabels(selector).endMetadata().withSpec(podSpec).endTemplate().endSpec().build();
            deployment = client.extensions().deployments().inNamespace("podcreation").create(deployment);

            log("Created deployment", deployment);
            System.err.println("Deleting:" + deployment.getMetadata().getName());
            client.resource(deployment).delete();
            log("Done.");

        }catch (Exception ex)
        {
            log(ex.getMessage());
        }
        finally {
            client.namespaces().withName("podcreation").delete();
            client.close();
        }


    }

    private void CreateNamespace(KubernetesClient client) {
        try {
            // Create a namespace for all our stuff
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName("thisisatest").addToLabels("this", "rocks").endMetadata().build();
            log("Created namespace", client.namespaces().createOrReplace(ns));

            ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata().withName("fabric8").endMetadata().build();

            client.serviceAccounts().inNamespace("thisisatest").createOrReplace(fabric8);
            for (int i = 0; i < 2; i++) {
                System.err.println("Iteration:" + (i+1));
                Deployment deployment = new DeploymentBuilder()
                        .withNewMetadata()
                        .withName("nginx")
                        .endMetadata()
                        .withNewSpec()
                        .withReplicas(1)
                        .withNewTemplate()
                        .withNewMetadata()
                        .addToLabels("app", "nginx")
                        .endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        .withName("nginx")
                        .withImage("nginx")
                        .addNewPort()
                        .withContainerPort(80)
                        .endPort()
                        .endContainer()
                        .endSpec()
                        .endTemplate()
                        .endSpec()
                        .build();

                deployment = client.extensions().deployments().inNamespace("thisisatest").create(deployment);
                log("Created deployment", deployment);

                System.err.println("Scaling up:" + deployment.getMetadata().getName());
                client.extensions().deployments().inNamespace("thisisatest").withName("nginx").scale(2, true);
                log("Created replica sets:", client.extensions().replicaSets().inNamespace("thisisatest").list().getItems());
                System.err.println("Deleting:" + deployment.getMetadata().getName());
                client.resource(deployment).delete();
            }
            log("Done.");

        }finally {
            client.namespaces().withName("thisisatest").delete();
            client.close();
        }
    }

    private static void log(String action, Object obj) {
        logger.info("{}: {}", action, obj);
    }

    private static void log(String action) {
        logger.info(action);
    }
}
