package com.tianma.xsmscode.core;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CodeParserTest {
    private String parse(String s) { return CodeParser.parse(s, "验证码|code", Collections.emptyList()); }
    @Test public void originalChineseAndEnglishScoring() {
        assertEquals("123456", parse("【银行】您的验证码为123456，5分钟内有效"));
        assertEquals("876543", parse("Your code is 876543, expires in 10 minutes."));
        assertEquals("", parse("今天下午两点开会"));
        assertEquals("", parse(null));
    }
    @Test public void ruleOrderAndJavaLookaroundArePreserved() {
        List<CodeParser.Rule> rules = Arrays.asList(
            new CodeParser.Rule("Bank", "code", "(?<=token=)[A-Z]{4}"),
            new CodeParser.Rule("Bank", "code", "[0-9]{6}"));
        assertEquals("ABCD", CodeParser.parse("Bank code token=ABCD 123456", "code", rules));
    }
    @Test public void badRuleDoesNotSuppressFollowingRule() {
        List<CodeParser.Rule> rules = Arrays.asList(
            new CodeParser.Rule("", "", "["), new CodeParser.Rule("", "", "[0-9]{6}"));
        assertEquals("123456", CodeParser.parse("code 123456", "[", rules));
        assertEquals("", CodeParser.parse("code 123456", "[", Collections.emptyList()));
    }
    @Test public void originalDecimalAndDistanceBehaviorIsIntentionallyUnchanged() {
        assertEquals("123456", parse("code 123456.231"));
        assertEquals("123456", parse("code 123456 or 987654"));
    }
}
