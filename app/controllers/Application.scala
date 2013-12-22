package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import views._
import models._
import shib._
import models.mutable.companion
import shib.Id

/**
 * Manage a database of computers
 */
object Application extends Controller { 
  
  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.list(0, "name", false, ""))
  
  /**
   * Describe the computer form (used in both edit and create screens).
   */
  val computerForm = Form(
    tuple(
      "computer" -> mapping(
        "id" -> ignored(NotAssigned: Pk[Long]),
        "version" -> longNumber,
        "name" -> nonEmptyText,
        "introduced" -> optional(date("yyyy-MM-dd")),
        "discontinued" -> optional(date("yyyy-MM-dd")),
        "wrapped" -> ignored[Option[mutable.Computer]](None)
      )(Computer.apply)(Computer.unapply),
      "companyId" -> optional(longNumber))
  )
  
  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */  
  def index = Action { Home }
  
  /**
   * Display the paginated list of computers.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on computer names
   */
  def list(page: Int, orderBy: String, orderDesc:Boolean, filter: String) = Action { implicit request =>
    Shib.reading {
      Ok(html.list(
        Computer.list(page = page, orderBy = orderBy, orderDesc = orderDesc, filter = ("%"+filter+"%")),
        orderBy, orderDesc, filter
      ))
    }
  }
  
  /**
   * Display the 'edit form' of a existing Computer.
   *
   * @param id Id of the computer to edit
   */
  def edit(id: Long) = Action {
    Shib.reading {
      Computer.findByIdOption(id).map {
        computer =>
          Ok(html.editForm(id, computerForm.fill((computer, computer.company.map(_.id.get))), Company.options))
      }.getOrElse(NotFound)
    }
  }

  /**
   * Handle the 'edit form' submission 
   *
   * @param id Id of the computer to edit
   */
  def update(id: Long) = Action {
    implicit request =>
      Shib.writing {
        computerForm.bindFromRequest.fold(
        formWithErrors => BadRequest(html.editForm(id, formWithErrors, Company.options)), {
          case (computer, companyId) =>
            Computer.update(computer.copy(id = Id(id)))

            //Shib: There should be a macro for this pattern, but it hasn't been necessary yet in my main project.
            //The below code is only temporary and does not represent best practice
            val mutableComputer = companion.Computer.findById(id)
            mutableComputer.setCompany(companyId.map(id => companion.Company.findById(id)).orNull) //Shib: macro limitation--can't use findById as a partial function


            Home.flashing("success" -> "Computer %s has been updated".format(computer.name))
        }
        )
      }
  }

  /**
   * Display the 'new computer form'.
   */
  def create = Action {
    Shib.reading {
    Ok(html.createForm(computerForm, Company.options))
    }
  }
  
  /**
   * Handle the 'new computer form' submission.
   */
  def save = Action { implicit request =>
    Shib.writing {
    computerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.createForm(formWithErrors, Company.options)),
      { case (computer, companyId) =>
        Computer.insert(computer, companyId.fold[Relation[Company]](NoRelation)(id => Fk(id)))
        Home.flashing("success" -> "Computer %s has been created".format(computer.name))
      }
    )
    }
  }
  
  /**
   * Handle computer deletion.
   */
  def delete(id: Long) = Action {
    Shib.writing {
      Computer.delete(Computer.findById(id))
      Home.flashing("success" -> "Computer has been deleted")
    }
  }

}
            
