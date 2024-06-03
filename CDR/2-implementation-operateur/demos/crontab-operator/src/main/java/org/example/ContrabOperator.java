package org.example;

/**
 * Hello world!
 *
 */
public class ContrabOperator 
{
    public static void main(String[] args) {
        KubernetesClient client = new KubernetesClientBuilder().build();
        Operator operator = new Operator(client, ConfigurationServiceProvider.instance());

        try {
            operator.register(new CronTabReconciler(client));
            operator.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
