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
    public static String toAbsoluteUrl(final String protocol, final String host, final String anyUrl) {
        String res, path = anyUrl.trim();
        if (path.matches("^https?://.*")) {
            res = path;
        } else if (path.startsWith("//")) {
            res = "http:".concat(path);
        } else if (path.startsWith("/")) {
            res = protocol.concat("://").concat(host).concat(path);
        } else {
            res = protocol.concat("://").concat(host).concat("/").concat(path);
        }
        res = res.indexOf('#') != -1 ? res.substring(0, res.indexOf('#')) : res; // remove local jump
        res = res.endsWith("/") ? res.substring(0, res.length() - 1) : res; // avoid duplicate links
        return res;
    }

    public static String toEscapedFileName(String unescapedFileName) {
        return unescapedFileName.trim().replaceAll(" +|://+|/+|\\\\+|\\*+|:+|\"+|\\?+|<+|>+|\\|+", "_");
    }
}