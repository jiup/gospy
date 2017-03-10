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

package cc.gospy.core.processor.impl;

import cc.gospy.core.Page;
import cc.gospy.core.Task;
import cc.gospy.core.processor.Processor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JSoupProcessor implements Processor {
    private String defaultCharacterEncoding = "UTF-8";

    public JSoupProcessor() {
        init();
    }

    private void init() {

    }

    private String getCharacterEncoding(Page page) {
        for (String kv : page.getContentType().split(";")) {
            if (kv.trim().startsWith("charset=")) {
                return kv.trim().substring(8);
            }
        }
        return null;
    }

    private Document parse(Page page) throws UnsupportedEncodingException {
        String charsetName = getCharacterEncoding(page);
        String html;
        try {
            html = page.getContent().toString(charsetName != null ? charsetName : defaultCharacterEncoding);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unknown charset " + charsetName + ", using default (" + defaultCharacterEncoding + ")");
            html = page.getContent().toString("UTF-8");
        }
        return Jsoup.parse(html);
    }

    @Override
    public void exceptionCaught(Throwable throwable, Task task, Page page) {
        throwable.printStackTrace();
    }

    @Override
    public List<Task> process(Task task, Page page) {
        //        page.getExtra().forEach((k, v) -> System.out.println(k + " -> " + v));
        List<Task> newTasks = new ArrayList<>();
        Document document = null;
        try {
            document = parse(page);
        } catch (UnsupportedEncodingException e) {
            exceptionCaught(e, task, page);
        }

//        System.out.println(document);
        Elements elements = document.select("useragent");
        System.out.println(elements.size());
        Iterator<Element> iterator = elements.iterator();
        while (iterator.hasNext()) {
            Element element = iterator.next();
            if (element.hasAttr("useragent") && !element.attr("useragent").equals("")) {
//                System.out.println(element.attr("description"));
                System.out.println("public static final String "
                        + element.attr("description").trim().replaceAll("-", "_").trim().replaceAll("( +)|\\(", "_").replaceAll("_+", "_").replaceAll("\\)","")
                        .replaceAll("ID:_", "").replaceAll("/", "_").replaceAll("\\.", "_")
                        + " = \"" + element.attr("useragent").trim() + "\";");
            }
        }

        return newTasks;
    }
}
