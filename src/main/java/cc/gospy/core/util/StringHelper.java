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
        return prefix.concat(suffix.startsWith("/") ? suffix : "/" + suffix);
    }

    @Experimental
    public static String toRelativeUrl(final String protocol, final String host, final String parentUrl, final String targetUrl) {
        String pattern = protocol.concat("://").concat(host).concat("/"), target = targetUrl;
        String parent = parentUrl.length() == pattern.length() - 1 ? parentUrl.concat("/") : parentUrl;
        String parentDir = parent.lastIndexOf('/') == -1 ? "/" : parent.substring(pattern.length() - 1, parent.lastIndexOf('/'));
        StringBuffer relativeUrl = new StringBuffer();
        if (target.matches("http://.*|https://.*|//.*|/.*")) {
            target = target.startsWith("//") ? "http:".concat(target) : target;
            target = target.startsWith("/") ? pattern.concat(target.substring(1)) : target;
            if (!target.startsWith(pattern)) {
                return null; // target url crosses domain
            }
        } else {
            target = target.length() == pattern.length() - 1 ? target.concat("/") : target;
        }
        String res = target.substring(target.lastIndexOf('/') + 1);
        String targetDir = target.substring(pattern.length() - 1, target.lastIndexOf('/'));
        while (true) {
            if (0 == Math.min(parentDir.length(), targetDir.length()) || parentDir.charAt(0) != targetDir.charAt(0)) {
                break;
            }
            parentDir = parentDir.substring(1);
            targetDir = targetDir.substring(1);
        }
        int depth = parentDir.length() != 0 ? parentDir.split("/").length : 0;
        depth = targetDir.length() != 0 ? depth : depth - 1;
        while (depth-- > 0) {
            relativeUrl.append("../");
        }
        relativeUrl.append(targetDir);
        return (parentDir.length() != 0 ? "" : ".").concat(relativeUrl.toString()).concat(targetDir.length() == 0 ? "" : "/").concat(res);
    }

    public static boolean isAbsoluteUrl(String url) {
        return url.matches("http://.*|https://.*|//.*");
    }

    public static String toEscapedFileName(String unescapedFileName) {
        return toEscapedFileName(unescapedFileName, "_");
    }

    public static String toEscapedFileName(String unescapedFileName, String escapeStr) {
        return unescapedFileName.trim().replaceAll(" +|://+|/+|\\\\+|\\*+|:+|\"+|\\?+|<+|>+|\\|+", escapeStr);
    }
}
