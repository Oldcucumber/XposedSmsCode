package com.tianma.xsmscode.core;

import com.tianma.xsmscode.common.constant.SmsCodeConst;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Synthetic examples only; no personal SMS bodies or codes are included. */
public class BuiltInCodeParserTest {
    private String parse(String text) {
        return CodeParser.parse(text, SmsCodeConst.VERIFICATION_KEYWORDS_REGEX, Collections.emptyList());
    }

    @Test public void productNamesAndLinkParametersAreNotKeywords() {
        assertEquals("", parse("【示例云】Mate新品促销，优惠1234元。"));
        assertEquals("", parse("【CodeGeeX】下载工具领取123456积分。"));
        assertEquals("", parse("【示例】体验DuMate，每日赠送1234积分。"));
        assertEquals("", parse("请评价 https://example.invalid/survey?code=123456"));
        assertEquals("", parse("请评价 www.example.invalid/?code=123456"));
    }

    @Test public void validCodesSurviveLinksAndFraudWarnings() {
        assertEquals("123456", parse("【示例银行】验证码：123456，有效期5分钟，请勿泄露，谨防诈骗。客服95555。"));
        assertEquals("2468", parse("验证码2468，帮助 https://example.invalid/?code=987654"));
        assertEquals("2468", parse("帮助https://example.invalid/help，验证码2468"));
        assertEquals("2468", parse("https://example.invalid/help验证码2468"));
        assertEquals("123456", parse("验证码：1 2 3 4 5 6，5分钟有效"));
        assertEquals("AB12CD", parse("【示例】验证码：AB12CD，请勿转发"));
        assertEquals("ABCD", parse("【示例】验证码：ABCD"));
        assertEquals("13579", parse("【示例餐厅】上网密码：13579"));
    }

    @Test public void commonKeywordVariantsAndCompactFormsWork() {
        for (String word : Arrays.asList("Code", "code", "CODE", "cOdE", "OTP", "otp", "Otp",
                "Код", "код", "КОД", "Kod", "Ma", "Mã", "验证码", "驗證碼",
                "登录码", "登陆码", "登錄碼", "一次性密码", "一次性密碼", "一次性口令")) {
            assertEquals(word, "123456", parse(word + ": 123456"));
        }
        assertEquals("123456", parse("code123456"));
        assertEquals("123456", parse("您的code:123456"));
        assertEquals("", parse("decode:123456"));
        assertEquals("", parse("passcode:123456")); // Not a new keyword in this conservative update.
    }

    @Test public void savedDefaultsUpgradeWithoutChangingCustomRegexes() {
        assertEquals("", CodeParser.parse("Mate促销1234元", SmsCodeConst.LEGACY_VERIFICATION_KEYWORDS_REGEX,
                Collections.emptyList()));
        assertEquals("123456", CodeParser.parse("登录码：123456", SmsCodeConst.LEGACY_VERIFICATION_KEYWORDS_REGEX,
                Collections.emptyList()));
        assertEquals("123456", CodeParser.parse("https://example.invalid/?code=123456", "code",
                Collections.emptyList()));
        List<CodeParser.Rule> rules = Arrays.asList(new CodeParser.Rule("example.invalid", "code", "(?<=code=)[0-9]{6}"));
        assertEquals("123456", CodeParser.parse("https://example.invalid/?code=123456",
                SmsCodeConst.VERIFICATION_KEYWORDS_REGEX, rules));
    }

    @Test public void maskedUrlsCannotJoinSeparateDigitGroups() {
        assertEquals("", parse("验证码12https://example.invalid/x，34"));
    }

    @Test public void newOneTimePasswordFormatsUseTheLabelNotTransactionNumbers() {
        assertEquals("87654321", parse("【示例銀行】登入手機銀行\n一次性密碼 87654321\n交易序號 1234-5678\n12/09/26 10:30"));
        assertEquals("87654321", parse("【示例銀行】一次性密碼：87654321\n交易序號：1234-5678\n12/09/2026 10:30"));
        assertEquals("246810", parse("您的一次性密碼為246810，密碼會於180秒後逾期"));
        assertEquals("", parse("請勿透露一次性密碼。交易序號1234-5678"));
        assertEquals("", parse("登录码：123456789，客服电话95555"));
    }
}
