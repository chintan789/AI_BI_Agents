package com

import spray.json._
import java.time.Instant

// Existing Definition: Remains the same
case class KPIDefinition(kpiName: String, entity: Option[String], kpiType: String)

// --- FINAL UPDATED KPI CASE CLASS (7 fields) ---
// Column names now exactly match the Pinot schema snake_case convention
case class KPI(
                kpi_name: String,             // Maps to Pinot kpi_name (STRING)
                kpi_type: String,             // Maps to Pinot kpi_type (STRING)
                entity_id: Option[String],    // Maps to Pinot entity_id (STRING)
                source: String,               // Maps to Pinot source (STRING)
                value_str: Option[String],    // Maps to Pinot value_str (STRING)
                value_num: Option[Double],    // Maps to Pinot value_num (DOUBLE)
                timestampInEpoch: Long     // Maps to Pinot timestampInEpoch (LONG, after conversion)
              )

object ModelsJsonProtocol extends DefaultJsonProtocol {
  // Existing Instant formatter
  implicit object InstantJsonFormat extends JsonFormat[Instant] {
    // Note: We are writing the timestamp as an ISO-8601 string here.
    // The Pinot ingestion spec must convert this string to EPOCH LONG.
    def write(instant: Instant) = JsString(instant.toString)
    def read(value: JsValue) = value match {
      case JsString(str) => Instant.parse(str)
      case _             => deserializationError("Expected ISO-8601 string for Instant")
    }
  }

  // Updated formatters
  implicit val kpiDefinitionFormat: RootJsonFormat[KPIDefinition] = jsonFormat3(KPIDefinition)

  // KPI now uses the renamed fields (kpi_name, kpi_type, entity_id, etc.)
  implicit val kpiFormat: RootJsonFormat[KPI] = jsonFormat7(KPI)
}
