# A note from the author

This is my first recruitment task that I have done with the help of AI. Before I started on the implementation
itself, I took a step back and thought about where to begin. I did some research, read about harness engineering,
and decided to start by preparing a setup for my AI tools. I used two models, GPT-6.2-Sol and Claude Opus 5.5,
because one model often finds things and bugs that another one does not.

So that I would not have to repeat the same prompts and context over and over, I also prepared a set of agents
and skills that help me work on the project.

When I design roles like these, I make sure I always stay in control: even when the AI does something, the
decisions are mine in the end.

One model was responsible for the development, and both the same model and another one reviewed it in the same
way. I created and followed the same structure in order to verify whether the findings made by AI were correct or
just redundant edge-case findings.

For one of the fields there was a deliberate architectural decision to accept such a value (a `null` plan type).
Even so, Codex pointed it out as a blocker twice. I consciously ignored these warnings.

In the end, whatever products we build in the era of AI-driven development, a human is the one who uses them.
That is why I ran every solution by hand in Swagger to verify that everything works. I managed to find a bug
in PowerShell that AI would not have found.

If I had more time, I would set up the AI review as part of CI, so that it would be easy to check things at the
level of the code and when reading PRs.

---

*Written by the author, mostly in Polish (one paragraph was written in English), and translated into English by AI
without changing the content; only spelling and grammar were corrected.*
