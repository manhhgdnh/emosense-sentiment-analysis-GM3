#!/usr/bin/env python3
from pathlib import Path
import csv
from collections import Counter
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "results" / "data" / "sample_scores.csv"
FIG = ROOT / "results" / "figures"
FIG.mkdir(parents=True, exist_ok=True)

rows = []
with DATA.open(newline="", encoding="utf-8") as f:
    rows = list(csv.DictReader(f))

# 1) Sample sentiment scores
labels = [r["id"] for r in rows]
scores = [float(r["score20"]) for r in rows]
fig, ax = plt.subplots(figsize=(11, 5.5))
ax.barh(labels, scores)
ax.axvline(10.0, linewidth=1.2, linestyle="--", label="midpoint = 10")
ax.set_xlim(0, 20)
ax.set_xlabel("Rule-based sentiment score (/20)")
ax.set_title("EmoSense — Rule-Based Scores on Representative Sentences")
ax.legend()
fig.tight_layout()
fig.savefig(FIG / "sample_scores.png", dpi=180, bbox_inches="tight")
plt.close(fig)

# 2) Positive / negative contribution shares
pos = [float(r["positivePercent"]) for r in rows]
neg = [float(r["negativePercent"]) for r in rows]
fig, ax = plt.subplots(figsize=(11, 5.5))
y = range(len(labels))
ax.barh(y, pos, label="positive contribution")
ax.barh(y, neg, left=pos, label="negative contribution")
ax.set_yticks(list(y), labels)
ax.set_xlim(0, 100)
ax.set_xlabel("Contribution share (%)")
ax.set_title("Positive vs Negative Contribution Mass")
ax.legend()
fig.tight_layout()
fig.savefig(FIG / "positive_negative_share.png", dpi=180, bbox_inches="tight")
plt.close(fig)

# 3) Dominant bucket counts
counts = Counter(r["dominantBucket"] for r in rows)
fig, ax = plt.subplots(figsize=(9, 4.8))
ax.bar(list(counts.keys()), list(counts.values()))
ax.set_ylabel("Number of sample sentences")
ax.set_title("Dominant Emotion Buckets in the Demo Set")
ax.tick_params(axis="x", rotation=35)
fig.tight_layout()
fig.savefig(FIG / "dominant_emotions.png", dpi=180, bbox_inches="tight")
plt.close(fig)

# 4) Architecture / processing pipeline
fig, ax = plt.subplots(figsize=(12, 4.6))
ax.axis("off")
steps = [
    (0.08, "Input text"),
    (0.27, "Tokenization\n& normalization"),
    (0.47, "Lexicon +\nmarker matching"),
    (0.67, "Rule engine\ncontributions"),
    (0.87, "Score /20 +\nemotion buckets"),
]
for x, label in steps:
    ax.text(x, 0.62, label, ha="center", va="center", fontsize=11,
            bbox=dict(boxstyle="round,pad=0.5"))
for (x1, _), (x2, _) in zip(steps, steps[1:]):
    ax.annotate("", xy=(x2 - 0.08, 0.62), xytext=(x1 + 0.08, 0.62),
                arrowprops=dict(arrowstyle="->", linewidth=1.5))
ax.text(0.47, 0.18, "Optional comparison: Gemini API via GEMINI_API_KEY",
        ha="center", va="center", fontsize=10,
        bbox=dict(boxstyle="round,pad=0.4"))
ax.annotate("", xy=(0.83, 0.53), xytext=(0.57, 0.23),
            arrowprops=dict(arrowstyle="->", linewidth=1.2, linestyle="--"))
ax.set_title("EmoSense Processing Architecture", fontsize=14, pad=16)
fig.tight_layout()
fig.savefig(FIG / "architecture.png", dpi=180, bbox_inches="tight")
plt.close(fig)

print(f"Generated figures in {FIG}")
