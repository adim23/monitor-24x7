<a href='http://ombprojects.com/24x7monitoring.png'><img src='http://ombprojects.com/24x7monitoring.png' width='500' /></a>


**Description**

24x7Monitoring is an Open Source Application Performance Monitoring (APM) Framework for J2EE Applications that uses Aspect Oriented Programming to collect Performance metrics about the running JVM and display the data to the user in a tabular/graphical format.

24x7Monitoring does not require any modification to the source code of running application. you just need to add the monitoring agent to your application server classpath and aspectJ Load Time Weaving (LTW) takes care of weaving into your code at runtime and collect the performance metrics.

**Features**

The Monitoring Agent Collects the following Metrics about the running applications.
  * **Method execution time**: classes/packages to be monitored are configurable using regular-expression like syntax
  * **SQL queries response time**: Support all JDBC Drivers implementing the interfaces from the javax.sql.`*` package.
  * **Business Transactions**: monitoring incoming HTTP Requests and collect the response time of the requested pages (i.e Catalogue Page, Login Page.....)
  * **DB Connection Pool**: Keeps track of the number of open DB connections at any point of time and display the data to the user in a graphical format.
  * **User Session Monitoring**: Keeps track of the Active HttpSessions and display the collected data in graphical format.
  * **Live Threads**: Keeps track of the Active Threads using the Java Management API.
  * **Heap/Memory Usage**: Monitors the Memory Heap and collect the memory usage on a fixed interval using the Java Management API.
  * **CPU Usage**: Monitor and Collect the CPU Utilization % using Sigar API.
  * **Exception logging**: Uses AspectJ to collect any exception thrown in the application and store it for future analysis. could replace the log monitoring for exception logging.

In Addition to the Metrics mentioned before, the application shall allow the user to **configure alerts/notifications** and **scheduled reports** on specific metrics to have the tool sends an alert to the user when the metrics number reaches a pre-defined and configurable threshold. For example: I want the tool to notify me by email when the CPU usage goes beyond 50%.

**Overhead**

Load testing was performed on a sample eCommerce application to measure the overhead of the monitoring framework. For this test, I have enabled ALL methods tracing from the application package. this means that it tracks all methods executed in the application ( including getters and setters). the result was an average overhead of ~3% and the higher the response time of a specific page the lower the overhead will be.

<a href='http://ombprojects.com/overhead.jpg'><img src='http://ombprojects.com/overhead.jpg' width='500' /></a>


**Demo**

http://ombprojects.com/24x7monitoring/

Select "Custom Range" from the drop-down and enter:
from: 12/01/2013 04:25 pm
to: 12/01/2013 05:00 pm
Then select a Node from the Tree.

**Installation Steps**

**1-** download [monitoring.zip](https://drive.google.com/#folders/0B44i2qOIOmVpS1BHVVVJUWZ6LUk) (shared on google drive) and unzip into in your local drive.

**2-** Add the following VM Arguments to your application server start-up script. Note the ':' after javaagent ( not '=' )
> -javaagent:" YOUR\_EXTRACTED\_ZIP\_DIR \aspectjweaver.jar"
-Dmonitoring.configLocation="YOUR\_EXTRACTED\_ZIP\_DIR\config.properties"
for Example: in Tomcat you can modify your TOMCAT\_HOME\bin\catalina.bat or sh and add them to CATALINA\_OPTS as
set CATALINA\_OPTS=-Dmonitoring.configLocation="C:\monitor\config.properties" -javaagent:"C:\ monitor\aspectjweaver.jar"
> > For other application servers, please consult their official documentation on how to add VM argument to their start-up scripts.

**3-** add YOUR\_EXTRACTED\_ZIP\_DIR\sigar-lib folder to the your PATH variable.

**4-** Add monitoringCollector.jar to your application server's classpath. for example, in tomcat, you can add the jar file to your classpath by copying it in your TOMCAT\_HOME\lib folder or by editing the CLASSPATH variable in TOMCAT\_HOME\bin\setclasspath.bat/sh. for other application servers, please consult their official documentation on how to add a jar/properties file in your application server classpath.

**5-** Modify config.properties and provide the following parameters based on your environment settings:

com.ombillah.monitoring.aspectj.MethodExecutionTimeAspect : include the list of classes/packages that you want to monitor. you can include multiple classes/packages by  separating them with a comma.

com.ombillah.monitoring.aspectj.ExceptionHandlerAspect=: include the list of classes/packages that you want to monitorp for exception logging. you can include multiple classes/packages by  separating them with a comma.


> Here is a list of examples of how to use the expression language:
> com.aspects.blog.package.**: This will match all the methods in all classes of com.aspects.blog.package.
> com.aspects.blog.package..** : This will match all the methods in all classes of com.aspects.blog.package and its sub packages. The only difference is the extra dot(.) after package.
> com.aspects.blog.package.DemoClass : This will match all the methods in the DemoClass.
> DemoInterface+ : This will match all the methods which are in classes which implement DemoInterface.

SMTP\_HOST=the host name of your SMTP mail server used to send alerts and repors.

SMTP\_PORT=The Port of your SMTP server.

SMTP\_USERNAME=Your username

SMTP\_PASSWORD=Your password.

ALERT\_JOB\_FREQUENCY\_IN\_MS= the frequency of the Alert Job. Default to 1 minute.

DEPLOYED\_CLIENT\_CONTEXT\_URL=http://localhost:8080/monitoring


**6-** Deploy monitoring.war into your application server.

**7-** Access your collected performance metrics by accessing htp://host:port/monitoring where host and port are the host and port of your application server.