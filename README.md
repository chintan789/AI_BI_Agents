# AI + BI Agents Platform

AI + BI Agents is an experimental platform to explore how **AI agents, real-time analytics, and BI systems** can work together to generate insights, KPIs, and decision support automatically.

The goal is to combine:
- Streaming & real-time data (Kafka, Pinot)
- Analytical storage
- AI / LLM-driven agents
- BI-style metrics and dashboards

into a single architecture for **autonomous, intelligent analytics**.

---

## 🚀 Objectives

- Build AI agents that can:
  - Understand business questions
  - Generate KPIs and metrics dynamically
  - Query analytical systems
  - Provide explanations and insights
- Integrate real-time and batch data sources
- Support modern analytical backends (Pinot, Kafka)
- Enable future integration with BI tools (Superset, Power BI)

---

## 🧩 Architecture (High Level)

```text
Data Sources → Kafka → Pinot / Storage → AI Agents → Superset / Dashboards → Users




Main components:

Akka Streams / Pekko — real-time ingestion, transformation, and streaming

Kafka — message bus for streaming data

Pinot — low-latency analytical storage

AI Agents — reasoning layer (Python-based)

Superset — BI dashboards and charts with live/real-time refresh



⚙️ Tech Stack

Language: Scala (Akka / Pekko), Python (AI agents)

Streaming / Ingestion: Akka Streams / Pekko

Messaging: Apache Kafka

Analytics DB: Apache Pinot

BI / Visualization: Superset

Containerization: Docker

Future: LLMs, Vector DB, BI tools




AI_BI_Agents/
│
├── ai_bi/              # Core AI + BI agent logic
├── kafka_docker/       # Kafka setup using Docker
├── pinot/              # Apache Pinot configs / setup
├── README.md
└── README.txt




Future Roadmap

 LLM integration (OpenAI / local models)

 Vector DB for context memory

 Alerting and recommendations engine

 UI dashboard


![iScreen Shoter - Google Chrome - 260114213914](https://github.com/user-attachments/assets/5c5bf9c4-abe2-4813-beef-3b6e499a3272)



![iScreen Shoter - Google Chrome - 260114214129](https://github.com/user-attachments/assets/8af1d552-7d12-4da4-ae5b-23fbd2ad8e45)


