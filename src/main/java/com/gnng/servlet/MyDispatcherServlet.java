package com.gnng.servlet;

import com.gnng.annotation.MyController;
import com.gnng.annotation.MyRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

public class MyDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<String>();

    private Map<String,Object> ioc = new HashMap();

    private Map<String,Method> handlerMapping = new HashMap<String, Method>();

    private Map<String,Object> controllerMap = new HashMap<String, Object>();

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.初始化相关联的类，扫描用户设定包下的所有类
        doScanner(properties.getProperty("scanPackage"));
        //3.拿到扫描到的类，通过反射实例化，并放入IOC容器中,（k-v,beanName-bean）,beanName默认首字母小写
        doInstance();
        //4.初始化HandlerMapping(将url和method对应上)
        iniHandlerMapping();
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500！Server Exception");
        }
    }

    @Override


    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * 读取web.xml中配置的初始化参数init-param的value
     * @param location
     */
    private void doLoadConfig(String location) {
        //把web.xml中contextConfigLocation对应的value值加载到流里面
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            //用Properties文件加载文件里面的内容
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //关闭流
            if(resourceAsStream != null){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 扫描的所用户设定包下有类
     * @param packageName
     */
    private void doScanner(String packageName) {

        //把所有的.替换成/
        URL url = this.getClass().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if(file.isDirectory()){
                //递归读取包
                doScanner(packageName+"."+file.getName());
            }else {
                //去掉.class后缀存入集合中
                String className = packageName+"."+file.getName().replace(".class","");
                classNames.add(className);
            }
        }
    }

    /**
     * 拿到扫描到的类，通过反射实例化，并放入IOC容器中,（k-v,beanName-bean）,beanName默认首字母小写
     */
    private void doInstance() {
        if(classNames.isEmpty()){
            return;
        }
        for (String className : classNames) {
            //通过反射进行实例化（只有加@controller的需要实例化）
            try {
                Class<?> clazz = Class.forName(className);
                //判断是否有myController注解
                if(clazz.isAnnotationPresent(MyController.class)){
                    ioc.put(toLowFirstWord(clazz.getSimpleName()),clazz.newInstance());
                }else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

        }
    }


    /**
     * 把字符串首字母小写
     * @param name
     * @return
     */
    private String toLowFirstWord(String name){
        char[] chars = name.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }


    /**
     * 初始化HandlerMapping(将url和method对应上)
     */
    private void iniHandlerMapping(){
        if(ioc.isEmpty()){
            return;
        }
        try {
            for (Entry<String, Object> entry : ioc.entrySet()) {
                Class<?> clazz = entry.getValue().getClass();
                if (!clazz.isAnnotationPresent(MyController.class)) {
                    continue;
                }

                //拼url时，是controller头url拼上方法的url
                String baseUrl = "";
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();
                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    handlerMapping.put(url, method);
                    controllerMap.put(url, clazz.newInstance());
                    System.out.println(url+","+method);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 处理请求
     * @param req
     * @param resp
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if(handlerMapping.isEmpty()){
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");

        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 No Found");
            return;
        }
        Method method = this.handlerMapping.get(url);

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        //保存参数值
        Object [] paramValues = new Object[parameterTypes.length];

        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称，做某些处理
            String requestParam = parameterTypes[i].getSimpleName();
            if("HttpServletRequest".equals(requestParam)){
                //参数类型已明确，强转类型是
                paramValues[i] = req;
                continue;
            }
            if("HttpServletResponse".equals(requestParam)){
                paramValues[i] = resp;
                continue;
            }
            if("String".equals(requestParam)){
                for (Entry<String, String[]> param : parameterMap.entrySet()) {
                    String valuse = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                    paramValues[i] = valuse;
                }
            }
        }

        //利用反射机制调用
        try {
            method.invoke(this.controllerMap.get(url),paramValues);//第一个参数为method所对应的实例，在IOC容器中
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
