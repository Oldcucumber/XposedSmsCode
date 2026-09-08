package com.tianma.xsmscode.core;

import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class BoundedParserTest {
    @Test public void hungParserDoesNotSpawnMoreWorkers() throws Exception {
        BoundedParser parser = new BoundedParser();
        CountDownLatch release = new CountDownLatch(1);
        try {
            assertNull(parser.evaluate(() -> {
                while (release.getCount() != 0) {
                    try { release.await(); } catch (InterruptedException ignored) { }
                }
                return "late";
            }, 20));
            for (int i = 0; i < 100; i++) assertNull(parser.evaluate(() -> "unsafe", 20));
        } finally { release.countDown(); parser.close(); }
    }
    @Test public void fastResultAndExceptions() {
        BoundedParser parser = new BoundedParser();
        try { assertEquals("123456", parser.evaluate(() -> "123456", 200)); }
        finally { parser.close(); }
    }
}
