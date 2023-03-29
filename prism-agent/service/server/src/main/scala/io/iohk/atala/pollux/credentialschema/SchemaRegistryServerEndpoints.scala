package io.iohk.atala.pollux.credentialschema

import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.credentialschema.SchemaRegistryEndpoints.*
import io.iohk.atala.pollux.credentialschema.controller.{CredentialSchemaController, CredentialSchemaControllerLogic}
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage,
  FilterInput
}
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.{Task, URIO, ZIO, ZLayer}

import java.util.UUID

class SchemaRegistryServerEndpoints(
    credentialSchemaController: CredentialSchemaController
) {
  def throwableToInternalServerError(throwable: Throwable) =
    ZIO.fail[ErrorResponse](ErrorResponse.internalServerError(detail = Option(throwable.getMessage)))

  val createSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    createSchemaEndpoint.zServerLogic { case (ctx: RequestContext, schemaInput: CredentialSchemaInput) =>
      credentialSchemaController.createSchema(schemaInput)(ctx)
    }

  val updateSchemaServerEndpoint: ZServerEndpoint[Any, Any] =
    updateSchemaEndpoint.zServerLogic {
      case (ctx: RequestContext, author: String, id: UUID, schemaInput: CredentialSchemaInput) =>
        credentialSchemaController.updateSchema(author, id, schemaInput)(ctx)
    }

  val getSchemaByIdServerEndpoint: ZServerEndpoint[Any, Any] =
    getSchemaByIdEndpoint.zServerLogic { case (ctx: RequestContext, guid: UUID) =>
      credentialSchemaController.getSchemaByGuid(guid)(ctx)
    }

  val lookupSchemasByQueryServerEndpoint: ZServerEndpoint[Any, Any] =
    lookupSchemasByQueryEndpoint.zServerLogic {
      case (
            ctx: RequestContext,
            filter: FilterInput,
            paginationInput: PaginationInput,
            order: Option[Order]
          ) =>
        credentialSchemaController.lookupSchemas(
          filter,
          paginationInput.toPagination,
          order
        )(ctx)
    }

  val testServerEndpoint: ZServerEndpoint[Any, Any] =
    testEndpoint.zServerLogic(requestContext => ZIO.succeed(requestContext.request.toString))

  val all: List[ZServerEndpoint[Any, Any]] =
    List(
      createSchemaServerEndpoint,
      updateSchemaServerEndpoint,
      getSchemaByIdServerEndpoint,
      lookupSchemasByQueryServerEndpoint,
      testServerEndpoint
    )
}

object SchemaRegistryServerEndpoints {
  def all: URIO[CredentialSchemaController, List[ZServerEndpoint[Any, Any]]] = {
    for {
      schemaRegistryService <- ZIO.service[CredentialSchemaController]
      schemaRegistryEndpoints = new SchemaRegistryServerEndpoints(
        schemaRegistryService
      )
    } yield schemaRegistryEndpoints.all
  }
}
