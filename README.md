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
