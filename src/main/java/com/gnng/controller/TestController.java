package com.gnng.controller;


import com.gnng.annotation.MyController;
import com.gnng.annotation.MyRequestMapping;
import com.gnng.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("/test")
public class TestController {

    @MyRequestMapping("/doTest")
    public void test1(HttpServletRequest request, HttpServletResponse response, @MyRequestParam("param") String param){
        System.out.println(param);
        try {
            response.getWriter().write("do test1 method success,"+param);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @MyRequestMapping("/doTest2")
    public void test2(HttpServletRequest request, HttpServletResponse response){
        try {
            response.getWriter().write("do test2 method success");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
