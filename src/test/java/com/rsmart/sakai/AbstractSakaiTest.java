package com.rsmart.sakai;

import com.github.javafaker.Faker;
import org.junit.Before;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: jbush
 * Date: 2/5/13
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class AbstractSakaiTest {
    protected UserDirectoryService uds;
    protected SiteService siteService;
    protected Faker faker = new Faker();
    protected Random randomGenerator = new Random();
    protected ContentHostingService chs;
    protected SessionManager sessionManager;
    protected SecurityService securityService;
    protected SearchService searchService;

   @Before
   public void setUp() throws Exception {


       siteService = (SiteService) ComponentManager.get(SiteService.class);
       uds = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
       chs = (ContentHostingService) ComponentManager.get(ContentHostingService.class);
       sessionManager = (SessionManager) ComponentManager.get(SessionManager.class);
       securityService = (SecurityService) ComponentManager.get(SecurityService.class);
       searchService = (SearchService) ComponentManager.get(SearchService.class);

       Session sakaiSession = sessionManager.getCurrentSession();
       sakaiSession.setUserId("admin");
       sakaiSession.setUserEid("admin");

       SuperUserSecurityAdvisor securityAdvisor = new SuperUserSecurityAdvisor();
       securityAdvisor.setSuperUser("admin");
       securityService.pushAdvisor(securityAdvisor);
   }

    public class SuperUserSecurityAdvisor implements SecurityAdvisor {
        private String superUser;

        public SecurityAdvice isAllowed(String userId, String function, String reference) {
            if (userId != null && userId.equals(superUser)) {
                return SecurityAdvice.ALLOWED;
            }
            return SecurityAdvice.PASS;
        }

        public String getSuperUser() {
            return superUser;
        }

        public void setSuperUser(String superUser) {
            this.superUser = superUser;
        }
    }
}
