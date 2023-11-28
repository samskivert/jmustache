//
// JMustache - A Java implementation of the Mustache templating language
// http://github.com/samskivert/jmustache/blob/master/LICENSE

package com.samskivert.mustache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.samskivert.mustache.Mustache.TemplateLoader;

public class PartialThreadSafeTest {

    @Test
    public void testPartialThreadSafe() throws Exception {
        long t = System.currentTimeMillis();
        AtomicInteger loadCount = new AtomicInteger();
        TemplateLoader loader = new TemplateLoader() {
            @Override
            public Reader getTemplate(String name) throws Exception {
                if ("partial".equals(name)) {
                    loadCount.incrementAndGet();
                    TimeUnit.MILLISECONDS.sleep(20);
                    return new StringReader("Hello");
                }
                throw new IOException(name);
            }
        };

        Template template = Mustache.compiler().withLoader(loader).compile("{{stuff}}\n\t{{> partial }}");
        ExecutorService executor = Executors.newFixedThreadPool(64);
        ConcurrentLinkedDeque<Exception> q = new ConcurrentLinkedDeque<>();

        Map<String, Object> m = new HashMap<>();
        m.put("stuff", "Foo");
        for (int i = 100; i > 0; i--) {
            int ii = i;
            executor.execute(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(ii % 10);
                    template.execute(m);
                } catch (Exception e) {
                    q.add(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(10_000, TimeUnit.MILLISECONDS);
        if (!q.isEmpty()) {
            System.out.println(q);
        }
        assertTrue(q.isEmpty());
        assertEquals(1, loadCount.get());
        System.out.println(loadCount);
        System.out.println(System.currentTimeMillis() - t);
    }
}
