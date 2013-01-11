package com.rsmart.sakai;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: jbush
 * Date: 1/10/13
 * Time: 12:31 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Arquillian.class)
public class ServerConfigurationServiceTest {
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    @Test
    public void getGetServerId() {
        ServerConfigurationService configurationService = (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class);
        assertEquals(configurationService.getServerId(),"server2");

    }


}
