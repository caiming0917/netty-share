package org.caijuan.tomcat;

import java.util.ArrayList;
import java.util.List;

public class ServletMappingConfig {
    public static List<ServletMapping> servletMappingList = new ArrayList<>();

    //制定哪个URL交给哪个servlet来处理
    static {
        servletMappingList.add(new ServletMapping("findGirl", "/girl", "org.caijuan.tomcat.FindGirlServlet"));
        servletMappingList.add(new ServletMapping("helloWorld", "/world", "org.caijuan.tomcat.HelloWorldServlet"));
    }
}
