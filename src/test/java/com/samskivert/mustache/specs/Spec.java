package com.samskivert.mustache.specs;

import java.util.Map;

/**
 *
 * @author Yoryos Valotasios
 */
public class Spec {
    private final Map<String, Object> map;
    
    public Spec(Map<String, Object> map) {
        this.map = map;
    }
    
    public String getName() {
        return (String) map.get("name");
    }
    
    public String getTemplate() {
        return (String) map.get("template");
    }
    
    public String getExpectedOutput() {
        return (String) map.get("expected");
    }
    
    public Object getData() {
        return map.get("data");
    }
    
    public String getDescription() {
    	return (String) map.get("desc");
    }
    
    public String getSpecName() {
    	return String.format("%s - %s", getName(), getDescription());
    }
}
