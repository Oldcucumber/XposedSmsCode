package com.tianma.xsmscode.core;

import java.util.*;
import java.util.regex.*;

/** Original scoring and matching semantics, independent of Android and Xposed. */
public final class CodeParser {
    private static final Map<String, Pattern> CACHE = new LinkedHashMap<String, Pattern>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Pattern> e) { return size() > 128; }
    };
    private static synchronized Pattern compile(String regex) {
        Pattern p = CACHE.get(regex);
        if (p == null) { p = Pattern.compile(regex); CACHE.put(regex, p); }
        return p;
    }
    public static final class Rule {
        public String company;
        public String keyword;
        public String regex;
        public Rule(String company, String keyword, String regex) {
            this.company = company; this.keyword = keyword; this.regex = regex;
        }
    }
    private static boolean empty(String s) { return s == null || s.isEmpty(); }
    public static String parse(String content, String keywords, List<Rule> rules) {
        if (empty(content)) return "";
        String lower = content.toLowerCase(); // preserve the original locale behavior
        for (Rule rule : rules) {
            if (rule == null || rule.company == null || rule.keyword == null || rule.regex == null) continue;
            try {
                if (lower.contains(rule.company.toLowerCase()) && lower.contains(rule.keyword.toLowerCase())) {
                    Matcher m = compile(rule.regex).matcher(content);
                    if (m.find() && !empty(m.group())) return m.group();
                }
            } catch (PatternSyntaxException ignored) { /* A malformed rule must not poison all rules. */ }
        }
        try {
            String keyword = parseKeyword(keywords, content);
            if (empty(keyword)) return "";
            return containsChinese(content) ? getSmsCodeCN(keyword, content) : getSmsCodeEN(keyword, content);
        } catch (PatternSyntaxException | NullPointerException ignored) { return ""; }
    }
    private static boolean containsChinese(String text) {
        String regex = "[\u4e00-\u9fa5]|。";
        Pattern pattern = compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    /**
     * 解析文本内容中的验证码关键字，如果有则返回第一个匹配到的关键字，否则返回 空字符串
     */
    private static String parseKeyword(String keywordsRegex, String content) {
        Pattern pattern = compile(keywordsRegex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private static String getSmsCodeCN(String keyword, String content) {
        // 之前的正则表达式是 [a-zA-Z0-9]{4,8}
        // 现在的正则表达式是 [a-zA-Z0-9]+(\.[a-zA-Z0-9]+)? 匹配数字和字母之间最多一个.的字符串
        // 之前的不能识别和剔除小数，比如 123456.231，很容易就把 123456 作为验证码。
        String codeRegex = "(?<![a-zA-Z0-9])[a-zA-Z0-9]{4,8}(?![a-zA-Z0-9])";
        // 先去掉所有空白字符处理
        String handledContent = removeAllWhiteSpaces(content);
        String smsCode = getSmsCode(codeRegex, keyword, handledContent);
        if (empty(smsCode)) {
            // 没解析出就按照原文本再处理一遍
            smsCode = getSmsCode(codeRegex, keyword, content);
        }
        return smsCode;
    }

    /**
     * 获取英文短信包含的验证码
     */
    private static String getSmsCodeEN(String keyword, String content) {
        // 之前的正则表达式是 [0-9]{4,8} 匹配由数字组成的4到8长度的字符串
        // 现在的正则表达式是 [0-9]+(\\.[0-9]+)? 匹配数字之间最多一个.的字符串
        // 之前的不能识别和剔除小数，比如 123456.231，很容易就把 123456 作为验证码。
        String codeRegex = "(?<![0-9])[0-9]{4,8}(?![0-9])";
        String smsCode = getSmsCode(codeRegex, keyword, content);
        if (empty(smsCode)) {
            // 没解析出就去掉所有空白字符再处理
            content = removeAllWhiteSpaces(content);
            smsCode = getSmsCode(codeRegex, keyword, content);
        }
        return smsCode;
    }

    /**
     * Remove all white spaces.
     */
    private static String removeAllWhiteSpaces(String content) {
        return content.replaceAll("\\s*", "");
    }

    /**
     * Parse SMS code
     *
     * @param codeRegex SMS code regular expression
     * @param keyword   SMS code SMS keywords expression
     * @param content   SMS content
     * @return the SMS code if it's found, otherwise return empty string ""
     */
    private static String getSmsCode(String codeRegex, String keyword, String content) {
        Pattern p = compile(codeRegex);
        Matcher m = p.matcher(content);
        List<String> possibleCodes = new ArrayList<>();
        while (m.find()) {
            final String matchedStr = m.group();
            possibleCodes.add(matchedStr);
        }
        if (possibleCodes.isEmpty()) { // no possible code
            return "";
        }

        List<String> filteredCodes = new ArrayList<>();
        for (String possibleCode : possibleCodes) {
            if (isNearToKeyword(keyword, possibleCode, content)) {
                filteredCodes.add(possibleCode);
            }
        }
        if (filteredCodes.isEmpty()) { // no possible code near to keywords
            filteredCodes = possibleCodes;
        }

        int maxMatchLevel = LEVEL_NONE;
        // minimum distance of possible code to keyword
        int minDistance = content.length();
        String smsCode = "";
        for (String filteredCode : filteredCodes) {
            final int curLevel = getMatchLevel(filteredCode);
            if (curLevel > maxMatchLevel) {
                maxMatchLevel = curLevel;
                // reset the minDistance
                minDistance = distanceToKeyword(keyword, filteredCode, content);
                smsCode = filteredCode;
            } else if (curLevel == maxMatchLevel) {
                int curDistance = distanceToKeyword(keyword, filteredCode, content);
                if (curDistance < minDistance) {
                    minDistance = curDistance;
                    smsCode = filteredCode;
                }
            }
        }
        return smsCode;
    }

    /* 匹配度：6位纯数字，匹配度最高 */
    private static final int LEVEL_DIGITAL_6 = 4;
    /* 匹配度：4位纯数字，匹配度次之 */
    private static final int LEVEL_DIGITAL_4 = 3;
    /* 匹配度：纯数字, 匹配度次之 */
    private static final int LEVEL_DIGITAL_OTHERS = 2;
    /* 匹配度：数字+字母 混合, 匹配度次之 */
    private static final int LEVEL_TEXT = 1;
    /* 匹配度：纯字母, 匹配度最低 */
    private static final int LEVEL_CHARACTER = 0;
    private static final int LEVEL_NONE = -1;

    private static int getMatchLevel(String matchedStr) {
        if (matchedStr.matches("^[0-9]{6}$"))
            return LEVEL_DIGITAL_6;
        if (matchedStr.matches("^[0-9]{4}$"))
            return LEVEL_DIGITAL_4;
        if (matchedStr.matches("^[0-9]*$"))
            return LEVEL_DIGITAL_OTHERS;
        if (matchedStr.matches("^[a-zA-Z]*$"))
            return LEVEL_CHARACTER;
        return LEVEL_TEXT;
    }

    /**
     * 可能的验证码是否靠近关键字；
     * @return 可能的验证码前后30个字符内是否包含验证码关键字，如果包含则返回true；否则返回false
     */
    private static boolean isNearToKeyword(String keyword, String possibleCode, String content) {
        return distanceToKeyword(keyword, possibleCode, content) <= 30;
    }

    /**
     * 计算可能的验证码与关键字的距离
     */
    private static int distanceToKeyword(String keyword, String possibleCode, String content) {
        int keywordIdx = content.indexOf(keyword);
        int possibleCodeIdx = content.indexOf(possibleCode);
        return Math.abs(keywordIdx - possibleCodeIdx);
    }

    public static String parseCompany(String content) {
        String regex = "((?<=【)(.*?)(?=】))|((?<=\\[)(.*?)(?=\\]))";
        Pattern pattern = compile(regex);
        Matcher matcher = pattern.matcher(content);
        List<String> possibleCompanies = new ArrayList<>();
        while (matcher.find()) {
            possibleCompanies.add(matcher.group());
        }
        StringBuilder sb = new StringBuilder();
        boolean needSpace = false; // 是否需要空格分隔
        for (String company : possibleCompanies) {
            if (needSpace) {
                sb.append(' ');
            } else {
                needSpace = true;
            }
            sb.append(company);
        }
        return sb.toString();
    }
}
