"""Core logic tests — pure stdlib, no third-party deps required.

Exercises the deterministic offline fallbacks that mirror the Java backend.
"""
import unittest

from anomaly.detector import detect as detect_anomalies
from embeddings.embedder import HashingEmbedder, cosine
from embeddings.faiss_index import BruteForceIndex
from nlp.entity_extractor import extract as extract_entities
from nlp.summarizer import summarize
from psychology.behavior_profiler import profile
from psychology.decision_memory import DecisionMemory
from psychology.feedback_loop import FeedbackLoop
from psychology.intent_classifier import classify
from psychology.mood_detector import detect as detect_mood
from psychology.priority_scorer import score
from rag.ingest import chunk
from rag.retriever import Retriever
from skills.deduplicator import deduplicate
from skills.intake import detect_format, intake


class EmbedderTest(unittest.TestCase):
    def test_deterministic_and_dim(self):
        e = HashingEmbedder()
        a = e.embed("pgvector index tuning")
        b = e.embed("pgvector index tuning")
        self.assertEqual(len(a), 512)
        self.assertEqual(a, b)
        self.assertAlmostEqual(cosine(a, b), 1.0, places=6)

    def test_similarity_ordering(self):
        e = HashingEmbedder()
        q = e.embed("how to tune the pgvector index")
        related = e.embed("pgvector index tuning guide")
        unrelated = e.embed("bought groceries and paid the bill")
        self.assertGreater(cosine(q, related), cosine(q, unrelated))


class IndexTest(unittest.TestCase):
    def test_search_ranks_by_cosine(self):
        e = HashingEmbedder()
        idx = BruteForceIndex()
        idx.add("a", e.embed("pgvector index tuning"), {"text": "pgvector index tuning"})
        idx.add("b", e.embed("cooking pasta recipe"), {"text": "cooking pasta recipe"})
        hits = idx.search(e.embed("tune pgvector index"), top_k=2)
        self.assertEqual(hits[0][0], "a")


class RagTest(unittest.TestCase):
    def test_chunking_respects_budget(self):
        text = "\n\n".join("paragraph %d content here" % i for i in range(50))
        chunks = chunk(text, max_chars=200, overlap=20)
        self.assertTrue(chunks)
        self.assertTrue(all(len(c) <= 200 for c in chunks))

    def test_retriever_ingest_and_query(self):
        r = Retriever()
        n = r.ingest("Netlify deploy command is netlify deploy --prod for the frontend.", source="doc")
        self.assertGreaterEqual(n, 1)
        results = r.query("how to deploy frontend to netlify", top_k=3)
        self.assertTrue(results)
        self.assertIn("netlify", results[0]["text"].lower())

    def test_blank_query_returns_empty(self):
        self.assertEqual(Retriever().query("  "), [])


class SkillsTest(unittest.TestCase):
    def test_format_detection(self):
        self.assertEqual(detect_format(url="https://youtu.be/abc"), "youtube")
        self.assertEqual(detect_format(url="https://example.com"), "web")
        self.assertEqual(detect_format(filename="notes.pdf"), "pdf")
        self.assertEqual(detect_format(filename="readme.md"), "markdown")
        self.assertEqual(detect_format(filename="plain.txt"), "text")

    def test_text_intake_chunks(self):
        result = intake(name="My Skill", text="Step one. Step two.\n\nMore detail here.")
        self.assertEqual(result["format"], "text")
        self.assertGreaterEqual(result["chunk_count"], 1)

    def test_dedup_removes_identical(self):
        recs = [{"text": "same content here"}, {"text": "same content here"}, {"text": "different"}]
        kept = deduplicate(recs, threshold=0.99)
        self.assertEqual(len(kept), 2)


class PsychologyTest(unittest.TestCase):
    def test_intent(self):
        self.assertEqual(classify("remember to call mom"), "CAPTURE")
        self.assertEqual(classify("buy 10 AAPL"), "TRADE_LIVE")
        self.assertEqual(classify("show my watchlist"), "TRADE_WATCH")
        self.assertEqual(classify("plan my day"), "PLAN")
        self.assertEqual(classify("what is my region"), "QUESTION")
        self.assertEqual(classify("hello"), "SMALL_TALK")

    def test_priority(self):
        self.assertGreater(score("URGENT fix now!"), score("someday maybe, fyi"))
        self.assertEqual(score("  "), 0.0)

    def test_mood(self):
        self.assertEqual(detect_mood("this is great, love the progress")["mood"], "positive")
        self.assertEqual(detect_mood("everything is broken and i am stuck")["mood"], "negative")

    def test_feedback_loop(self):
        loop = FeedbackLoop()
        base = loop.weight("QUESTION")
        loop.record("QUESTION", True)
        loop.record("QUESTION", True)
        self.assertGreater(loop.weight("QUESTION"), base)

    def test_behavior_profile(self):
        events = [{"intent": "QUESTION", "hour": 9}, {"intent": "QUESTION", "hour": 9}, {"intent": "PLAN", "hour": 10}]
        p = profile(events)
        self.assertEqual(p["top_intent"], "QUESTION")
        self.assertEqual(p["busiest_hour"], 9)

    def test_decision_memory_recall(self):
        dm = DecisionMemory()
        dm.record("market is crashing hard", "reduce exposure", "ok")
        dm.record("calm sideways market", "hold", "ok")
        hits = dm.recall("the market is crashing", top_k=1)
        self.assertEqual(hits[0]["decision"], "reduce exposure")


class NlpAnomalyTest(unittest.TestCase):
    def test_entities(self):
        ents = extract_entities("Email me at a@b.com about https://x.io on 2026-06-30")
        self.assertIn("a@b.com", ents["emails"])
        self.assertIn("https://x.io", ents["urls"])
        self.assertIn("2026-06-30", ents["dates"])

    def test_summarize_nonempty(self):
        text = " ".join("Sentence number %d about pgvector indexing." % i for i in range(10))
        summary = summarize(text, max_sentences=2)
        self.assertTrue(summary)

    def test_anomaly_detects_spike(self):
        result = detect_anomalies([10, 11, 9, 10, 200, 10, 11])
        self.assertTrue(any(a["value"] == 200 for a in result["anomalies"]))


if __name__ == "__main__":
    unittest.main()
