#!/usr/bin/env python3
"""Compile the actual model and run regression tests without Android or third-party libraries."""
import os
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
jdk = Path(os.environ.get('JAVA_HOME') or '/Applications/Android Studio.app/Contents/jbr/Contents/Home')
source = root / 'app/src/main/java/edu/hitsz'
output = root / 'app/build/world-tests'
output.mkdir(parents=True, exist_ok=True)
files = []
for folder in ['aircraft', 'basic', 'bullet', 'factory', 'prop', 'observer', 'strategy']:
    files.extend((source / folder).glob('*.java'))
for name in ['GameConfig', 'Sprite', 'Viewport', 'TouchState', 'GameWorld']:
    files.append(source / 'application' / (name + '.java'))
files.extend((root / 'tests').rglob('*.java'))
subprocess.run([str(jdk / 'bin/javac'), '--release', '11', '-encoding', 'UTF-8', '-d', str(output)]
               + [str(file) for file in sorted(files)], check=True)
subprocess.run([str(jdk / 'bin/java'), '-ea', '-cp', str(output),
                'edu.hitsz.application.GameWorldRegressionTest'], check=True)
