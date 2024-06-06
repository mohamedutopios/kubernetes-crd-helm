package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Group("example.com")
@Version("v1")
public class IaCAWS extends CustomResource<IaCAWS.Spec, IaCAWS.Status> implements KubernetesResource {
    @JsonProperty("spec")
    private Spec spec;

    @JsonProperty("status")
    private Status status;

    @Override
    public Spec getSpec() {
        return spec;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public static class Spec {
        @JsonProperty("vpcCidrBlock")
        private String vpcCidrBlock;

        @JsonProperty("ec2InstanceType")
        private String ec2InstanceType;

        @JsonProperty("ec2InstanceName")
        private String ec2InstanceName;

        @JsonProperty("rdsInstanceType")
        private String rdsInstanceType;

        @JsonProperty("rdsInstanceName")
        private String rdsInstanceName;

        @JsonProperty("dbUsername")
        private String dbUsername;

        @JsonProperty("dbPassword")
        private String dbPassword;

        public String getVpcCidrBlock() {
            return vpcCidrBlock;
        }

        public String getEc2InstanceType() {
            return ec2InstanceType;
        }

        public String getEc2InstanceName() {
            return ec2InstanceName;
        }

        public String getRdsInstanceType() {
            return rdsInstanceType;
        }

        public String getRdsInstanceName() {
            return rdsInstanceName;
        }

        public String getDbUsername() {
            return dbUsername;
        }

        public String getDbPassword() {
            return dbPassword;
        }
    }

    public static class Status {
        @JsonProperty("state")
        private String state;

        @JsonProperty("message")
        private String message;

        public String getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }
    }
}


