/*
 * Copyright 2017 ZhangJiupeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.gospy.core.util;

public class StringHelper {
    public static String toAbsoluteUrl(final String protocol, final String host, final String parentUrl, final String anyUrl) {
        String res, path = anyUrl.trim();
        if (path.matches("^https?://.*")) {
            res = path;
        } else if (path.startsWith("//")) {
            res = protocol.concat(":").concat(path);
        } else if (path.startsWith("/")) {
            res = protocol.concat("://").concat(host).concat(path);
        } else {
            res = parentUrl != null ? getNavigateTargetUrl(parentUrl, path) : protocol.concat("://").concat(host).concat("/".concat(path));
        }
        res = res.indexOf('#') != -1 ? res.substring(0, res.indexOf('#')) : res; // remove in-page jump
        res = res.endsWith("/") ? res.substring(0, res.length() - 1) : res; // avoid duplicate links
        return res;
    }

    public static String getNavigateTargetUrl(final String parentUrl, final String relativeUrl) {
        String prefix = parentUrl.replaceAll("://", "").indexOf("/") != -1 ? parentUrl.substring(0, parentUrl.lastIndexOf('/')) : parentUrl.concat("/");
        String suffix = relativeUrl.startsWith("./") ? relativeUrl.substring(2) : relativeUrl;
        while (suffix.startsWith("../")) {
            suffix = suffix.substring(3);
            prefix = prefix.replaceAll("://", "").indexOf("/") != -1 ? prefix.substring(0, prefix.lastIndexOf('/')) : prefix;
        }
        return prefix.concat("/").concat(suffix);
    }

    public static String toRelativeUrl(final String anyUrl) {
        String res, path = anyUrl.trim();
        if (path.matches("^https?://.*")) {
            res = path.substring(path.indexOf("://") + 3, path.length());
        } else if (path.startsWith("//")) {
            res = path.substring(1, path.length());
        } else if (!path.startsWith("/")) {
            res = "/".concat(path);
        } else {
            res = path;
        }
        res = res.indexOf('#') != -1 ? res.substring(0, res.indexOf('#')) : res; // remove local jump
        res = res.endsWith("/") ? res.substring(0, res.length() - 1) : res; // avoid duplicate links
        return res;
    }

    public static String toEscapedFileName(String unescapedFileName) {
        return toEscapedFileName(unescapedFileName, "_");
    }

    public static String toEscapedFileName(String unescapedFileName, String escapeStr) {
        return unescapedFileName.trim().replaceAll(" +|://+|/+|\\\\+|\\*+|:+|\"+|\\?+|<+|>+|\\|+", escapeStr);
    }

    public static void main(String[] args) throws Throwable {
        // CircularRedirectException
        // https://www.cnblogs.com/wanghaomiao/p/4899355.html
        // https://github.com/zhegexiaohuozi/JsoupXpath
        System.out.println(StringHelper.toAbsoluteUrl("http", "www.baidu.com", "http://www.baidu.com/1/2/3/4/test.jsp", "../../../../4/3/2/1/test/"));
    }
}
