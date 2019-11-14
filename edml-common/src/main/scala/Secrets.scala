
import java.io.{File, PrintWriter}
import java.util.Base64

import org.apache.commons.compress.utils.Charsets

/**
 * Created by loicmdivad.
 */
object Secrets extends App {

  val apiKey = System.getenv("API_KEY")
  val secretkey = System.getenv("SECRET_KEY")
  val bootstrapServers = System.getenv("BOOTSTRAP_SERVERS")
  val schemaRegistry = System.getenv("SCHEMA_REGISTRY_URL")
  val saslClass = System.getenv("SASL_CLASS")
  val registryPubkey = System.getenv("SR_API_KEY")
  val registrySecretkey = System.getenv("SR_SECRET_KEY")

  def base64(s: String) = Base64.getEncoder.encodeToString(s.getBytes(Charsets.UTF_8))

  val content =
    s"""|---
        |apiVersion: v1
        |kind: Secret
        |metadata:
        |  name: confluent-secrets
        |data:
        |  api-key: ${base64(apiKey)}
        |  secret-key: ${base64(secretkey)}
        |  bootstrap-servers: ${base64(bootstrapServers)}
        |  schema-registry-url: ${base64(schemaRegistry)}
        |  sasl-class: ${base64(saslClass)}
        |  sr-api-key: ${base64(registryPubkey)}
        |  sr-secret-key: ${base64(registrySecretkey)}
        |""".stripMargin

  val file = new File("out/kubernetes/.secrets.yaml")
  val isFolderExists = file.getParentFile.mkdirs()
  val writer = new PrintWriter(file)

  writer.write(content)
  writer.close()

}


