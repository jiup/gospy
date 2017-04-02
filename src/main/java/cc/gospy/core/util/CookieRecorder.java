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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CookieRecorder {
    public static final CookieRecorder INSTANCE = new CookieRecorder();

    private Map<String, CookieGroup> cookieGroups = new HashMap<>();

    private CookieRecorder() {
    }

    public CookieRecorder newInstance() {
        return new CookieRecorder();
    }

    public void addCookieGroup(CookieGroup group) {
        assert group != null;
        cookieGroups.put(group.getDomain(), group);
    }

    public void addCookieGroup(String domain) {
        addCookieGroup(CookieGroup.newInstance(domain));
    }

    public CookieGroup getCookieGroup(String domain) {
        return this.cookieGroups.get(domain);
    }

    public String getCookieString(String domain) {
        CookieGroup group = getCookieGroup(domain);
        return group == null ? null : group.getCookieString();
    }

    public void removeCookieGroup(String domain) {
        CookieGroup group = cookieGroups.remove(domain);
        if (group != null) {
            Map cookies = group.getCookies();
            if (cookies != null) {
                cookies.clear();
            }
        }
    }

    public void clear() {
        this.cookieGroups.forEach((s, group) -> group.getCookies().clear());
        this.cookieGroups.clear();
    }

    public static class CookieGroup {
        String domain;
        Map<String, String> cookies;

        private CookieGroup(String domain) {
            this.domain = domain;
            this.cookies = new LinkedHashMap<>();
        }

        public static CookieGroup newInstance(String domain) {
            return new CookieGroup(domain);
        }

        public String getDomain() {
            return domain;
        }

        public void putCookie(String key, String value) {
            this.getCookies().put(key.trim(), value.trim());
        }

        public void removeCookie(String key) {
            this.getCookies().remove(key);
        }

        public Map<String, String> getCookies() {
            return cookies;
        }

        public String getCookieString() {
            StringBuilder buffer = new StringBuilder();
            cookies.forEach((key, value) -> buffer.append(key).append("=").append(value).append("; "));
            return buffer.length() > 2 ? buffer.substring(0, buffer.length() - 2) : buffer.toString();
        }

        @Override
        public String toString() {
            return "CookieGroup{" +
                    "domain='" + domain + '\'' +
                    ", cookies=" + cookies +
                    '}';
        }
    }
}
