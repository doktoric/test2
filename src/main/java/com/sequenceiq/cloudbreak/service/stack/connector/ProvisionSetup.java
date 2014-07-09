package com.sequenceiq.cloudbreak.service.stack.connector;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ProvisionSetup {

    void setupProvisioning(Stack stack);

    CloudPlatform getCloudPlatform();

}