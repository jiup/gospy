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

package cc.gospy.util;

public class StringHelper {
    public static String toAbsoluteUrl(String protocol, String host, String anyUrl) {
        String res;
        if (anyUrl.matches("^https?://.*")) {
            res = anyUrl;
        } else if (anyUrl.startsWith("//")) {
            res = "http:".concat(anyUrl);
        } else if (anyUrl.startsWith("/")) {
            res = protocol.concat("://").concat(host).concat(anyUrl);
        } else {
            res = protocol.concat("://").concat(host).concat("/").concat(anyUrl);
        }
        res = res.indexOf('#') != -1 ? res.substring(0, res.indexOf('#')) : res; // remove local jump
        res = res.endsWith("/") ? res.substring(0, res.length() - 1) : res; // avoid duplicate links
        return res;
    }
}
