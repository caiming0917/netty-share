package org.caijuan.tomcat;

import java.io.IOException;


public class FindGirlServlet extends MyServlet {
    @Override
    public void doGet(MyRequest myRequest, MyResponse myResponse) {
        try {
            myResponse.write("get gril....");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doPost(MyRequest myRequest, MyResponse myResponse) {
        try {
            myResponse.write("post girl...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
