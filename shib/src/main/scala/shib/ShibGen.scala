package shib

import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.language.implicitConversions

/**
 * User: dweinberg
 * Date: 11/13/13
 * Time: 9:40 PM
 */

trait ImmutableEntityWithRelationships1[CaseType <: ImmutableEntity[CaseType, MutableType], MutableType, RelationType <: ImmutableEntity[_, _]] extends ImmutableEntitySupport[CaseType, MutableType] {
  def insert(entity: CaseType, relation: Relation[RelationType]): CaseType = macro ImmutableEntitySupport.insertWithRelationships1Impl[CaseType, MutableType, RelationType]
}

trait ImmutableEntityWithRelationships2[CaseType <: ImmutableEntity[CaseType, MutableType], MutableType, RelationType <: ImmutableEntity[_, _], RelationType2 <: ImmutableEntity[_, _]] extends ImmutableEntitySupport[CaseType, MutableType] {
  def insert(entity: CaseType, relation: Relation[RelationType], relation2: Relation[RelationType2]): CaseType = macro ImmutableEntitySupport.insertWithRelationships2Impl[CaseType, MutableType, RelationType, RelationType2]
}

trait ImmutableEntitySupport[CaseType <: ImmutableEntity[CaseType, MutableType], MutableType] {

  def apply(mutableEntity: MutableType): CaseType

  def insert(entity: CaseType): CaseType = macro ImmutableEntitySupport.insertImpl[CaseType, MutableType]

  def update(entity: CaseType): CaseType = macro ImmutableEntitySupport.updateImpl[CaseType, MutableType]

  def delete(entity: CaseType) = macro ImmutableEntitySupport.deleteImpl[CaseType, MutableType]

  def findById(id: Long): CaseType = macro ImmutableEntitySupport.findByIdImpl[CaseType, MutableType]

  def findByIdOption(id: Long): Option[CaseType] = macro ImmutableEntitySupport.findByIdOptionImpl[CaseType, MutableType]

  def findAll(): List[CaseType] = macro ImmutableEntitySupport.findAllImpl[CaseType, MutableType]

  def query(hql: String, params: Map[String, Any], firstResult: Option[Int], maxResults: Option[Int]): List[CaseType] = macro ImmutableEntitySupport.queryImpl[CaseType, MutableType]

}


trait ImmutableEntity[CaseType, MutableType] extends IDed with CopyableProperties {
  def copyProperties(mutableEntity: MutableType): MutableType

  def copyPropertiesInternal(mutableEntity: Any): Any = copyProperties(mutableEntity.asInstanceOf[MutableType])
}

trait IDed {
  def id: Pk[_] //this should be restricted to serializable, but because of boxing magic, that would reject Scala's Long
}

trait CopyableProperties {
  def copyPropertiesInternal(mutableEntity: Any): Any
}

object ImmutableEntitySupport {

  private def mutableToImmutable[CaseType: c.WeakTypeTag](c: Context)(tree: c.Tree): c.Expr[CaseType] = {
    import c.universe._
    val caseType = c.weakTypeOf[CaseType]
    c.Expr[CaseType](
      Apply(
        Select(Ident(caseType.typeSymbol.companionSymbol), newTermName("apply")),
        List(tree)
      )
    )
  }

  def findByIdImpl[CaseType: c.WeakTypeTag, MutableType: c.WeakTypeTag](c: Context)(id: c.Expr[Long]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]
    val typeName = mutableType.typeSymbol.fullName
    val ident = c.Expr[String](Literal(Constant(typeName)))
    val loader = reify {
      Shib.session.load(ident.splice, id.splice).asInstanceOf[MutableType]
    }

    mutableToImmutable[CaseType](c)(loader.tree)
  }

  def findByIdOptionImpl[CaseType: c.WeakTypeTag, MutableType: c.WeakTypeTag](c: Context)(id: c.Expr[Long]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]
    val typeName = mutableType.typeSymbol.fullName
    val ident = c.Expr[String](Literal(Constant(typeName)))
    val loader = reify {
      Shib.session.load(ident.splice, id.splice).asInstanceOf[MutableType]
    }

    reify {
      try {
        Option(mutableToImmutable[CaseType](c)(loader.tree).splice)
      } catch {
        case _: org.hibernate.ObjectNotFoundException => None
      }
    }

  }


  def deleteImpl[CaseType <: IDed with CopyableProperties : c.WeakTypeTag, MutableType: c.WeakTypeTag](c: Context)(entity: c.Expr[CaseType]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]
    val typeName = mutableType.typeSymbol.fullName
    val ident = c.Expr[String](Literal(Constant(typeName)))

    reify {
      val mutable = Shib.session.load(ident.splice, entity.splice.id.get.asInstanceOf[java.io.Serializable]).asInstanceOf[MutableType]
      Shib.session.delete(mutable)
    }

  }

  def findAllImpl[CaseType: c.WeakTypeTag, MutableType: c.WeakTypeTag](c: Context)() = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]
    val typeName = mutableType.typeSymbol.fullName
    val ident = c.Expr[String](Literal(Constant(typeName)))

    val itIdentifier = c.Expr[Ident](Ident(newTermName("it")))

    reify {
      import scala.collection.JavaConverters._
      Shib.session.createCriteria(ident.splice).list().asInstanceOf[java.util.List[MutableType]]
        .asScala
        .toList
        .map(it => mutableToImmutable[CaseType](c)(itIdentifier.tree).splice)
    }

  }

  def queryImpl[CaseType: c.WeakTypeTag, MutableType: c.WeakTypeTag](c: Context)
                                                                    (hql: c.Expr[String],
                                                                     params: c.Expr[Map[String, Any]],
                                                                     firstResult: c.Expr[Option[Int]],
                                                                     maxResults: c.Expr[Option[Int]]) = {
    import c.universe._

    val itIdentifier = c.Expr[Ident](Ident(newTermName("it")))

    reify {
      import scala.collection.JavaConverters._
      val query = Shib.session.createQuery(hql.splice)
      params.splice.foreach { case (name, value) =>
        query.setParameter(name, value)
      }
      firstResult.splice.map(query.setFirstResult)
      maxResults.splice.map(query.setMaxResults)

      query.list().asInstanceOf[java.util.List[MutableType]]
        .asScala
        .toList
        .map(it => mutableToImmutable[CaseType](c)(itIdentifier.tree).splice)
    }

  }

  def updateImpl[CaseType <: IDed with CopyableProperties : c.WeakTypeTag, MutableType: c.WeakTypeTag](c: Context)(entity: c.Expr[CaseType]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]
    val typeName = mutableType.typeSymbol.fullName
    val ident = c.Expr[String](Literal(Constant(typeName)))

    val mutableExpr = reify {
      val mutable = Shib.session.load(ident.splice, entity.splice.id.get.asInstanceOf[java.io.Serializable]).asInstanceOf[MutableType]
      entity.splice.copyPropertiesInternal(mutable)
      Shib.session.evict(mutable)
      Shib.session.update(mutable) //basically we need to copy everything _including the version_ so we evict first
      mutable
    }
    mutableToImmutable[CaseType](c)(mutableExpr.tree)
  }

  def insertImpl[CaseType <: CopyableProperties : c.WeakTypeTag, MutableType: c.WeakTypeTag](c: Context)(entity: c.Expr[CaseType]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]

    val newMutableType = c.Expr[MutableType](Apply(Select(New(Ident(mutableType.typeSymbol)), nme.CONSTRUCTOR), Nil))

    reify {
      val mutable = newMutableType.splice
      entity.splice.copyPropertiesInternal(mutable)
      Shib.session.save(mutable)
      mutableToImmutable[CaseType](c)(c.Expr[TermName](Ident(newTermName("mutable"))).tree).splice
    }
  }

  def insertWithRelationships1Impl[CaseType <: IDed with CopyableProperties : c.WeakTypeTag, MutableType: c.WeakTypeTag, RelationType <: ImmutableEntity[_, _]: c.WeakTypeTag]
  (c: Context)(entity: c.Expr[CaseType], relation: c.Expr[Relation[RelationType]]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]

    val newMutableType = c.Expr[MutableType](Apply(Select(New(Ident(mutableType.typeSymbol)), nme.CONSTRUCTOR), Nil))

    val relMutableType = c.weakTypeOf[RelationType]
      .baseType(c.typeOf[ImmutableEntity[_, _]].typeSymbol)
      .asInstanceOf[TypeRefApi]
      .args(1)

    val ident = c.Expr[String](Literal(Constant(relMutableType.toString)))

    val Some(setter) = mutableType.members.find(member => member.typeSignature.asInstanceOf[MethodType].params.headOption.map(_.typeSignature).orNull == relMutableType)

    reify {
      val mutable = newMutableType.splice
      entity.splice.copyPropertiesInternal(mutable)

      val stupid = relation.splice.asInstanceOf[Relation[RelationType]]
      if (stupid != Unspecified) {
        val rel = stupid match {
          case Fk(long) => Shib.session.load(ident.splice, long)
          case NoRelation => null
          case Entity(ent: IDed) => Shib.session.load(ident.splice, ent.id.get.asInstanceOf[java.io.Serializable])
        }
        val _ = rel //this is just to avoid the warning in IDEA
        c.Expr[Unit](Apply(Select(Ident(newTermName("mutable")), setter.asTerm), List(TypeApply(Select(Ident(newTermName("rel")), newTermName("asInstanceOf")), List(TypeTree(relMutableType)))))).splice
      }

      Shib.session.save(mutable)
      mutableToImmutable[CaseType](c)(c.Expr[TermName](Ident(newTermName("mutable"))).tree).splice
    }
  }

  def insertWithRelationships2Impl[CaseType <: IDed with CopyableProperties : c.WeakTypeTag, MutableType: c.WeakTypeTag, RelationType <: ImmutableEntity[_, _]: c.WeakTypeTag, RelationType2 <: ImmutableEntity[_, _]: c.WeakTypeTag]
  (c: Context)(entity: c.Expr[CaseType], relation: c.Expr[Relation[RelationType]], relation2: c.Expr[Relation[RelationType2]]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]

    val newMutableType = c.Expr[MutableType](Apply(Select(New(Ident(mutableType.typeSymbol)), nme.CONSTRUCTOR), Nil))

    val relMutableType = c.weakTypeOf[RelationType]
      .baseType(c.typeOf[ImmutableEntity[_, _]].typeSymbol)
      .asInstanceOf[TypeRefApi]
      .args(1)

    val relMutableType2 = c.weakTypeOf[RelationType2]
      .baseType(c.typeOf[ImmutableEntity[_, _]].typeSymbol)
      .asInstanceOf[TypeRefApi]
      .args(1)

    val ident = c.Expr[String](Literal(Constant(relMutableType.toString)))
    val ident2 = c.Expr[String](Literal(Constant(relMutableType2.toString)))

    val Some(setter) = mutableType.members.find(member => member.typeSignature.isInstanceOf[MethodTypeApi] && member.typeSignature.asInstanceOf[MethodType].params.headOption.map(_.typeSignature).orNull == relMutableType)
    val Some(setter2) = mutableType.members.find(member => member != setter && member.typeSignature.isInstanceOf[MethodTypeApi] && member.typeSignature.asInstanceOf[MethodType].params.headOption.map(_.typeSignature).orNull == relMutableType2)

    reify {
      val mutable = newMutableType.splice
      entity.splice.copyPropertiesInternal(mutable)

      val stupid = relation.splice.asInstanceOf[Relation[RelationType]]
      if (stupid != Unspecified) {
        val rel = stupid match {
          case Fk(long) => Shib.session.load(ident.splice, long)
          case NoRelation => null
          case Entity(ent: IDed) => Shib.session.load(ident.splice, ent.id.get.asInstanceOf[java.io.Serializable])
        }
        val _ = rel //this is just to avoid the warning in IDEA
        c.Expr[Unit](Apply(Select(Ident(newTermName("mutable")), setter.asTerm), List(TypeApply(Select(Ident(newTermName("rel")), newTermName("asInstanceOf")), List(TypeTree(relMutableType)))))).splice
      }
      val stupid2 = relation2.splice.asInstanceOf[Relation[RelationType2]]
      if (stupid2 != Unspecified) {
        val rel = stupid2 match {
          case Fk(long) => Shib.session.load(ident2.splice, long)
          case NoRelation => null
          case Entity(ent: IDed) => Shib.session.load(ident2.splice, ent.id.get.asInstanceOf[java.io.Serializable])
        }
        val _ = rel //this is just to avoid the warning in IDEA
        c.Expr[Unit](Apply(Select(Ident(newTermName("mutable")), setter2.asTerm), List(TypeApply(Select(Ident(newTermName("rel")), newTermName("asInstanceOf")), List(TypeTree(relMutableType2)))))).splice
      }

      Shib.session.save(mutable)
      mutableToImmutable[CaseType](c)(c.Expr[TermName](Ident(newTermName("mutable"))).tree).splice
    }
  }

  //todo this doesn't work, and i have no idea why not!
  /*def insertWithRelationships2Impl2[CaseType <: IDed with CopyableProperties : c.WeakTypeTag, MutableType: c.WeakTypeTag, RelationType <: ImmutableEntity[_, _]: c.WeakTypeTag, RelationType2 <: ImmutableEntity[_, _]: c.WeakTypeTag]
  (c: Context)(entity: c.Expr[CaseType], relation: c.Expr[Relation[RelationType]], relation2: c.Expr[Relation[RelationType2]]):c.Expr[CaseType] = {
    insertWithRelationshipsNImpl[CaseType, MutableType](c)(List(c.weakTypeOf[RelationType], c.weakTypeOf[RelationType2]), entity, List(relation, relation2)).asInstanceOf[c.Expr[CaseType]]
  }

  def insertWithRelationshipsNImpl[CaseType <: IDed with CopyableProperties : c.WeakTypeTag, MutableType: c.WeakTypeTag]
  (c: Context)(relationTypes: List[c.Type], entity: c.Expr[CaseType], relations: List[c.Expr[Relation[_]]]): c.Expr[CaseType] = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]

    val newMutableType = c.Expr[MutableType](Apply(Select(New(Ident(mutableType.typeSymbol)), nme.CONSTRUCTOR), Nil))


    val relMutableTypes = relationTypes.map {
      relType =>
        relType
          .baseType(c.typeOf[ImmutableEntity[_, _]].typeSymbol)
          .asInstanceOf[TypeRefApi]
          .args(1)
    }

    val identList = relMutableTypes.map {
      relMutableType =>
        c.Expr[String](Literal(Constant(relMutableType.toString)))
    }

    //todo multiple of same type
    val setters = relMutableTypes.map {
      relMutableType =>
        mutableType.members.find(member => member.typeSignature.isInstanceOf[MethodTypeApi] && member.typeSignature.asInstanceOf[MethodType].params.headOption.map(_.typeSignature).orNull == relMutableType).get
    }

    reify {
      val mutable = newMutableType.splice
      entity.splice.copyPropertiesInternal(mutable)

      for (i <- 0 until relations.size) {
        val relation = relations(i)
        val relMutableType = relMutableTypes(i)
        val ident = identList(i)
        val setter = setters(i)

        if (relation.splice != Unspecified) {
          val rel = relation.splice.asInstanceOf[Relation[_]] match {
            case Fk(long) => Shib.session.load(ident.splice, long)
            case NoRelation => null
            case Entity(ent: IDed) => Shib.session.load(ident.splice, ent.id.get.asInstanceOf[Serializable])
          }
          val _ = rel //this is just to avoid the warning in IDEA
          c.Expr[Unit](Apply(Select(Ident(newTermName("mutable")), setter.asTerm), List(TypeApply(Select(Ident(newTermName("rel")), newTermName("asInstanceOf")), List(TypeTree(relMutableType)))))).splice
        }
      }

      Shib.session.save(mutable)
      mutableToImmutable[CaseType](c)(c.Expr[TermName](Ident(newTermName("mutable"))).tree).splice
    }


  }
*/

}

trait EntityCompanion[MutableType] {
  def findById(id: Long): MutableType = macro EntityCompanion.findByIdImpl[MutableType]
}

object EntityCompanion {
  def findByIdImpl[MutableType: c.WeakTypeTag](c: Context)(id: c.Expr[Long]) = {
    import c.universe._

    val mutableType = c.weakTypeOf[MutableType]
    val typeName = mutableType.typeSymbol.fullName
    val ident = c.Expr[String](Literal(Constant(typeName)))
    reify {
      Shib.session.load(ident.splice, id.splice).asInstanceOf[MutableType]
    }
  }
}
