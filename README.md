This is an integration suite/framework for Sakai.  It leverages the Arquillian framework from jboss (http://arquillian.org/).

Unit testing in Sakai is hard.  You end up either creating a lot of mocks or struggling with the component manager to either get pieces or the full content loaded.  In my experience, a lot of bugs are environment specific.  Folks use all sorts of different providers, extensions, and various external systems.  Configuration, data, and network issues can break things.  A lot of things aren't discovered until production because testing the real environment is difficult, and a lot of time facilitating the test only through the UI is either difficult or impossible to test every flow.

This framework aims to solve this problem.  What is does is let you junit (or testng) to write tests in the normal way.  You have full access to the real Sakai services.  You then package up any dependencies needed for your test using ShrinkWrap api.  The framework then packages up a war file with your test and dependencies and uses Tomcat's manager app and jmx to deploy and run your tests.  It then undeploys your test.war when its finished.  

Configuration
//TODO

Running
//TODO

Add Test Cases
//TODO
