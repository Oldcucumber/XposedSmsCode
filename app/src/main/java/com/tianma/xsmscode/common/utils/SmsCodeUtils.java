package com.tianma.xsmscode.common.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.github.tianma8023.xposed.smscode.BuildConfig;
import com.tianma.xsmscode.common.constant.PrefConst;
import com.tianma.xsmscode.data.db.DBProvider;
import com.tianma.xsmscode.data.db.entity.SmsCodeRule;
import com.tianma.xsmscode.data.db.entity.SmsCodeRuleDao;
import com.tianma.xsmscode.feature.store.EntityStoreManager;
import com.tianma.xsmscode.feature.store.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 验证码相关Utils
 */
public class SmsCodeUtils {

    private SmsCodeUtils() {
    }

    /**
     * 是否包含中文
     *
     * @param text text
     */
    private static boolean containsChinese(String text) {
        String regex = "[\u4e00-\u9fa5]|。";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    /**
     * 解析文本内容中的验证码关键字，如果有则返回第一个匹配到的关键字，否则返回 空字符串
     */
    private static String parseKeyword(String keywordsRegex, String content) {
        Pattern pattern = Pattern.compile(keywordsRegex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private static String loadCodeKeywordsBySP(Context context) {
        return SPUtils.getSMSCodeKeywords(context);
    }

    private static String loadCodeKeywordsByXSP() {
        XSharedPreferences preferences = new XSharedPreferences(BuildConfig.APPLICATION_ID, PrefConst.PREF_NAME);
        return XSPUtils.getSMSCodeKeywords(preferences);
    }

    /**
     * 解析文本中的验证码并返回，如果不存在返回空字符
     */
    public static String parseSmsCodeIfExists(Context context, String content, boolean useXSP) {
        List<com.tianma.xsmscode.core.CodeParser.Rule> rules = new ArrayList<>();
        for (SmsCodeRule r : queryAllSmsCodeRules(context)) {
            rules.add(new com.tianma.xsmscode.core.CodeParser.Rule(r.getCompany(), r.getCodeKeyword(), r.getCodeRegex()));
        }
        String keywords = useXSP ? loadCodeKeywordsByXSP() : loadCodeKeywordsBySP(context);
        return com.tianma.xsmscode.core.CodeParser.parse(content, keywords, rules);
    }

    private static List<SmsCodeRule> queryAllSmsCodeRules(Context context) {
        List<SmsCodeRule> rules = new ArrayList<>();
        try {
            Uri smsCodeRuleUri = DBProvider.SMS_CODE_RULE_URI;
            ContentResolver resolver = context.getContentResolver();

            final String companyColumn = SmsCodeRuleDao.Properties.Company.columnName;
            final String keywordColumn = SmsCodeRuleDao.Properties.CodeKeyword.columnName;
            final String regexColumn = SmsCodeRuleDao.Properties.CodeRegex.columnName;

            String[] projection = {
                    companyColumn,
                    keywordColumn,
                    regexColumn,
            };

            Cursor cursor = resolver.query(smsCodeRuleUri, projection, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    SmsCodeRule rule = new SmsCodeRule();
                    rule.setCompany(cursor.getString(cursor.getColumnIndexOrThrow(companyColumn)));
                    rule.setCodeKeyword(cursor.getString(cursor.getColumnIndexOrThrow(keywordColumn)));
                    rule.setCodeRegex(cursor.getString(cursor.getColumnIndexOrThrow(regexColumn)));
                    rules.add(rule);
                }
                cursor.close();
                XLog.d("Load SmsCode rules succeed by content provider");
            } else {
                throw new Exception("Cursor is null");
            }
        } catch (Throwable e) {
            rules = EntityStoreManager.loadEntitiesFromFile(
                    EntityType.CODE_RULES, SmsCodeRule.class
            );
            XLog.d("Load SmsCode rules by file");
        }
        return rules;
    }

    /**
     * Parse company info from message content if it exists
     *
     * @param content message content
     * @return company info if it exists, otherwise return empty string
     */
    public static String parseCompany(String content) {
        return com.tianma.xsmscode.core.CodeParser.parseCompany(content);
    }
}
