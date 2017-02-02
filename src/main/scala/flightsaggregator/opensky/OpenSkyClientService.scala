package flightsaggregator.opensky

import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, StatusCode, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import flightsaggregator.core.http.Error
import flightsaggregator.opensky.OpenSkyClientService.OpenSkyResponse
import flightsaggregator.opensky.domain.{OpenSkyConfig, OpenSkyStatesRequest, OpenSkyStatesResponse}
import spray.json.RootJsonFormat

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object OpenSkyClientService {
  type OpenSkyResponse[T] = Either[Error, T]
}

class OpenSkyClientService(config: OpenSkyConfig, logger: LoggingAdapter)(implicit ec: ExecutionContext) {

  private val host = config.host

  def getChannelHistory(request: OpenSkyStatesRequest): Future[OpenSkyResponse[OpenSkyStatesResponse]] = {
    val statesRequest = buildStatesRequest(request)
    sendRequestToOpenSky[OpenSkyStatesResponse](statesRequest, "Couldn't retrieve OpenSky states.")
  }

  private def buildStatesRequest(request: OpenSkyStatesRequest) = RequestBuilding.Get(Uri(s"$host/states/all"))

  private def sendRequestToOpenSky[T](request: HttpRequest, failureMessage: String)(implicit conversion: RootJsonFormat[T]) = {
    logger.debug("[OpenSkyClientService] request sent to Slack {} ", request)
    Http().singleRequest(request).flatMap { response =>
      response.entity.toStrict(5.seconds).flatMap { entity =>
        logRequestResponse(request, response.status, entity)
        response.status match {
          case OK => Unmarshal(entity).to[T].map(Right(_))
          case _  => Future.successful(Left(Error(failureMessage)))
        }
      }
    }
  }


  private def logRequestResponse(request: HttpRequest, status: StatusCode, response: HttpEntity.Strict): Unit =
    logger.info(infoMessage(request, status, response.data.utf8String))

  private def infoMessage(request: HttpRequest, status: StatusCode, responseBody: String) =
    s"[OpenSkyService] Request sent to OpenSky: $request \n OpenSky response status: $status and body: $responseBody"

}
