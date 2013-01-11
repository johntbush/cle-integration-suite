CLE Integration Test Suite
==========================

This is an integration suite/framework for Sakai.  It leverages the Arquillian framework from jboss (http://arquillian.org/).

Unit testing in Sakai can be hard.  You end up either creating a lot of mocks or struggling with the component manager to either get pieces or the full content loaded.  In my experience, a lot of bugs are environment specific.  Folks use all sorts of different providers, extensions, and various external systems.  Configuration, data, and network issues can break things.  A lot of things aren't discovered until production because testing the real environment is difficult, and a lot of time facilitating the test only through the UI is either difficult or impossible to test every flow.

This framework aims to solve this problem.  What is does is let you junit (or testng) to write tests in the normal way.  You have full access to the real Sakai services.  You then package up any dependencies needed for your test using ShrinkWrap api.  The framework then packages up a war file with your test and dependencies and uses Tomcat's manager app and jmx to deploy and run your tests.  It then undeploys your test.war when its finished.  

Project Dependencies
--------------------
* You need a running Sakai instance
* Maven - you'll need maven 3 to build the container code

Arquillian requires a container to run your tests in.  This suite is designed to work with a tomcat 7 remote container because that is what Sakai runs in.  Unfortunately there is no standard tomcat 7 remote container in the normal location here (https://github.com/arquillian/arquillian-container-tomcat).  So I have written one which hopefully jboss will pick up.  So you will need to get my fork in the meantime and build it.

```
git clone git@github.com:johntbush/arquillian-container-tomcat.git
cd arquillian-container-tomcat
mvn3 install
```

Ok you are done with that.  Now that you have the tomcat 7 remote container installed locally you can move on to setting up the project to run.

Tomcat Configuration
--------------------

In order for this to work you need to make sure tomcat is running the manager app, and jmx is enabled.  You also need to adjust the tomcat users.

* Turn on jmx in tomcat in setenv.sh or whatever tomcat env file you use
```
export JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8089 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=localhost";
```
* Adjust tomcat.home/conf/tomcat/tomat-users.xml.  You need a user with the 4 manager roles as described below
```
<tomcat-users>
  <user username="admin" password="admin" roles="manager-gui,manager-script,manager-jmx,manager-status"/>
  <role rolename="admin"/>
  <role rolename="manager-gui"/>
  <role rolename="manager-script"/>
  <role rolename="manager-jmx"/>
  <role rolename="manager-status"/>
</tomcat-users>
```
* Make sure you have the manager app in your tomcat.home/webapps folder.  It should be there by default if you haven't removed it

Test Suite Configuration
------------------------
Adjust the src/test/resources/arquillian.xml file to make your tomcat environment

```
<?xml version="1.0" encoding="UTF-8"?>
<arquillian xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns="http://jboss.org/schema/arquillian"
    xsi:schemaLocation="http://jboss.org/schema/arquillian http://jboss.org/schema/arquillian/arquillian_1_0.xsd">
      <engine>
        <property name="deploymentExportPath">target/</property>
    </engine>

    <container qualifier="tomcat-remote-7" default="true">
        <configuration>
            <property name="jmxPort">8089</property>
            <property name="host">localhost</property>
            <property name="hostPort">8080</property>
            <property name="user">admin</property>
            <property name="pass">admin</property>
        </configuration>
    </container>
</arquillian>
```

Running
-------
Make sure your tomcat server is running. Then invoke junit with maven or run inside your IDE in the normal way you run unit tests
```
mvn test
```
You should see in tomcat output like this, as the suite deploys, run, and undeploys the test app
```
Jan 11, 2013 8:59:19 AM org.apache.catalina.startup.HostConfig deployWAR
INFO: Deploying web application archive /Users/jbush/Dev/tools/apache-tomcat-7.0.27/webapps/test.war
Jan 11, 2013 8:59:22 AM org.apache.catalina.startup.HostConfig checkResources
INFO: Undeploying context [/test]
Jan 11, 2013 8:59:23 AM org.apache.catalina.startup.HostConfig deployWAR
INFO: Deploying web application archive /Users/jbush/Dev/tools/apache-tomcat-7.0.27/webapps/test.war
Jan 11, 2013 8:59:25 AM org.apache.catalina.startup.HostConfig checkResources
INFO: Undeploying context [/test]
```

Then the on the client side where you ran your tests, you see the normal surefire type of report like this.
```
------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.rsmart.sakai.ServerConfigurationServiceTest
Jan 11, 2013 9:57:26 AM org.jboss.arquillian.container.impl.MapObject populate
WARNING: Configuration contain properties not supported by the backing object org.jboss.arquillian.container.tomcat.remote_7.TomcatRemoteConfiguration
Unused property entries: {hostPort=8080}
Supported property names: [host, bindHttpPort, managerUrl, unpackArchive, httpPort, jmxVirtualHost, jmxUri, pass, jmxPort, bindAddress, urlCharset, user, appBase]
Jan 11, 2013 9:57:29 AM org.jboss.arquillian.container.tomcat.ProtocolMetadataParser connect
INFO: Connecting to JMX at service:jmx:rmi:///jndi/rmi://localhost:8089/jmxrmi
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.672 sec
Running com.rsmart.sakai.UserDirectoryServiceTest
Jan 11, 2013 9:57:32 AM org.jboss.arquillian.container.tomcat.ProtocolMetadataParser connect
INFO: Connecting to JMX at service:jmx:rmi:///jndi/rmi://localhost:8089/jmxrmi
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.325 sec

Results :

Tests run: 2, Failures: 0, Errors: 0, Skipped: 0

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESSFUL
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 12 seconds
[INFO] Finished at: Fri Jan 11 09:57:34 MST 2013
[INFO] Final Memory: 22M/81M
[INFO] ------------------------------------------------------------------------
```

Creating Test Cases
-------------------
The existing Arquillian guides are a great resource give them a look: http://arquillian.org/guides/getting_started/
The big things are to annotate your class with  @RunWith(Arquillian.class) and use the @Deployment for packaging.  Other than that everything is standard junit stuff.  Here is a sample:

```
@RunWith(Arquillian.class)
public class UserDirectoryServiceTest {
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    @Test
    public void testGetUser() {
        UserDirectoryService uds = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
        User user = null;
        try {
            user = uds.getUser("admin");
        } catch (UserNotDefinedException e) {
            assertFalse(true);
        }
        assertNotNull(user);
    }


}
```

The ShrinkWrap API that is used in the @Deployment method is there to package up any test specific dependencies you need.  You should not add any Sakai dependencies that are in shared here, since those will already be available to your test web app.  So if you are used to writing Sakai tools this is no different.  So for testing core services you won't need to add anything, but if you need to add some utilities jars for creating random data, or kernel-util, or whatever else you need, you do it with ShrinkWrap calls.  You can find many examples at https://github.com/arquillian/arquillian-examples.  Also there is a Spring module that can be used to integrate Spring: https://github.com/arquillian/arquillian-extension-spring.  I think its going to be a slippery slope if you attempt to recreate the whole Sakai spring tree on the client.  But it might be useful for client side only stuff as the test suite build up.

Video Demonstration
-------------------
If you like my sexy voice, check this out http://www.youtube.com/watch?v=RfFS4k98x1A&feature=youtu.be
