import play.api.{GlobalSettings, Application}
import shib.Shib
import scala.language.implicitConversions

/**
 * User: dweinberg
 * Date: 7/25/13
 * Time: 9:56 PM
 */
object Global extends GlobalSettings {

  override def beforeStart(app: Application) = {
    //Shib: this is necessary in dev mode because of classloader issues.  There are still some issues with c3p0 which is why the
    //c3p0 settings are such that it will fail quickly and restart.  Will still get noise in the logs, but hot reloading does work
    Shib.buildSessionFactory()
  }

  override def onStop(app: Application) {
    Shib.closeSessionFactory()
  }

  //Shib: you can handle stale objects here
/*
  override def onError(request: RequestHeader, ex: Throwable): Future[SimpleResult] = {
    ex.getCause match {
      case _: StaleObjectStateException => Future {
        Results.Conflict {
          views.html.staleObject()
        }
      }
      case _ => super.onError(request, ex)
    }
  }*/

}
