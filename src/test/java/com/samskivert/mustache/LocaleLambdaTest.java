package com.samskivert.mustache;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.samskivert.mustache.Template.Fragment;

public class LocaleLambdaTest {
    
    @Test
    public void testLocaleSpike() {
        Map<String, Object> model = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Michael");
        user.put("balance", new BigDecimal("5.50"));
        model.put("user", user);

        /*
         * This is our pretend resource bundle.
         */
        Map<String,String> messages = new HashMap<>();
        messages.put("hello.welcome", "Hello {0}, & btw you owe {1,number,currency}!");
        
        /*
         * Here is our magic i18n
         */
        MessageLambda ml = new MessageLambda(messages::get, Locale.US, Escapers.HTML);
        model.put("@message", ml);
        String template = "{{#@message}}hello.welcome(user.name,user.balance){{/@message}}";
        String actual = Mustache.compiler().compile(template).execute(model);
        String expected = "Hello Michael, &amp; btw you owe $5.50!";
        assertEquals(expected, actual);
    }
    static class MessageLambda implements Mustache.Lambda {
        
        private final Function<String,String> bundle;
        private final Locale locale;
        private final Mustache.Escaper escaper;
       
        public MessageLambda(Function<String,String> bundle, Locale locale, Mustache.Escaper escaper) {
            super();
            this.bundle = bundle;
            this.locale = locale;
            this.escaper = escaper;
        }

        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            String body = frag.decompile();
            MessageFunction function = parseDSL(body);
            String key = function.getKey();
            String message = bundle.apply(key);
            if (message == null) {
                throw new RuntimeException("Bundle missing key: " + key);
            }
            MessageFormat mf = new MessageFormat(message, locale);
            /*
             * Replace the args with values from the context.
             */
            Object[] args = function.getParams().stream().map(k -> frag.valueOrNull(k)).toArray();
            String response = mf.format(args);
            escaper.escape(out, response);
        }
        
    }
    
    /*
     * Our format is 
     * 
     * key(param,...)
     * 
     */
    public static MessageFunction parseDSL(String input) {
        if (! input.contains("(")) {
            return new MessageFunction(input, Collections.emptyList());
        }
        // Chat GPT wrote this garbage but it looks good to me... well after I edited.
        // Regular expression pattern to match the DSL syntax
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9\\.\\-_]+)\\((.*?)\\)$");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            String key = matcher.group(1);
            String paramsStr = matcher.group(2);

            List<String> params = parseParameters(paramsStr);
            return new MessageFunction(key, params);
        }

        return null; // Invalid DSL syntax
    }

    private static List<String> parseParameters(String paramsStr) {
        List<String> params = new ArrayList<>();
        
        // Split parameters by commas
        String[] paramTokens = paramsStr.split(",");
        
        // Add each parameter to the list
        for (String param : paramTokens) {
            params.add(param.trim()); // Remove any surrounding whitespace
        }
        
        return params;
    }
    
    static class MessageFunction {
        private String key;
        private List<String> params;

        public MessageFunction(String key, List<String> params) {
            this.key = key;
            this.params = params;
        }

        public String getKey() {
            return key;
        }

        public List<String> getParams() {
            return params;
        }
    }

}
