自定义SpringMVC

1.SpringMVC执行流程

1.1 执行流程图

 

1.2 执行过程

1. 前端发送Http请求到DispatcherServlet；
2. DispatcherServlet收到请求调用HandlerMapping处理映射器，处理映射器根据请求的url找到具体的处理器，生成处理器对象以及处理器拦截器（如果有则生成），一并返回给DispatcherServlet；
3. DispatcherServlet通过HandlerAdapter处理器适配器调用处理器；
4. 执行处理器（Controlle，也叫后端控制器）,返回ModelAndView（数据模型和视图名称）;
5. HandlerAdapter将ModelAndView传给DispatcherServlet；
6. DispatcherServlet将ModelAndView传给ViewResolver视图解析器，解析后返回具体的View;
7. DispatcherServlet对View进行渲染，将数据模型填充到View中；
8. DispatcherServlet响应用户；

2.设计思路

1. 读取配置文件
   SpringMVC本质上是一个Servlet，为了读取web.xml配置，这里用到了ServletConfig 这个类，它代表当前Servlet在web.xml中的配置信息，通过  config.getInitParameter("contextConfigLocation");//读取启动参数，读取application.properties。
2. 初始化阶段
   
3. 运行阶段
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
   

3.代码实现

	https://github.com/gnng/MySpringMVC

4.自我总结

1. Maven的依赖范围<scope>provided</scope>
    
2. Java元注解
   
