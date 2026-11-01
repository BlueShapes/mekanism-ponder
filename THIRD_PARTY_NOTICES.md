# Third-party notices

Mekanism Ponder Guide is distributed under the MIT License in `LICENSE`.

## AE2 Ponder Guide (MIT)

The data-driven scene player, catalogue, progress, tooltip and navigation code
are adapted from AE2 Ponder Guide by aosankaku and contributors (2026):
https://github.com/AoSankaku/ae2-ponder-guide

The local reference was `../guideme-ponder/docs/mod-guide-authoring.md` and its
accompanying implementation. The original MIT notice is retained in `LICENSE`.
This project replaces its namespace, content, target mod and build configuration.
It does not include AE2 lessons, AE2 assets, GuideME code or GuideME translations.

## Ponder (MIT)

Ponder is by The Create Team, Copyright (c) 2022 The Create Team:
https://github.com/Creators-of-Create/Ponder

Ponder 1.0.87+mc1.21.1 is installed separately. Its API and standard playback UI
are used by this guide. The inherited MIT attribution is retained in `LICENSE`
and `LICENSES/MIT-Ponder.txt`. Ponder's JAR, models and textures are not bundled.
This guide does not replace global Ponder captions or translations.

## Mekanism (separate MIT dependency)

Mekanism is by Aidan C. Brady and the Mekanism contributors:
https://github.com/mekanism/Mekanism

Lesson facts, registry identifiers, localized terminology, connection NBT and
multiblock requirements were checked against tag `v1.21.1-10.7.19.85`:
https://github.com/mekanism/Mekanism/tree/v1.21.1-10.7.19.85

Mekanism remains separately installed. No Mekanism implementation, textures,
models or mod JAR is copied into the released guide. Official localized item
names are reused in the independently authored guide translations; the original
MIT notice is included in `LICENSES/MIT-Mekanism.txt`. When a locale lacks an
official name, the guide identifies its proposed translation alongside English.
New explanations are
authored for this project. No endorsement by Mekanism or The Create Team is implied.

## Gradle Wrapper (Apache-2.0)

The wrapper scripts and JAR are Gradle build tooling:
https://github.com/gradle/gradle

Their original notices are preserved, with the complete license in
`LICENSES/Apache-2.0.txt`. The wrapper is part of the source project and is not
included in the released mod JAR.

Minecraft, NeoForge and all separately installed dependencies retain their own
licenses. AI assistance does not replace any original attribution or terms.
