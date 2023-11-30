//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache.specs;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * @author Yoryos Valotasios
 */
public class Spec
{
    private final Map<String, Object> map;
    private final Map<String, String> partials;

    public Spec (Map<String, Object> map) {
        this.map = map;
        @SuppressWarnings("unchecked") Map<String, String> partials =
            (Map<String, String>) map.get("partials");
        if (partials == null) partials = Collections.emptyMap();
        this.partials = partials;
    }

    public String getName () {
        return (String) map.get("name");
    }

    public String getDescription () {
        return (String) map.get("desc");
    }

    public String getTemplate () {
        return (String) map.get("template");
    }

    public String getExpectedOutput () {
        return (String) map.get("expected");
    }

    public Object getData () {
        return map.get("data");
    }

    public String getPartial (String name) {
        return partials == null ? null : partials.get(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Consumer<String> value = s -> sb.append("\"").append(s).append("\"").append("\n");
        Consumer<String> label = s -> sb.append("").append(s).append(": ");
        label.accept("name");
        value.accept(getName());
        label.accept("desc");
        value.accept(getDescription());
        label.accept("template");
        value.accept(getTemplate());
        if (! partials.isEmpty()) {
            label.accept("partials");
            sb.append("\n");
            for( Entry<String, String> e : partials.entrySet()) {
                sb.append("\t").append(e.getKey()).append(":\n");
                value.accept(e.getValue());
            }
            sb.append("\n");
        }
        label.accept("expected");
        value.accept(getExpectedOutput());
        return sb.toString();
    }
}
