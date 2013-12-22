package models

import java.util.Date
import shib._
import scala.Some
import ImmutableTypes._
import scala.collection.JavaConversions._
import shib._

/**
 * Helper for pagination.
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

case class Company(id: Pk[Long] = NotAssigned, version: Long = 0, name: String,
                   private val wrapped:Option[mutable.Company] = None) extends ImmutableEntity[Company, mutable.Company] {
  def copyProperties(mutableEntity: mutable.Company): mutable.Company = {
    id.foreach(mutableEntity.setId)
    mutableEntity.setVersion(version)
    mutableEntity.setName(name)
    mutableEntity
  }

  lazy val computers = wrapped.getOrUnBackedError.getComputers.toList.map(Computer.apply)
}

object Company extends ImmutableEntitySupport[Company, mutable.Company]{

  def apply(mutableEntity: mutable.Company): Company = Company(
    Id(mutableEntity.getId),
    mutableEntity.getVersion,
    mutableEntity.getName,
    Some(mutableEntity))

  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options: Seq[(String,String)] = {
    findAll().map(c => c.id.toString -> c.name)
  }
}

case class Computer(id: Pk[Long] = NotAssigned, version: Long = 0, name: String, introduced: Option[Date], discontinued: Option[Date],
                    private val wrapped:Option[mutable.Computer]) extends ImmutableEntity[Computer, mutable.Computer] {
  def copyProperties(mutableEntity: mutable.Computer): mutable.Computer = {
    id.foreach(mutableEntity.setId)
    mutableEntity.setVersion(version)
    mutableEntity.setName(name)
    mutableEntity.setIntroduced(introduced.orNull)
    mutableEntity.setDiscontinued(discontinued.orNull)
    mutableEntity
  }

  lazy val company = Option(wrapped.getOrUnBackedError.getCompany).map(Company.apply)
}

object Computer extends ImmutableEntityWithRelationships1[Computer, mutable.Computer, Company] {

  def apply(mutableEntity: mutable.Computer): Computer = Computer(
    Id(mutableEntity.getId),
    mutableEntity.getVersion,
    mutableEntity.getName,
    Option(mutableEntity.getIntroduced),
    Option(mutableEntity.getDiscontinued),
    Some(mutableEntity))


  val orderFields = Map (
    "name" -> "Computer name",
    "introduced" -> "Introduced",
    "discontinued" -> "Discontinued",
    "company.name" -> "Company")

  /**
   * Return a page of (Computer).
   *
   * @param page Page to display
   * @param pageSize Number of computers per page
   * @param orderBy Computer property used for sorting
   * @param orderDesc Sort descending (or else ascending)
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: String = "name", orderDesc:Boolean = false, filter: String = "%"): Page[Computer] = {
    val offset = pageSize * page

    val orderClause = "order by " + (if (orderFields.isDefinedAt(orderBy)) orderBy else "name") + (if (orderDesc) " desc " else "") + " nulls last"

    val computers = query("from models.mutable.Computer where name like :filter " + orderClause, Map("filter" -> filter), Some(offset), Some(pageSize))

    val countQuery = Shib.session.createQuery("select count(*) from models.mutable.Computer where name like :filter")
    countQuery.setString("filter", filter)
    val totalRows = countQuery.list().get(0).asInstanceOf[Long]

    Page(computers, page, offset, totalRows)
  }

}



