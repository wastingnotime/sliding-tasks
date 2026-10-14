# Video walkthrough

Two narrated, illustrated walkthroughs introduce Plan, Today, the three planning
choices, Done/Skip/Missed, Undo, and Review. They are approximately 83 seconds
long and have uploaded, synchronized captions. The screens are animated
illustrations rather than recordings of the app.

| Language | YouTube |
| --- | --- |
| English | https://www.youtube.com/watch?v=RxyXm0tKKXg |
| Brazilian Portuguese | https://www.youtube.com/watch?v=Sm0Mm8f_FAs |

Each landing page shows a locally hosted poster and loads the matching
`youtube-nocookie.com` player only after a click. Playback does not start
automatically. A direct YouTube link also works without JavaScript.

The app's About screen opens the matching video externally and labels it as
requiring internet. The offline interactive practice guide remains the primary
learning path.

When replacing a video, update the landing page ID and direct link, the
localized `walkthrough_url` Android resource, the corresponding poster, and
this record together. Site production follows `contracts/site-promotion.md`;
mobile source changes require a subsequent app release.

## Generation process used on October 12, 2026

The animation was authored locally in HTML/JavaScript Canvas. Runway generated
the narration; it did not generate the video frames. YouTube hosts the finished
exports. No app screen recording was used.

### 1. Verify behavior and write the storyboard

Check the task model, Android labels, and practice guide before writing the
script. The base animation has eight scenes over 75 seconds: Today, Routine,
an unanswered weekly task, the alternative weekly Skip decision, One-time,
Undo, Review, and the practice-guide invitation. Its 18 captions also supply
the spoken script.

Keep these distinctions explicit:

- Routine decisions close only that day's occurrence.
- Weekly Done/Skip closes the entire active calendar week; an unanswered task
  can return on another eligible day. Weeks run Monday–Sunday, and the next
  active week depends on the configured interval.
- An unresolved one-time task carries forward; Done/Skip resolves it permanently.
- Undo is temporary. Review distinguishes completed, skipped, and missed cards.
- Review's sample day is illustrative; it does not combine the mutually
  exclusive outcomes shown earlier in the animation.

### 2. Build and render the animation

`walkthrough.html` contains the storyboard, text, colors, phone illustrations,
calendar changes, and horizontal swipe animations. A deterministic
`window.render(t)` draws any frame on a 1920 × 1080 canvas. The interactive
player allows seeking to check individual moments.

`render.cjs` uses Playwright and headless Chromium to render canvas frames at
30 fps, sends JPEG frames to FFmpeg, and exports H.264 MP4 plus the base SRT.
Run `node render.cjs --proof` for scene PNGs, or `node render.cjs` for the
75-second silent base video. Requirements are Node.js, Playwright, Chromium,
FFmpeg, and the chosen fonts (Lato with Arial fallback). The original runner
contains workstation-specific Playwright and Chromium paths; adjust them when
moving to another machine.

### 3. Generate narration through Runway

Submit the exact spoken text to the connected Runway speech tool with:

| Setting | Value used |
| --- | --- |
| Voice | `Maggie` (stock synthetic voice) |
| Speech model | `eleven_multilingual_v2` |
| Speed | `1.0` |
| Language | `en` for English; `pt-br` for Brazilian Portuguese |

English used eight clips, one per scene, with paragraphs separated by pauses.
Brazilian Portuguese used 18 clips, one per caption, making synchronization
independent of paragraph-pause detection. Save the returned MP3s and task IDs
so rebuilding or retiming does not require paying for duplicate generations.
Only regenerate passages whose wording or delivery needs to change.

The delivered narration came from Runway. Earlier local-model and alternative
speech-service attempts did not produce the final audio; the unused
`narrate.py` draft is not part of this process.

### 4. Localize all visible content

`localize-pt-br.py` generated the Portuguese animation and translation mapping.
Translate headings, task examples, buttons, calendars, status labels, captions,
and the closing invitation. Match Android's localized names: Rotina, Uma vez
por semana, Pontual, Hoje, Planejar, Revisar, Concluir, Pular hoje, Pular esta
semana, Pular tarefa, and Desfazer.

Adjust typography and card layout for longer Portuguese labels, then visually
check the exported scene stills. The Portuguese version is rendered separately;
it is not an English video with only the audio replaced.

### 5. Assemble and synchronize

Run `python3 assemble-narration.py` after rendering the base animation and
placing the generated MP3s in `runway-audio/`. This requires Python, NumPy, and
FFmpeg. The English assembler splits clips at measured silent paragraph
boundaries; the Portuguese assembler consumes its 18 complete clips.

The assembler decodes narration to 48 kHz mono, applies short fades at clip
boundaries, and leaves a small leading margin for each passage. Each caption
interval is extended when the speech needs more time, rounded to a 30 fps
frame boundary. Corresponding video segments are retimed with FFmpeg, and
`narrated-captions.srt` is regenerated from that final timeline. Speech stays at
natural speed. `narration-timing.json` records the source and final intervals.

Final exports use H.264 video, 1920 × 1080 at 30 fps, AAC mono audio at 48 kHz
and 192 kbps, and MP4 fast-start metadata. Audio normalization targets -16 LUFS
and -1.5 dBTP before AAC encoding. The English export is 82.5 seconds; the
Portuguese export is approximately 83.37 seconds.

### 6. Validate, publish, and integrate

Inspect scene stills for layout and semantic accuracy. Decode the whole final
MP4 with FFmpeg to check for errors, inspect its streams and duration with
ffprobe, measure encoded audio loudness, and verify that all 18 speech passages
fit within their corresponding contiguous caption intervals. Also watch and
listen to the finished video before future publications to assess pacing and
pronunciation; technical checks alone do not establish voice quality.

Upload each language separately to the Wasting No Time YouTube channel. Set
the matching video language, localized title and description, a thumbnail,
and Public visibility with embedding enabled. Upload the **narrated** SRT,
not the original 75-second SRT. These videos were marked not made for kids,
without paid promotion, and with AI use disclosed. YouTube's initial copyright
and Community Guidelines checks passed for both published videos.

Captions are already visible in the animation; enabling YouTube's optional
caption track can display the same text twice. The uploaded track still makes
the spoken text available through YouTube's caption features.

The landing-page posters were resized to 1280 × 720 and exported as WebP.
`walkthrough.js` replaces the local poster button with a privacy-enhanced
YouTube iframe only after a click, with no autoplay. Check both languages on
desktop and phone widths, the correct video IDs, keyboard focus, and the direct
YouTube fallback link. The Android About screen opens the localized video
externally and identifies the internet requirement. The Android debug build
and the existing test for reopening the practice guide passed after integration.

## Source and artifact location

The original animation sources, rendering/assembly scripts, speech MP3s, task
references, timing files, SRTs, proof images, and MP4 exports were saved in this
workstation directory outside the repository:

```text
/home/henrique/.codex/.chatgpt-projects/g-p-69a47318fb1c8191b4d4a0d9d57cda13/outputs/sliding-tasks-walkthrough/
```

English files are at its root; Portuguese files are under `pt-br/`.
`localize-pt-br.py` is at the root. The repository contains the production
posters, embeds, app links, and this process record, but not the complete video
source bundle. Preserve or archive that output directory before removing the
local chat workspace; a repository checkout alone cannot reproduce these videos.
