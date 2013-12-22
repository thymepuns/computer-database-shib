package shib

import scala.language.implicitConversions

/**
 * User: dweinberg
 * Date: 11/16/13
 * Time: 9:07 PM
 */
object ImmutableTypes {

  implicit class LazyBackedOption[T](val opt: Option[T]) extends AnyVal {
    def getOrUnBackedError = opt.getOrElse(throw new RuntimeException("No backing object"))
  }

}

abstract class Pk[+ID] {

  def toOption: Option[ID] = this match {
    case Id(x) => Some(x)
    case NotAssigned => None
  }

  def isDefined: Boolean = toOption.isDefined
  def get: ID = toOption.get
  def getOrElse[V >: ID](id: V): V = toOption.getOrElse(id)
  def map[B](f: ID => B) = toOption.map(f)
  def flatMap[B](f: ID => Option[B]) = toOption.flatMap(f)
  def foreach(f: ID => Unit) = toOption.foreach(f)

}

object Pk {
  def Apply[ID](opt: Option[ID]) = opt match {
    case Some(id) => Id(id)
    case None => NotAssigned
  }
}

case class Id[ID](id: ID) extends Pk[ID] {
  override def toString = id.toString
}

case object NotAssigned extends Pk[Nothing] {
  override def toString = "NotAssigned"
}


object Relation {
  implicit def longToFk[CaseType <: ImmutableEntity[_, _]](long: Long) = Fk[CaseType](long)
  implicit def entityToRelationEntity[CaseType <: ImmutableEntity[_, _]](entity: CaseType) = Entity[CaseType](entity)
}


trait Relation[+CaseType <: ImmutableEntity[_, _]]
case class Fk[+CaseType <: ImmutableEntity[_, _]](id: Long) extends Relation[CaseType]
case class Entity[+CaseType <: ImmutableEntity[_, _]](entity: CaseType) extends Relation[CaseType]
case object NoRelation extends Relation[Nothing]
case object Unspecified extends Relation[Nothing]

