package com.samskivert.mustache;

/**
 * Compiler options for Mustache.  Use the {@link Builder} via {{@link #builder()}} to create.
 *
 * <p>The standards mode setting tells JMustache to more closely follow the spec.  Specifically, this means:
 * <ul>
 *     <li>{{.}} will refer to the current node rather than {{this}}</li>
 *     <li>Referring to non-existent variables in loops will be treated as false rather than throwing an exception</li>
 *     <li>The parent contexts will not be searched if the value is not found in the current one</li>
 * </ul>
 *
 * @since 1.1
 */
public class Options {
    private final boolean escapeHTML;
    private final boolean standardsMode;

    private Options (boolean escapeHTML, boolean standardsMode) {
        this.escapeHTML = escapeHTML;
        this.standardsMode = standardsMode;
    }

    public boolean isEscapeHTML () {
        return escapeHTML;
    }

    public boolean isStandardsMode () {
        return standardsMode;
    }

    public static Builder builder () {
        return new Builder();
    }

    public static class Builder {
        private boolean escapeHTML = true;
        private boolean standardsMode = false;

        /**
         * @param escapeHTML whether HTML entities should be escaped or not
         */
        public Builder setEscapeHTML (boolean escapeHTML) {
            this.escapeHTML = escapeHTML;
            return this;
        }

        /**
         * @param standardsMode whether templates should be processed in strict accordance with the spec. Defaults to false.
         */
        public Builder setStandardsMode (boolean standardsMode) {
            this.standardsMode = standardsMode;
            return this;
        }

        public Options build () {
            return new Options(escapeHTML, standardsMode);
        }
    }
}
