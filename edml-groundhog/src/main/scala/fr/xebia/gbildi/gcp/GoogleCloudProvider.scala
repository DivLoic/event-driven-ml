package fr.xebia.gbildi.gcp

import java.io.FileInputStream

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.bigquery.{BigQuery, BigQueryOptions, FieldValueList, QueryJobConfiguration}
import com.google.cloud.storage.{Storage, StorageOptions}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.Try


/**
 * Created by loicmdivad.
 */
trait GoogleCloudProvider {

  private val logger = LoggerFactory.getLogger(getClass)

  val GoogleApplicationCredentials = "GOOGLE_APPLICATION_CREDENTIALS"

  def getCredential: Try[ServiceAccountCredentials] =
    Try(sys.env(GoogleApplicationCredentials))
      .map(new FileInputStream(_))
      .map(ServiceAccountCredentials.fromStream)

  def getCloudStorageClient(onGCPInfrastructure: Boolean): Try[Storage] =
    if(onGCPInfrastructure) Try(StorageOptions.getDefaultInstance.getService)
    else getCredential.map { credential =>
        StorageOptions
          .newBuilder()
          .setCredentials(credential)
          .build()
          .getService
      }

  def getBigQueryClient(onGCPInfrastructure: Boolean): Try[BigQuery] =
    if(onGCPInfrastructure) Try(BigQueryOptions.getDefaultInstance.getService)
    else getCredential.map { credential =>
      BigQueryOptions
        .newBuilder()
        .setCredentials(credential)
        .build()
        .getService
    }

  def queryStream(bq: BigQuery, query: String): () => Iterator[FieldValueList] = () => {
    val queryConfig = QueryJobConfiguration.newBuilder(query).build
    logger info s"Start the following query: $query"
    val table = bq.query(queryConfig)
    table.iterateAll().iterator.asScala
  }
}
