# MySpringMVC# 自定义SpringMVC

## 1.SpringMVC执行流程

	1.1 执行流程图
![091846_FTTR_3577599.png](https://static.oschina.net/uploads/space/2018/0222/091846_FTTR_3577599.png) 

    1.2 执行过程

1. 前端发送Http请求到DispatcherServlet；
2. DispatcherServlet收到请求调用HandlerMapping处理映射器，处理映射器根据请求的url找到具体的处理器，生成处理器对象以及处理器拦截器（如果有则生成），一并返回给DispatcherServlet；
3. DispatcherServlet通过HandlerAdapter处理器适配器调用处理器；
4. 执行处理器（Controlle，也叫后端控制器）,返回ModelAndView（数据模型和视图名称）;
5. HandlerAdapter将ModelAndView传给DispatcherServlet；
6. DispatcherServlet将ModelAndView传给ViewResolver视图解析器，解析后返回具体的View;
7. DispatcherServlet对View进行渲染，将数据模型填充到View中；
8. DispatcherServlet响应用户；

## 2.设计思路

1. 读取配置文件

   SpringMVC本质上是一个Servlet，为了读取web.xml配置，这里用到了ServletConfig 这个类，它代表当前Servlet在web.xml中的配置信息，通过 [^ config.getInitParameter("contextConfigLocation");//读取启动参数]，读取application.properties。

   ```xml
           <init-param>
               <param-name>contextConfigLocation</param-name>
               <param-value>application.properties</param-value>
           </init-param>
   ```

   

2. 初始化阶段

   ```java
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
   ```

   

3. 运行阶段

    ``` java
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
    ```

   

## 3.代码实现

​	https://github.com/gnng/MySpringMVC

## 4.自我总结

 1. Maven的依赖范围<scope>provided</scope>

    ```xml
        <dependencies>
            <dependency>
                <groupId>javax.servlet</groupId>
                <artifactId>javax.servlet-api</artifactId>
                <version>3.1.0</version>
                <!--    <scope>xxx</scope>依赖范围
                    * compile，缺省值，适用于所有阶段，会随着项目一起发布。
                    * provided，类似compile，期望JDK、容器或使用者会提供这个依赖。如servlet.jar。
                    * runtime，只在运行时使用，如JDBC驱动，适用运行和测试阶段。
                    * test，只在测试时使用，用于编译和运行测试代码。不会随项目发布。
                    * system，类似provided，需要显式提供包含依赖的jar，Maven不会在Repository中查找它。
                -->
                <scope>provided</scope>
            </dependency>
        </dependencies>
    ```

 2. Java元注解

    ```java
    /**
     * java中元注解有四个： @Retention @Target @Document @Inherited；
     * 　　@Retention：注解的保留位置
     * 　　　　@Retention(RetentionPolicy.SOURCE)   //注解仅存在于源码中，在class字节码文件中不包含
     * 　　　　@Retention(RetentionPolicy.CLASS)     // 默认的保留策略，注解会在class字节码文件中存在，但运行时无法获得，
     * 　　　　@Retention(RetentionPolicy.RUNTIME)  // 注解会在class字节码文件中存在，在运行时可以通过反射获取到
     *
     * 　　@Target:注解的作用目标
     * 　　　　@Target(ElementType.TYPE)   //接口、类、枚举、注解
     * 　　　　@Target(ElementType.FIELD) //字段、枚举的常量
     * 　　　　@Target(ElementType.METHOD) //方法
     * 　　　　@Target(ElementType.PARAMETER) //方法参数
     * 　　　　@Target(ElementType.CONSTRUCTOR)  //构造函数
     * 　　　　@Target(ElementType.LOCAL_VARIABLE)//局部变量
     * 　　　　@Target(ElementType.ANNOTATION_TYPE)//注解
     * 　　　　@Target(ElementType.PACKAGE) ///包   
     *  
     *     @Document：说明该注解将被包含在javadoc中
     *  
     * 　  @Inherited：说明子类可以继承父类中的该注解
     */
    ```

    
