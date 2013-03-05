package com.samskivert.mustache.specs;

import com.samskivert.mustache.Mustache;
import java.util.Map;

/**
 *
 * @author valotas
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
    
    public void testWith(Mustache mustache) {
        
    }
}
