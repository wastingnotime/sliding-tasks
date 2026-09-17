"""Test bootstrap for WNT-owned user-space runtime infrastructure."""

from pathlib import Path
import sys


WNT_MRL_RUNTIME = Path.home() / ".wnt" / "runtime" / "mrl"
if WNT_MRL_RUNTIME.is_dir():
    sys.path.insert(0, str(WNT_MRL_RUNTIME))

