# Hiraishin Kunai — Minecraft Forge 1.20.1

Mod ID: `hiraishinkunai`  
Target: Minecraft 1.20.1, Forge 47.2.0, Java 17.

## Implemented in source

- Dedicated creative tab **Hiraishin Kunai**, containing the item **Hiraishin Kunai**.
- Hold right-click to charge a throw like a bow. Charge controls force and range, capped at 40 blocks.
- A camera-facing 2D kunai billboard with white distance text is rendered for each active projectile and owned mark.
- Left-click the indicator to send a server-validated teleport request. A successfully selected projectile is collected by its owner.
- A projectile that hits a block stays at the impact. The owner can collect it by walking over it. A projectile that hits a living entity becomes a mark attached to that entity.
- Shift + right-click blocks or living entities to toggle persistent marks without consuming the held kunai. Marks grow in over several ticks and follow entity movement.
- Multiple marks may be active simultaneously. Marks are saved as entities and synced to clients.
- Shift + right-click an owned mark indicator to remove it. Entity marks are removed when the marked entity dies.

## Building

Requires JDK 17 and an internet-connected Gradle 8.x installation with access to Gradle Plugin Portal and Forge Maven. Run `gradle --no-daemon build` from this folder. The intended distributable is `build/libs/hiraishin-kunai-1.0.0.jar`.

A GitHub Actions workflow is included in `.github/workflows/build.yml`. Push this source to a GitHub repository and run the workflow to compile on a hosted Java 17 runner and upload the JAR as an artifact.

## Verification status

The implementation has not been compiled or tested in Minecraft in the current environment because Gradle, Java 17 and ForgeGradle dependencies are unavailable and external DNS resolution failed. Do not treat this source package as a verified release until the build workflow succeeds and the mod is tested in-game.
