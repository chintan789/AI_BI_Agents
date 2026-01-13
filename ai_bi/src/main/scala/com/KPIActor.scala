package com

import org.apache.pekko.actor.Actor
import spray.json._
import scala.concurrent.Await
import scala.concurrent.duration._
import java.time.Instant
// *** ADD THESE IMPORTS ***
import org.apache.pekko.kafka.ProducerSettings
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.Producer

import com.ModelsJsonProtocol._

// Messages for actor
case class ProcessEvent(json: String)
case object FlushBatch

// Updated constructor to accept sinkTopic and producerSettings
class KPIActor(
                //kpiDefinitions: Seq[KPIDefinition],
                batchSize: Int = 50,
                llmClientOpt: Option[LlmClient] = None,
                sinkTopic: String, // <--- NEW PARAMETER
                producerSettings: ProducerSettings[String, String] // <--- NEW PARAMETER
              ) extends Actor {

  // --- Constants ---
  private val ACTOR_SOURCE = "AI_Engine"
  // -----------------

  private var buffer = Vector.empty[String]
  private val kafkaProducer: Producer[String, String] = producerSettings.createKafkaProducer()

  override def receive: Receive = {
    case ProcessEvent(json) =>
      buffer :+= json
      if (buffer.size >= batchSize) {
        processBatch(buffer)
        buffer = Vector.empty
      }

    case FlushBatch =>
      if (buffer.nonEmpty) {
        processBatch(buffer)
        buffer = Vector.empty
      }
  }

  private def extractJsonArray(raw: String): String = {
    val arrayStart = raw.indexOf("[")
    val arrayEnd = raw.lastIndexOf("]")
    if (arrayStart >= 0 && arrayEnd >= 0 && arrayEnd > arrayStart)
      raw.substring(arrayStart, arrayEnd + 1)
    else
      raw
  }

  import spray.json._
  import java.time.Instant

  import spray.json._
  import java.time.Instant

  private def fixUnixTimestamps(rawJson: String): String = {
    rawJson.parseJson match {
      case JsArray(elements) =>
        JsArray(elements.map {
          case obj: JsObject =>
            obj.fields.get("timestampInEpoch") match {
              case Some(JsNumber(epoch)) =>
                val iso = Instant.ofEpochSecond(epoch.toLong).toString
                JsObject(obj.fields + ("timestamp" -> JsString(iso)))
              case _ => obj
            }
          case other => other
        }).compactPrint

      case other => other.compactPrint
    }
  }



  // Add clean up logic for the producer
  override def postStop(): Unit = {
    kafkaProducer.close()
    println(s"Closed Kafka Producer for topic $sinkTopic.")
  }

  private def processBatch(events: Vector[String]): Unit = {
    println(s"Processing batch of ${events.size} events")

    val parsed = events.flatMap { e =>
      try Some(e.parseJson.asJsObject)
      catch {
        case _: spray.json.JsonParser.ParsingException =>
          println(s"[Warning] Skipping invalid JSON: $e")
          None
      }
    }
    // --- STRICT NUMERIC KPI EXTRACTION (ENTITY IS REQUIRED) ---

    val numericKPIs: Seq[KPI] = parsed.flatMap { jsObj =>

      // 🔴 Entity must exist — otherwise discard the record
      val entityId: Option[String] =
        jsObj.fields.get("symbol") match {
          case Some(JsString(s)) => Some(s)
          case Some(v)           => Some(v.toString.replaceAll("\"", ""))
          case None              => None
        }

      if (entityId.isEmpty) {
        println(s"[WARN] Dropping record – entity_id missing: $jsObj")
        Nil
      } else {

        val timestamp: Long =
          jsObj.fields.get("timestamp") match {
            case Some(JsNumber(v)) => v.toLong
            case Some(JsString(s)) =>
              try s.toLong catch { case _: Exception => Instant.now().getEpochSecond }
            case _ => Instant.now().getEpochSecond
          }

        jsObj.fields.flatMap {
          case (fieldName, JsNumber(v))
            if fieldName != "symbol" && fieldName != "timestamp" =>

            Some(
              KPI(
                kpi_name = fieldName,
                kpi_type = "Numeric",
                entity_id = entityId,   // ✅ explicitly supplied
                source = ACTOR_SOURCE,
                value_str = None,
                value_num = Some(v.toDouble),
                timestampInEpoch = timestamp
              )
            )

          case _ => None
        }
      }
    }

    println(s"[Debug] Numeric KPIs extracted: ${numericKPIs.size}")

    //    // --- DYNAMIC NUMERIC KPI EXTRACTION ---
//    val numericKPIs: Seq[KPI] = parsed.flatMap { jsObj =>
//      jsObj.fields.flatMap { case (fieldName, value) =>
//        val numericOpt: Option[Double] = value match {
//          case JsNumber(v) => Some(v.toDouble)
//          case JsString(s) =>
//            try Some(s.trim.toDouble)
//            catch { case _: NumberFormatException => None }
//          case _ => None
//        }
//
//        numericOpt.map { v =>
//          KPI(
//            kpi_name = fieldName,
//            kpi_type = "Numeric",
//            entity_id = jsObj.fields.get("entity_id").map(_.toString),
//            source = ACTOR_SOURCE,
//            value_str = None,
//            value_num = Some(v),
//            timestampInEpoch = jsObj.fields.get("timestamp")
//              .flatMap(t => try Some(t.toString.toLong) catch { case _: Exception => None })
//              .getOrElse(Instant.now().getEpochSecond)
//          )
//        }
//      }
//    }
//    println(s"[Debug] Numeric KPIs extracted: ${numericKPIs.size}")

    // ✅ LLM semantic KPI extraction - PROMPT AND MAPPING UPDATED
    val semanticKPIs: Seq[KPI] = llmClientOpt match {
      case Some(client) =>
        val prompt =
          s"""
             |You are an analytics assistant.
             |Your task is to return ONLY gated KPIs from the input events.
             |❗ IMPORTANT: Return a JSON array with EXACTLY these 7 keys for each object:
             |  "kpi_name", "kpi_type", "entity_id", "source", "value_str", "value_num", "timestampInEpoch".
             |The "kpi_type" must be "Semantic".
             |Set "source" to "LLM_Dummy" to satisfy the JSON structure.
             |Use "value_str" for the description and set "value_num" to null or a relevant metric.
             |
             |Events:
             |${events.mkString("\n")}
           """.stripMargin

        try {
          val resp = Await.result(client.callLLM(prompt), 45.seconds)

          try {
            val cleanJson = extractJsonArray(resp)
            // Note: fixUnixTimestamps still looks for the old 'timestamp' field.
            // If the LLM uses 'timestampInEpoch', this part might need adjustment
            // but for now, we rely on the LLM to output what we ask.
           // val fixedJson = fixUnixTimestamps(cleanJson)

            val rawSemanticKPIs = cleanJson.parseJson.convertTo[Seq[KPI]]

            // Override the LLM's source with the hardcoded ACTOR_SOURCE
            rawSemanticKPIs.map(kpi => kpi.copy(source = ACTOR_SOURCE))

          } catch {
            case ex: Exception =>
              println(s"[KPIActor] Failed to parse semantic KPI JSON: ${ex.getMessage}")
              println(s"[KPIActor] Raw LLM response:\n$resp")
              Seq.empty
          }
        } catch {
          case ex: Exception =>
            println(s"[KPIActor] LLM call failed: ${ex.getMessage}")
            Seq.empty
        }

      case None => Seq.empty
    }

    val finalKPIs = numericKPIs ++ semanticKPIs
    // ✅ PRODUCING TO KAFKA
    if (finalKPIs.nonEmpty) {
      println(s"Producing ${finalKPIs.size} KPIs to topic: $sinkTopic")

      finalKPIs.foreach { kpi =>
        val kpiJson = kpi.toJson.compactPrint
        // FIX: Using the new kpi_name field for the Kafka key
        val key = kpi.kpi_name

        // Use the Kafka Producer directly to send the record
        val record = new ProducerRecord[String, String](sinkTopic, key, kpiJson)
        kafkaProducer.send(record)

        println(s"[Produced] Key: $key, Value: $kpiJson")
      }
      kafkaProducer.flush()
    }
  }
}
