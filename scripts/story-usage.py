#!/usr/bin/env python3
"""Report token usage and estimated cost for a story, broken down by agent.

Reads Claude Code's local transcripts, so it needs no collector. The sessions for a
story are the ones scripts/story-run.sh recorded in .story-runs/<JIRA-ID>.sessions.

    scripts/story-usage.py PROJ-123
    scripts/story-usage.py PROJ-123 --json
    scripts/story-usage.py --session <session-id>

Costs are estimates at API list prices, not billing data.
"""
import argparse
import json
import os
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PRICES_AS_OF = "2026-11-05"
PRICES_SOURCE = "https://platform.claude.com/docs/en/about-claude/pricing"
LONG_PROMPT_TOKENS = 100_000

# USD per million tokens: input, 5-minute cache write, 1-hour cache write, cache read, output.
PRICES = {
    "claude-fable-5-1": (10, 12.5, 20, 0.25, 50),
    "claude-opus-5-5": (4, 5, 8, 0.20, 20),
    "claude-sonnet-5-5": (2, 2.5, 4, 0.10, 10),
    "claude-haiku-5-5": (0.10, 0.125, 0.20, 0.01, 0.50),
}
HAIKU_LONG_PROMPT = (0.50, 0.625, 1, 0.05, 2.50)

TOKEN_FIELDS = ("input", "output", "cache_read", "cache_write")


def price_for(model, prompt_tokens):
    for prefix, prices in PRICES.items():
        if model and model.startswith(prefix):
            if prefix == "claude-haiku-5-5" and prompt_tokens > LONG_PROMPT_TOKENS:
                return HAIKU_LONG_PROMPT
            return prices
    return None


def request_cost(usage, prices):
    write_5m, write_1h = cache_writes(usage)
    return (
        (usage.get("input_tokens") or 0) * prices[0]
        + write_5m * prices[1]
        + write_1h * prices[2]
        + (usage.get("cache_read_input_tokens") or 0) * prices[3]
        + (usage.get("output_tokens") or 0) * prices[4]
    ) / 1_000_000


def cache_writes(usage):
    """Split cache writes by lifetime. Without a split, price them all as 5-minute writes."""
    split = usage.get("cache_creation") or {}
    write_1h = split.get("ephemeral_1h_input_tokens") or 0
    total = usage.get("cache_creation_input_tokens") or 0
    return total - write_1h, write_1h


def agent_type(transcript):
    meta = transcript.with_suffix(".meta.json")
    try:
        return json.loads(meta.read_text()).get("agentType") or "unknown-subagent"
    except (OSError, ValueError):
        return "unknown-subagent"


def session_files(session_id, projects_dir):
    """Return (agent name, transcript) pairs for a session and its subagents."""
    for main in projects_dir.glob(f"*/{session_id}.jsonl"):
        yield "main session", main
        for transcript in sorted((main.parent / session_id / "subagents").glob("*.jsonl")):
            yield agent_type(transcript), transcript


def empty_row():
    row = {field: 0 for field in TOKEN_FIELDS}
    row.update(requests=0, cost_usd=0.0, models=set(), unpriced_requests=0)
    return row


def read_transcript(path):
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            try:
                yield json.loads(line)
            except ValueError:
                continue


def collect(session_ids, projects_dir):
    rows = defaultdict(empty_row)
    found = []
    seen_requests = set()
    for session_id in session_ids:
        files = list(session_files(session_id, projects_dir))
        if files:
            found.append(session_id)
        for agent, path in files:
            # A response is written once per content block, so keep the last usage for each message id.
            requests = {}
            for entry in read_transcript(path):
                message = entry.get("message") or {}
                # Claude Code writes errors such as a failed login as "<synthetic>" messages, not model requests.
                if entry.get("type") == "assistant" and message.get("usage") and message.get("model") != "<synthetic>":
                    requests[message.get("id") or entry.get("uuid")] = message
            for request_id, message in requests.items():
                if request_id in seen_requests:
                    continue
                seen_requests.add(request_id)
                add_request(rows[agent], message)
    return rows, found


def add_request(row, message):
    usage, model = message["usage"], message.get("model")
    prompt_tokens = (
        (usage.get("input_tokens") or 0)
        + (usage.get("cache_read_input_tokens") or 0)
        + (usage.get("cache_creation_input_tokens") or 0)
    )
    row["requests"] += 1
    row["input"] += usage.get("input_tokens") or 0
    row["output"] += usage.get("output_tokens") or 0
    row["cache_read"] += usage.get("cache_read_input_tokens") or 0
    row["cache_write"] += usage.get("cache_creation_input_tokens") or 0
    row["models"].add(model or "unknown")
    prices = price_for(model, prompt_tokens)
    if prices:
        row["cost_usd"] += request_cost(usage, prices)
    else:
        row["unpriced_requests"] += 1


def totals(rows):
    total = empty_row()
    for row in rows.values():
        for key in (*TOKEN_FIELDS, "requests", "cost_usd", "unpriced_requests"):
            total[key] += row[key]
        total["models"] |= row["models"]
    return total


def as_json(label, session_ids, found, rows):
    def plain(row):
        return {**row, "models": sorted(row["models"]), "cost_usd": round(row["cost_usd"], 4)}

    return {
        "story": label,
        "sessions_recorded": session_ids,
        "sessions_found": found,
        "agents": {agent: plain(row) for agent, row in sorted(rows.items())},
        "total": plain(totals(rows)),
        "prices": {"as_of": PRICES_AS_OF, "source": PRICES_SOURCE, "estimate": True},
    }


def print_table(label, session_ids, found, rows):
    print(f"Token usage for {label}: {len(found)} of {len(session_ids)} session(s) found\n")
    header = f"{'Agent':<24}{'Requests':>9}{'Input':>10}{'Output':>10}{'Cache write':>13}{'Cache read':>13}{'Est. cost':>11}"
    print(header)
    print("-" * len(header))
    for agent, row in sorted(rows.items(), key=lambda item: -item[1]["cost_usd"]):
        print_row(agent, row)
    print("-" * len(header))
    total = totals(rows)
    print_row("Total", total)
    print(f"\nModels: {', '.join(sorted(total['models']))}")
    print(f"Estimated cost at API list prices as of {PRICES_AS_OF}, not billing data.")
    if total["unpriced_requests"]:
        print(f"Warning: {total['unpriced_requests']} request(s) used a model with no price here, so the cost is too low.")


def print_row(name, row):
    print(
        f"{name:<24}{row['requests']:>9,}{row['input']:>10,}{row['output']:>10,}"
        f"{row['cache_write']:>13,}{row['cache_read']:>13,}{'$' + format(row['cost_usd'], ',.2f'):>11}"
    )


def main():
    parser = argparse.ArgumentParser(description="Report token usage for a story, by agent.")
    parser.add_argument("story", nargs="?", help="Jira ID recorded by scripts/story-run.sh")
    parser.add_argument("--session", action="append", default=[], help="a session ID (repeatable)")
    parser.add_argument("--json", action="store_true", help="print JSON")
    args = parser.parse_args()

    session_ids = list(args.session)
    if args.story:
        if not re.fullmatch(r"[A-Z][A-Z0-9]+-[0-9]+", args.story):
            parser.error(f"not a Jira ID: {args.story}")
        record = ROOT / ".story-runs" / f"{args.story}.sessions"
        if not record.exists():
            sys.exit(f"No sessions recorded for {args.story}. Start the story with scripts/story-run.sh {args.story}.")
        session_ids += record.read_text().split()
    if not session_ids:
        parser.error("give a Jira ID or --session")

    config_dir = Path(os.environ.get("CLAUDE_CONFIG_DIR") or Path.home() / ".claude")
    rows, found = collect(session_ids, config_dir / "projects")
    if not found:
        sys.exit("No transcripts found. They may have been cleaned up, or the run happened on another machine.")

    label = args.story or "selected sessions"
    if args.json:
        print(json.dumps(as_json(label, session_ids, found, rows), indent=2))
    else:
        print_table(label, session_ids, found, rows)


if __name__ == "__main__":
    main()
