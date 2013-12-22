package shib

import org.hibernate.cfg.Configuration
import org.hibernate.service.ServiceRegistryBuilder
import org.hibernate.SessionFactory
import play.api.Play.current
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider
import org.hibernate.service.jdbc.connections.internal.C3P0ConnectionProvider

/**
 * User: dweinberg
 * Date: 11/14/13
 * Time: 10:53 PM
 */
object Shib {

  private def buildConfig() = {
    val config = new Configuration().configure("hibernate.cfg.xml")
    current.configuration.getString("db.default.url").map(config.setProperty("hibernate.connection.url", _))
    current.configuration.getString("db.default.driver").map(config.setProperty("hibernate.connection.driver_class", _))
    current.configuration.getString("db.default.user").map(config.setProperty("hibernate.connection.username", _))
    current.configuration.getString("db.default.password").map(config.setProperty("hibernate.connection.password", _))
    current.configuration.getString("db.default.logStatements").map(config.setProperty("hibernate.show_sql", _))
    current.configuration.getString("db.default.driver").map {
      driver =>
        Map(
          "h2" -> "org.hibernate.dialect.H2Dialect",
          "mysql" -> "org.hibernate.dialect.MySQLDialect"
        ).foreach {
          case (driverString, dialect) =>
            if (driver.contains(driverString))
              config.setProperty("hibernate.dialect", dialect)
        }
    }
    config
  }

  private def buildSessionFactory(config:Configuration) = {
    val serviceRegistryBuilder = new ServiceRegistryBuilder().applySettings(config.getProperties)
    config.buildSessionFactory(serviceRegistryBuilder.buildServiceRegistry())
  }

  object SessionFactoryStrategy extends Enumeration {
    type SessionFactoryStrategy = Value
    val Default, Thread = Value
  }

  private lazy val sessionFactoryStrategy = current.configuration.getString("sessionFactoryStrategy").map(SessionFactoryStrategy.withName).getOrElse(SessionFactoryStrategy.Default)

  private var cachedSessionFactory:SessionFactory = null

  private val threadLocalSessionFactory = new ThreadLocal[(SessionFactory, String)]

  def buildSessionFactory() {
    cachedSessionFactory = buildSessionFactory(buildConfig())
  }

  def closeSessionFactory() {
    val sf: SessionFactoryImpl = cachedSessionFactory.asInstanceOf[SessionFactoryImpl]
    val conn:ConnectionProvider = sf.getConnectionProvider
    conn.asInstanceOf[C3P0ConnectionProvider].close()
    cachedSessionFactory.close()
  }

  def sessionFactory = {
    if (sessionFactoryStrategy == SessionFactoryStrategy.Thread) {
      val Some(dbUrl) = current.configuration.getString("db.default.url")
      if (threadLocalSessionFactory.get() == null || threadLocalSessionFactory.get()._2 != dbUrl) {
        threadLocalSessionFactory.set((buildSessionFactory(buildConfig()), dbUrl))
      }
      threadLocalSessionFactory.get()._1
    } else {
      cachedSessionFactory
    }
  }

  def session = sessionFactory.getCurrentSession

  private val loggedInUserName:ThreadLocal[String] = new ThreadLocal[String]

  def setLoggedInUserName(userName:String) {
    loggedInUserName.set(userName)
  }

  def getLoggedInUserName = loggedInUserName.get()

  def reading[T](block: => T):T = {
    if (session.getTransaction.isActive) {
      block
    } else {
      try {
        session.beginTransaction()
        session.setDefaultReadOnly(true)
        block
      } finally {
        session.getTransaction.rollback()
      }
    }
  }

  def writing[T](block: => T):T = {
    if (session.getTransaction.isActive) {
      if (session.isDefaultReadOnly) {
        throw new RuntimeException("Cannot start writable transaction from a read")
      }
      block
    } else {
      try {
        session.beginTransaction()
        val result = block
        session.getTransaction.commit()
        result
      } finally {
        if (session.getTransaction.isActive)
          session.getTransaction.rollback()
      }
    }
  }

}