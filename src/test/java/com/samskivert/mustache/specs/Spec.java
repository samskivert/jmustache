//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache.specs;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

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
        this.partials = partials == null ? Collections.emptyMap() : partials;
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
        appendField(sb, "name", getName());
        appendField(sb, "desc", getDescription());
        appendField(sb, "template", getTemplate());
        if (! partials.isEmpty()) {
            appendLabel(sb, "partials");
            sb.append("\n");
            for( Entry<String, String> e : partials.entrySet()) {
                sb.append("\t").append(e.getKey()).append(":\n");
                appendQuotedValue(sb, e.getValue());
            }
            sb.append("\n");
        }
        appendField(sb, "expected", getExpectedOutput());
        return sb.toString();
    }

    private void appendField (StringBuilder sb, String label, String value) {
        appendLabel(sb, label);
        appendQuotedValue(sb, value);
    }

    private void appendLabel (StringBuilder sb, String label) {
        sb.append(label).append(": ");
    }

    private void appendQuotedValue (StringBuilder sb, String value) {
        sb.append("\"").append(value).append("\"").append("\n");
    }
}
