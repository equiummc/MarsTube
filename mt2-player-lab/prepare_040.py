"""Prepare the exact production patch chain in a disposable local source copy."""
from pathlib import Path
import re
import subprocess
import sys

here = Path(__file__).resolve().parent
root = Path(sys.argv[1]).resolve()
build = (here.parent / 'tools/build-marstube-production-039-arm32.sh').read_text()
patches = list(dict.fromkeys(re.findall(r'patch_[\w]+\.py', build)))
settings = root / 'settings.gradle'
settings.write_text(settings.read_text().replace("'../SharedModules'", "'./SharedModules'")
                    .replace("'../MediaServiceCore'", "'./MediaServiceCore'"))
for patch in patches:
    subprocess.run([sys.executable, str(here / patch), str(root)], check=True)
print('PRODUCTION 039 PATCH CHAIN OK:', len(patches))
