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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.assertj.core.util.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Experimental
public class UrlBundle {
    // Have Fun!
    // UrlBundle.parse("https://www.my_website.com/abc/s?{1~99}&p={10~-10~-3}&t={a~z&A~Z}").forEach(System.out::println);

    public static List<String> parse(String rule) {
        return recursiveParse(rule);
    }

    private static List<String> recursiveParse(String rule) {
        if (StringHelper.isNotNull(rule)) {
            List<String> result = new ArrayList<>();
            result.add(rule);
            return parseFirstGroup(result);
        } else {
            return Arrays.asList();
        }
    }

    private static List<String> parseFirstGroup(List<String> urls) {
        int startPos, endPos;
        Iterator<String> parents = urls.iterator();
        List<String> children = Lists.newArrayList();
        while (parents.hasNext()) {
            String rule = parents.next();
            if ((startPos = rule.indexOf('{')) == -1 || (endPos = rule.indexOf('}')) == -1) {
                return urls;
            }
            if (startPos > endPos) {
                throw new RuntimeException("invalid bound, '}' matched '{': " + rule);
            }
            String firstGroup = rule.substring(startPos + 1, endPos);
            if (firstGroup.contains("&")) {
                for (String subRule : firstGroup.split("&")) {
                    children.add(String.format("%s{%s}%s", rule.substring(0, startPos), subRule, rule.substring(endPos + 1)));
                }
            } else {
                String[] params = Iterables.toArray(Splitter.on('~').trimResults().split(firstGroup), String.class);
                if (params.length < 2 || params.length > 3) {
                    throw new RuntimeException(String.format("invalid param size %d, in '%s'", params.length, firstGroup));
                }
                if (params[0].length() == 1 && params[1].length() == 1
                        && Character.isLetter(params[0].charAt(0)) && Character.isLetter(params[1].charAt(0))) {
                    String lowercase = "abcdefghijklmnopqrstuvwxyz";
                    String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                    boolean isLowercase;
                    if (lowercase.contains(params[0]) && lowercase.contains(params[1])) {
                        isLowercase = true;
                    } else if (uppercase.contains(params[0]) && uppercase.contains(params[1])) {
                        isLowercase = false;
                    } else {
                        throw new RuntimeException("letter case crashed, in group " + firstGroup);
                    }
                    int startChar = isLowercase ? lowercase.indexOf(params[0]) : uppercase.indexOf(params[0]);
                    int endChar = isLowercase ? lowercase.indexOf(params[1]) : uppercase.indexOf(params[1]);
                    boolean upward = startChar < endChar;
                    try {
                        int step = params.length == 3 ? Integer.parseInt(params[2]) : (upward ? 1 : -1);
                        if (step == 0 || step > 0 && !upward || step < 0 && upward) {
                            throw new RuntimeException("infinite iteration, please check the 'step', in group: " + firstGroup);
                        }
                        for (int i = startChar; upward ? i <= endChar : i >= endChar; i = i + step) {
                            children.add(rule.substring(0, startPos) + (isLowercase ? lowercase.charAt(i) : uppercase.charAt(i)) + rule.substring(endPos + 1));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                    }
                } else {
                    int startValue;
                    int endValue;
                    try {
                        startValue = Integer.parseInt(params[0]);
                        endValue = Integer.parseInt(params[1]);
                        boolean upward = startValue <= endValue;
                        int step = params.length == 3 ? Integer.parseInt(params[2]) : (upward ? 1 : -1);
                        if (step == 0 || step > 0 && !upward || step < 0 && upward) {
                            throw new RuntimeException("infinite iteration, please check 'step', in group: " + firstGroup);
                        }
                        for (int i = startValue; upward ? i <= endValue : i >= endValue; i = i + step) {
                            children.add(rule.substring(0, startPos) + i + rule.substring(endPos + 1));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage());
                    }
                }
            }
        }
        return parseFirstGroup(children);
    }

}
