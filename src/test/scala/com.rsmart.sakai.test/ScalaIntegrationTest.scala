package com.rsmart.sakai.test

import org.jboss.arquillian.junit.Arquillian
import org.junit.runner.RunWith
import org.jboss.shrinkwrap.api.{GenericArchive, ShrinkWrap}
import org.jboss.shrinkwrap.api.spec.WebArchive
import org.junit.Test
import org.jboss.arquillian.container.test.api.Deployment

import org.sakaiproject.component.cover.ComponentManager
import org.sakaiproject.user.api.{User, UserDirectoryService}
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers

/**
 * Created with IntelliJ IDEA.
 * User: jbush
 * Date: 3/12/13
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(classOf[Arquillian])
object ScalaIntegrationTest {

  @Deployment
  def deployment(): WebArchive = {
    val resolver: MavenDependencyResolver = DependencyResolvers.use(classOf[MavenDependencyResolver]).loadMetadataFromPom("pom.xml")

    return ShrinkWrap.create(classOf[WebArchive], "test.war")
      .addAsLibraries(resolver.artifacts("org.scala-lang:scala-library").resolveAs(classOf[GenericArchive]))
      .addAsLibraries(resolver.artifacts("org.scala-tools.testing:scalatest").resolveAs(classOf[GenericArchive]))
      .addAsLibraries(resolver.artifacts("org.scala-tools.testing:scalacheck").resolveAs(classOf[GenericArchive]))

  }
}

@RunWith(classOf[Arquillian])
class ScalaIntegrationTest {

    @Test def testGetUser {
      val uds: UserDirectoryService = ComponentManager.get(classOf[UserDirectoryService]).asInstanceOf[UserDirectoryService]
      var user: User = null
      user = uds.getUser("admin")
      assert(user != null)
      assert(user.getEmail == "")
    }
  }

