package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.MissingParameterException;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.ResourceDefinitionService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class CredentialDefinitionService {

    private static final String SELECTOR = "selector";
    private static final String RESOURCE_TYPE = "credential";

    @Inject
    private ResourceDefinitionService definitionService;
    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    public Map<String, Object> processProperties(CloudPlatform cloudPlatform, Map<String, Object> properties) {
        return processValues(getDefinition(cloudPlatform), properties, false);
    }

    public Map<String, Object> revertProperties(CloudPlatform cloudPlatform, Map<String, Object> properties) {
        return processValues(getDefinition(cloudPlatform), properties, true);
    }

    private Definition getDefinition(CloudPlatform cloudPlatform) {
        String json = definitionService.getResourceDefinition(cloudPlatform.name(), RESOURCE_TYPE);
        try {
            return JsonUtil.readValue(json, Definition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> processValues(Definition definition, Map<String, Object> properties, boolean revert) {
        Map<String, Object> processed = new HashMap<>();
        processed.putAll(processValues(properties, definition.getDefaultValues(), revert));
        Object selector = properties.get(SELECTOR);
        if (selector != null) {
            processed.put(SELECTOR, selector);
            processed.putAll(processValues(properties, collectSelectorValues(definition, String.valueOf(selector)), revert));
        }
        return processed;
    }

    private List<Value> collectSelectorValues(Definition definition, String selectorName) {
        List<Value> values = new ArrayList<>();
        List<Selector> selectors = definition.getSelectors();
        String currentSelector = selectorName;
        Selector element;
        while ((element = findSelector(selectors, currentSelector)) != null) {
            values.addAll(element.getValues());
            currentSelector = element.getParent();
        }
        return values;
    }

    private Selector findSelector(List<Selector> selectors, String selector) {
        for (Selector s : selectors) {
            if (s.getName().equals(selector)) {
                return s;
            }
        }
        return null;
    }

    private Map<String, Object> processValues(Map<String, Object> properties, List<Value> values, boolean revert) {
        Map<String, Object> processed = new HashMap<>();
        for (Value value : values) {
            String key = value.getName();
            String property = getProperty(properties, key);
            if (isEncrypted(value)) {
                property = revert ? encryptor.decrypt(property) : encryptor.encrypt(property);
            }
            processed.put(key, property);
        }
        return processed;
    }

    private boolean isEncrypted(Value value) {
        Boolean encrypted = value.getEncrypted();
        return isNotNull(encrypted) && encrypted;
    }

    private boolean isNotNull(Object object) {
        return null != object;
    }

    private String getProperty(Map<String, Object> properties, String key) {
        Object value = properties.get(key);
        if (value == null) {
            throw new MissingParameterException(String.format("Missing '%s' property!", key));
        }
        return String.valueOf(value);
    }

}
