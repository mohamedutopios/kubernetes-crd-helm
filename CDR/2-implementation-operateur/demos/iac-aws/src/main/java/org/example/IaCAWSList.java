package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.openapi.models.V1ListMeta;

import java.util.List;

import io.fabric8.kubernetes.client.CustomResourceList;

public class IaCAWSList extends CustomResourceList<IaCAWS> {
}
