#!/usr/bin/env python3
"""chains — declarative RCON acceptance chains of the GT6 modernization project.

Every card's acceptance chain lives here as one python module: sites declared
once (gt6world), lifecycle delegated to gt6server, and the body spelled as
Step records judged by the framework's structured judge_step (ops-judge-literal:
the expect is the assertion; gt6rcon.judge_output stays the CLI layer's face).
Chains are committed, so review
can re-run them byte for byte — no more task-local scripts in tmp/ to rescue.
"""
