Standalone LittlePet author: SpidiBoost.
Upstream model authors retain the credits listed below.
Corresponding source and build tools: LittlePet-Standalone-SpidiBoost-${version}-sources.zip.

# Alex's Mobs crocodile

Model geometry, UV coordinates and crocodile_0.png from Alex's Mobs 1.22.9.
Authors: Alexthe668 (AlexModGuy), Carro1001, Paint_Ninja.
Source: https://github.com/AlexModGuy/AlexsMobs
Pinned commit: 09755dade2cfbdf14839e026d3af446f9d3ff843 (branch 1.20).
License declared by upstream mods.toml and CurseForge: LGPL-3.0.
License page: https://www.curseforge.com/minecraft/mc-mods/alexs-mobs/license
License copies: licenses/LGPL-3.0.txt and licenses/GPL-3.0.txt.

Changes (2026-10-05): ported AdvancedModelBox geometry to Yarn ModelPart; omitted the optional crown; overlapped neck/head/tail joints; resized the pet to 2.4 blocks; replaced attack animations with a mild mouth opening, walk cycle, and resting pose driven by LittlePet's isLay state. Texture unchanged.

Corresponding adapted Java source is in src/main/java/ in the companion source ZIP; original source and editable geometry are in upstream/alexsmobs/ in that ZIP. No ATools runtime dependency is required.


# Promenade capybara

Model geometry, UV coordinates and brown/eye textures from Promenade v4.1.1.
Authors: Hugman / Dawn Team. Source: https://github.com/DawnTeamMC/Promenade
Pinned commit: 2cbb6585762c28ef4400a6a5fd91b80d92312c30 (v4.1.1).
License: LGPL-3.0, as supplied in that pinned version's LICENSE.md.
The pinned upstream license is included in the companion source ZIP at upstream/promenade/LICENSE.md;
LGPL-3.0 and GPL-3.0 text copies are also included in licenses/.

Changes (2026-10-05): ported the standalone model to Minecraft 1.21.4 ModelPart;
scaled by 0.8; used the upstream larger eyes as a separate overlay with blinking;
added diagonal four-legged walking, rump sitting with forward paws, occasional
back rolling/paw wiggling and smooth AFK transitions driven by LittlePet's isLay.
Compute ground clearance using all transformed cuboid corners in every pose.
All three supplied textures are byte-for-byte upstream assets.
Adapted Java is in src/main/java/ and original files/attribution in upstream/promenade/ in the companion source ZIP.
No Promenade, Dawn API or additional runtime dependency is required.


# Dogs Unleashed dachshund — Такса 2

Original model geometry, UV layout and chocolate/tan texture from Dogs Unleashed.
Copyright (c) 2025-2026 Vitória Vedana and Graham Bartley. License: MIT.
Source: https://github.com/grabartley/minecraft-dogs-unleashed
Pinned commit: 07ac51b6bca56ed6df60deed7438f1179f34c7c1.
Full upstream MIT license is included in the companion source ZIP at upstream/dogs-unleashed/LICENSE and
licenses/Dogs-Unleashed-MIT.txt. Original geometry, animations, texture variants
and attribution are in upstream/dogs-unleashed/ in the companion source ZIP.

Changes (2026-10-05): standalone Minecraft 1.21.4 ModelPart port; Bedrock box UV
conversion; corrected cube coordinate/rotation conversion; reparented independent
face/ear/nose bones into one neck rig; added ankle joints and overlapping rump
connection; scaled by 1.5 to approximately 1.19 blocks nose-to-tail. Replaced
upstream animations with diagonal steps, level paws, head/ear motion, wagging tail
and a unique smooth AFK play bow. Ground clearance is computed from actual quad
vertices including source inflation values. The chocolate/tan PNG is unchanged.
Bedrock box UV convention checked against GeckoLib's GeometryQuadUvs/VertexSet
implementation (https://github.com/bernie-g/geckolib); no GeckoLib code/runtime
is bundled. Adapted Java source is included in src/main/java/ in the companion source ZIP.
No Dogs Unleashed or GeckoLib mod is required at runtime.

Original Taksa geometry and texture: ATools, released under CC0-1.0 in its fabric.mod.json. Standalone adaptation by SpidiBoost.

TaksaBrain and SmoothValue: ported from the same ATools CC0-1.0 source artifact. No ATools classes or dependency are bundled.
