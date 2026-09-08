package com.tianma.xsmscode.data.db.entity;

import org.junit.Test;
import java.util.HashSet;
import static org.junit.Assert.*;

public class SmsMsgTest {
    @Test public void unsavedMessagesHaveDistinctIdentity() {
        SmsMsg a = new SmsMsg();
        SmsMsg b = new SmsMsg();
        assertEquals(a, a);
        assertNotEquals(a, b);
        HashSet<SmsMsg> values = new HashSet<>();
        values.add(a); values.add(b);
        assertEquals(2, values.size());
    }
    @Test public void savedIdentityDoesNotDependOnMutableBody() {
        SmsMsg a = new SmsMsg(); a.setId(1L); a.setBody("first");
        SmsMsg b = new SmsMsg(); b.setId(1L); b.setBody("edited");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
