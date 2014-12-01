package com.samskivert.mustache;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertEquals;

public class CodeGenerationTest {

    @Test
    public void testJavaSourceGeneration() throws Exception {

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("package", "com.example");
        ctx.put("class", "Configuration");
        ctx.put("import", asList("java.lang.*", "java.util.*"));
        ctx.put("properties", asList(p("foo"), p("bar"), p("baz")));

        String generatedCode = Mustache.compiler().compile(res("/java_src.tpl", "UTF-8")).execute(ctx);
        String expected = res("/java_expected.tpl", "UTF-8");

        assertEquals(expected, generatedCode);
    }

    private Property p(String name) {
        return new Property(name);
    }

    private static class Property {

        private final String name;

        private Property(String name) {
            this.name = name;
        }

        public String getGetter() {
            return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        public String getSetter() {
            return "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    private String res(String resource, String charset) throws IOException {
        StringBuilder str = new StringBuilder();
        Reader in = new InputStreamReader(CodeGenerationTest.class.getResourceAsStream(resource), charset);
        char[] buffer = new char[4096];
        int read;
        while ((read = in.read(buffer)) > 0) str.append(buffer, 0, read);
        in.close();
        return str.toString();
    }
}
