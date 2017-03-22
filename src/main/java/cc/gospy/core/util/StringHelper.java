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

import cc.gospy.core.entity.Task;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class StringHelper {
    private static final Logger logger = LoggerFactory.getLogger(StringHelper.class);
    public static String myExternalIp;

    static {
        try {
            URL ipEcho = new URL("http://ipecho.net/plain");
            BufferedReader in = new BufferedReader(new InputStreamReader(ipEcho.openStream()));
            myExternalIp = in.readLine();
            in.close();
        } catch (IOException e) {
            myExternalIp = "unknown ip";
            logger.error("Get external ip failure, cause: {}", e.getMessage());
        }
    }

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

    public static String toAbsoluteUrl(final Task parentTask, final String targetUrl) {
        if (targetUrl == null || targetUrl.equals("")) {
            return parentTask.getUrl();
        }
        return toAbsoluteUrl(parentTask.getProtocol(), parentTask.getHost(), parentTask.getUrl(), targetUrl);
    }

    public static String getNavigateTargetUrl(final String parentUrl, final String relativeUrl) {
        String prefix = parentUrl.replaceAll("://", "").contains("/") ? parentUrl.substring(0, parentUrl.lastIndexOf('/')) : parentUrl.concat("/");
        String suffix = relativeUrl.startsWith("./") ? relativeUrl.substring(2) : relativeUrl;
        while (suffix.startsWith("../")) {
            suffix = suffix.substring(3);
            prefix = prefix.replaceAll("://", "").contains("/") ? prefix.substring(0, prefix.lastIndexOf('/')) : prefix;
        }
        return prefix.concat(suffix.startsWith("/") ? suffix : "/" + suffix);
    }

    @Experimental
    public static String toRelativeUrl(final String protocol, final String host, final String parentUrl, final String targetUrl) {
        if (parentUrl.equals(targetUrl) || parentUrl.equals(targetUrl.concat("/"))) {
            return parentUrl;
        }
        String pattern = protocol.concat("://").concat(host).concat("/"), target = targetUrl;
        String parent = parentUrl.length() == pattern.length() - 1 ? parentUrl.concat("/") : parentUrl;
        String parentDir = parent.substring(pattern.length() - 1, parent.lastIndexOf('/'));
        StringBuilder relativeUrl = new StringBuilder();
        if (target.matches("http://.*|https://.*|//.*|/.*")) {
            target = target.startsWith("//") ? "http:".concat(target) : target;
            target = target.startsWith("/") ? pattern.concat(target.substring(1)) : target;
            if (!target.startsWith(pattern)) {
                return null; // target url crosses domain
            }
        }
        String res = target.substring(target.lastIndexOf('/') + 1);
        String targetDir = target.substring(pattern.length() - 1, target.lastIndexOf('/'));
        while (true) {
            if (0 == Math.min(parentDir.length(), targetDir.length()) || parentDir.charAt(0) != targetDir.charAt(0)) {
                break;
            }
            if (parentDir.charAt(0) == '/' && targetDir.charAt(0) == '/') {
                parentDir = parentDir.substring(1);
                targetDir = targetDir.substring(1);
                continue;
            }
            if (parentDir.indexOf('/') != -1 && targetDir.indexOf('/') != -1) {
                String s = parentDir.substring(0, parentDir.indexOf('/'));
                if (s.equals(targetDir.substring(0, targetDir.indexOf('/')))) {
                    parentDir = parentDir.substring(s.length());
                    targetDir = targetDir.substring(s.length());
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        int depth = parentDir.length() != 0 ? parentDir.split("/").length : 0;
        depth = targetDir.length() != 0 ? depth : depth - 1;
        while (depth-- > 0) {
            relativeUrl.append("../");
        }
        relativeUrl.append(targetDir.startsWith("/") ? targetDir.substring(1) : targetDir);
        return (parentDir.length() == 0 ? "./" : "")
                .concat(relativeUrl.toString())
                .concat(targetDir.length() == 0 ? "" : "/")
                .concat(res);
    }

    public static boolean isAbsoluteUrl(String url) {
        return url.matches("http://.*|https://.*|//.*");
    }

    public static String toEscapedFileName(String unescapedFileName) {
        return toEscapedFileName(unescapedFileName, "_");
    }

    public static String cutOffProtocolAndHost(final String absoluteUrl) {
        String absUrl = absoluteUrl;
        int tmp;
        if ((tmp = absUrl.indexOf("://")) != -1) {
            absUrl = absUrl.substring(tmp + 3);
        }
        if ((tmp = absUrl.indexOf('/')) != -1) {
            absUrl = absUrl.substring(tmp + 1);
        }
        return absUrl;
    }

    public static String getRandomSerial() {
        return RandomStringUtils.randomAlphanumeric(6).toLowerCase();
    }

    public static String getRandomIdentifier() {
        return getRandomSerial().concat(" @ ") + myExternalIp;
    }

    @Experimental
    public static String toEscapedFileName(String unescapedFileName, String escapeStr) {
        if (unescapedFileName == null || unescapedFileName.equals("")) {
            return "null";
        }
        return unescapedFileName.trim().replaceAll(" +|://+|/+|\\\\+|\\*+|:+|\"+|\\?+|<+|>+|\\|+", escapeStr);
    }

    public static String toString(boolean[] value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("boolean[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 32 == 0 ? "\n" : "").append(value[i] ? "T" : "F").append(" ");
        }
        return buffer.append("\n").toString();
    }

    public static String toString(byte[] value) {
        StringBuilder buffer = new StringBuilder();
        char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        buffer.append("byte[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 32 == 0 ? String.format("\n%06d:\t", i) : "")
                    .append(hex[(value[i] >>> 4) & 0x0F]).append(hex[(value[i]) & 0x0F]).append(" ");
        }
        return buffer.append("\n").toString();
    }

    public static String toString(short[] value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("short[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 16 == 0 ? "\n" : "").append(value[i]).append(" ");
        }
        return buffer.append("\n").toString();
    }

    public static String toString(int[] value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("int[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 16 == 0 ? "\n" : "").append(value[i]).append(" ");
        }
        return buffer.append("\n").toString();
    }

    public static String toString(float[] value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("float[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 16 == 0 ? "\n" : "").append(value[i]).append(" ");
        }
        return buffer.append("\n").toString();
    }

    public static String toString(double[] value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("double[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 8 == 0 ? "\n" : "").append(value[i]).append(" ");
        }
        return buffer.append("\n").toString();
    }

    public static String toString(long[] value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("long[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 8 == 0 ? "\n" : "").append(value[i]).append(" ");
        }
        return buffer.append("\n").toString();
    }

    public static String toString(char[] value) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("char[").append(value.length).append("]");
        for (int i = 0; i < value.length; i++) {
            buffer.append(i % 64 == 0 ? "\n" : "").append(value[i]);
        }
        return buffer.append("\n").toString();
    }

}
