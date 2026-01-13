

package com

import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.kafka.{ConsumerSettings, Subscriptions}
import org.apache.pekko.kafka.scaladsl.Consumer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.UUID
import spray.json._
import ModelsJsonProtocol._
import scala.concurrent.Await
import scala.concurrent.duration._
// *** ADD THESE IMPORTS ***
import org.apache.kafka.common.serialization.StringSerializer // Fixes: not found: type StringSerializer
import org.apache.pekko.kafka.ProducerSettings // Fixes: not found: value ProducerSettings
// *************************

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("KafkaConsumerSystem")
  implicit val materializer: Materializer = Materializer(system)
  import system.dispatcher

  val topic = "ingestion-events"
  val sinkTopic = "chalja-topic" // <--- TARGET TOPIC DEFINED
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("ai-bi-" + UUID.randomUUID())
    .withProperty("auto.offset.reset", "latest")

  // read API key and create LLM client

  val apiKey = sys.env.getOrElse("OPENROUTER_API_KEY","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")
  //val apiKey = sys.env.getOrElse("OPENROUTER_API_KEY", "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")
  //val apiKey = sys.env.getOrElse("OPENROUTER_API_KEY", "8f63106d-6239-4ae8-8dd2-c60a3f03a7b6")

  if (apiKey.isEmpty) {
    println("ERROR: set OPENROUTER_API_KEY environment variable before running.")
    sys.exit(1)
  }
  val llmClient = new LlmClient(apiKey)

  // ---- sample events used to ask LLM to discover KPIs ----
  val sampleEvents: Seq[String] = Seq(
    """{"stock":"AAPL","price":180.5,"volume":1000}""",
    """{"stock":"GOOG","price":135.2,"volume":500}"""
  )

  def extractKPIsFromLLMResponse(raw: String): Seq[KPIDefinition] = {
    try {
      // 1) Extract the code block or bracketed JSON
      val jsonText =
        if (raw.contains("```")) {
          // Get only the content inside backticks
          raw.split("```").find(_.contains("["))
            .getOrElse(throw new RuntimeException("No JSON block found"))
        } else {
          // Try to find the first '[' and the last ']'
          val start = raw.indexOf("[")
          val end = raw.lastIndexOf("]")
          if (start >= 0 && end > start) raw.substring(start, end + 1)
          else throw new RuntimeException("No JSON array found in response")
        }

      val cleaned = jsonText.trim
      println(s"[DEBUG] Cleaned JSON for parsing: $cleaned")

      // 2) Parse the cleaned JSON
      cleaned.parseJson.convertTo[Seq[KPIDefinition]]
    } catch {
      case e: Exception =>
        println(s"[Main] Error parsing LLM content: ${e.getMessage}")
        Seq.empty
    }
  }



  def discoverKPIsFromLLM(events: Seq[String]): Seq[KPIDefinition] = {
    val prompt =
      s"""
         |You are an analytics assistant. Given the sample JSON events below, suggest a concise list of KPI definitions.
         |Return only a JSON array where each element is:
         |{ "kpiName": "<fieldName>", "entity": null, "kpiType": "numeric" | "semantic" }
         |
         |Sample events:
         |${events.mkString("\n")}
         |
         |Example output:
         |[{"kpiName":"price","entity":null,"kpiType":"numeric"},{"kpiName":"volume","entity":null,"kpiType":"numeric"}]
     """.stripMargin

    // Call LLM
    val futureResp = llmClient.callLLM(prompt)
    val resp = Await.result(futureResp, 30.seconds)

    try {
      println(s"[DEBUG] Raw LLM response: $resp")
      extractKPIsFromLLMResponse(resp)
    } catch {
      case ex: Exception =>
        println(s"[Main] Failed to parse KPI definitions from LLM response: ${ex.getMessage}")
        println(s"[Main] Raw LLM response:\n$resp")
        Seq.empty
    }
  }


  //println(s"Starting Kafka consumer on topic: $topic")
  //val kpiDefs = discoverKPIsFromLLM(sampleEvents)
  //println(s"Discovered KPI Definitions: $kpiDefs")


  // *** NEW PRODUCER SETUP ***
  val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers("localhost:9092")

  //val kpiActor = system.actorOf(Props(new KPIActor(kpiDefs, batchSize = 50, Some(llmClient))), "kpi-actor")

  val kpiActor = system.actorOf(
    Props(new KPIActor( batchSize = 50, Some(llmClient), sinkTopic, producerSettings)),
    "kpi-actor"
  )
  // Kafka consumer -> send messages to KPI actor
  Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
    .map { msg =>
      kpiActor ! ProcessEvent(msg.value())
      msg.value()
    }
    .runWith(Sink.ignore)

  println("Kafka consumer running. Press ENTER to exit.")
  scala.io.StdIn.readLine()

  // flush
  kpiActor ! FlushBatch
  system.terminate()
}
