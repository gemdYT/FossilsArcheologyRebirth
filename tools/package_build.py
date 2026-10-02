"""Package the development build and its pinned runtime artifacts for local testing."""
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path.home() / '.gradle/caches/modules-2/files-2.1'
props = {}
for line in (ROOT / 'gradle.properties').read_text(encoding='utf-8').splitlines():
    if '=' in line and not line.startswith('#'):
        key, value = line.split('=', 1)
        props[key.strip()] = value.strip()
DEST = ROOT / f"dist/fossil-fabric-{props['minecraftVersion']}-{props['modVersion'].split('-', 1)[1]}"
MODS = DEST / 'mods'
MODS.mkdir(parents=True, exist_ok=True)
mod_name = f"{props['archivesName']}-fabric-{props['minecraftVersion']}-{props['modVersion']}.jar"
# Remove only superseded copies of this project's jar from its managed local kit.
for previous in MODS.glob(f"{props['archivesName']}-fabric-{props['minecraftVersion']}-*.jar"):
    if previous.name != mod_name:
        previous.unlink()
artifacts = [(ROOT / 'build/libs' / mod_name, mod_name, 'local build'),
    (CACHE / 'net.fabricmc.fabric-api/fabric-api' / props['fabricApiVersion'], f"fabric-api-{props['fabricApiVersion']}.jar", 'https://modrinth.com/mod/fabric-api'),
    (CACHE / 'maven.modrinth/geckolib' / props['geckoLibVersionId'], f"geckolib-fabric-{props['geckoLibVersion']}.jar", 'https://modrinth.com/mod/geckolib/version/' + props['geckoLibVersionId']),
    (CACHE / 'maven.modrinth/terrablender' / props['terraBlenderVersionId'], f"terrablender-fabric-{props['terraBlenderVersion']}.jar", 'https://modrinth.com/mod/terrablender/version/' + props['terraBlenderVersionId']),
    (CACHE / 'maven.modrinth/smartbrainlib' / props['smartBrainLibVersionId'], f"smartbrainlib-fabric-{props['smartBrainLibVersion']}.jar", 'https://modrinth.com/mod/smartbrainlib/version/' + props['smartBrainLibVersionId']),
    (CACHE / 'maven.modrinth/structure-pool-api' / props['structurePoolApiVersionId'], f"structure-pool-api-fabric-{props['structurePoolApiVersion']}.jar", 'https://modrinth.com/mod/structure-pool-api/version/' + props['structurePoolApiVersionId'])]
manifest = {'minecraft': props['minecraftVersion'], 'loader': 'fabric', 'fabricLoader': props['fabricLoaderVersion'], 'java': 25, 'version': props['modVersion'], 'files': []}
for source, name, origin in artifacts:
    if source.is_dir():
        matches = [path for path in source.rglob('*.jar') if not path.name.endswith(('-sources.jar', '-javadoc.jar'))]
        if len(matches) != 1:
            raise RuntimeError(f'Expected one pinned artifact in {source}, found {len(matches)}')
        source = matches[0]
    if not source.is_file():
        raise FileNotFoundError(source)
    shutil.copyfile(source, MODS / name)
    manifest['files'].append({'path': 'mods/' + name, 'sha256': hashlib.sha256(source.read_bytes()).hexdigest(), 'origin': origin})
for document in ['PLAY_GUIDE.md', 'ARCHITECTURE.md', 'CREDITS.md', 'ORIGINAL_CONTRIBUTORS.txt']:
    shutil.copyfile(ROOT / 'docs' / document, DEST / document)
assets = DEST / 'assets'
assets.mkdir(exist_ok=True)
shutil.copyfile(ROOT / 'docs/assets/rebirth-banner.svg', assets / 'rebirth-banner.svg')
(DEST / 'README.md').write_text((ROOT / 'README.md').read_text(encoding='utf-8').replace('docs/PLAY_GUIDE.md', 'PLAY_GUIDE.md').replace('docs/ARCHITECTURE.md', 'ARCHITECTURE.md').replace('docs/CREDITS.md', 'CREDITS.md').replace('docs/ORIGINAL_CONTRIBUTORS.txt', 'ORIGINAL_CONTRIBUTORS.txt').replace('docs/assets/', 'assets/'), encoding='utf-8')
shutil.copyfile(ROOT / 'LICENSE', DEST / 'LICENSE')
(DEST / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
screenshots = ROOT / 'build/run/clientGameTest/screenshots'
for name in ['quetzalcoatlus-flight', 'content-smoke', 'care-and-skeletons', 'dinopedia', 'machine-analyzer', 'machine-culture_vat', 'machine-sifter', 'machine-worktable', 'machine-feeder']:
    candidates = list(screenshots.glob('*fossil-' + name + '.png'))
    if candidates:
        screenshot = max(candidates, key=lambda path: path.stat().st_mtime)
        shutil.copyfile(screenshot, DEST / ('fossil-' + name + '.png'))
archive = DEST.with_name(DEST.name + '.zip')
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as output:
    for entry in sorted(DEST.rglob('*')):
        if entry.is_file():
            output.write(entry, str(Path(DEST.name) / entry.relative_to(DEST)))
print('Development kit:', archive)
print('Runtime jars:', len(manifest['files']))
