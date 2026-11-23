#!/usr/bin/env python3
"""Build a real debug APK using installed Android SDK tools, without Maven/Gradle downloads."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or Path.home() / 'Library/Android/sdk')
JDK = Path(os.environ.get('JAVA_HOME') or '/Applications/Android Studio.app/Contents/jbr/Contents/Home')
ENV = dict(os.environ, JAVA_HOME=str(JDK), PATH=str(JDK / 'bin') + os.pathsep + os.environ.get('PATH', ''))
TOOLS = SDK / 'build-tools/36.0.0'
PLATFORM = SDK / 'platforms/android-37.0/android.jar'
NS = 'http://schemas.android.com/apk/res/android'

def run(args):
    subprocess.run([str(x) for x in args], cwd=ROOT, env=ENV, check=True)

def build(test=False):
    output = ROOT / 'app/build/offline' / ('androidTest' if test else 'debug')
    if output.exists():
        shutil.rmtree(output)
    for folder in ['generated', 'classes', 'dex']:
        (output / folder).mkdir(parents=True)
    if test:
        manifest = ROOT / 'app/src/androidTest/AndroidManifest.xml'
        java_sources = list((ROOT / 'app/src/androidTest/java').rglob('*.java'))
        classpath = str(PLATFORM) + os.pathsep + str(ROOT / 'app/build/offline/debug/classes')
    else:
        # Gradle supplies package/debuggable; direct SDK linking needs them explicitly.
        manifest = output / 'AndroidManifest.xml'
        ET.register_namespace('android', NS)
        tree = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml')
        tree.getroot().set('package', 'edu.hitsz.aircraftwar')
        tree.getroot().find('application').set('{' + NS + '}debuggable', 'true')
        tree.write(manifest, encoding='utf-8', xml_declaration=True)
        run([TOOLS / 'aapt2', 'compile', '--dir', ROOT / 'app/src/main/res', '-o', output / 'compiled.zip'])
        java_sources = list((ROOT / 'app/src/main/java').rglob('*.java'))
        classpath = str(PLATFORM)
    link = [TOOLS / 'aapt2', 'link', '-I', PLATFORM, '--manifest', manifest,
            '--java', output / 'generated', '--min-sdk-version', '30', '--target-sdk-version', '37',
            '--version-code', '1', '--version-name', '1.0', '-o', output / 'resources.apk']
    if not test:
        link.extend([output / 'compiled.zip'])
    run(link)
    java_sources.extend((output / 'generated').rglob('*.java'))
    run([JDK / 'bin/javac', '--release', '11', '-encoding', 'UTF-8', '-classpath', classpath,
         '-d', output / 'classes'] + sorted(java_sources))
    run([JDK / 'bin/jar', '--create', '--file', output / 'classes.jar', '-C', output / 'classes', '.'])
    dex = [TOOLS / 'd8', '--lib', PLATFORM, '--min-api', '30', '--output', output / 'dex']
    if test:
        dex.extend(['--classpath', ROOT / 'app/build/offline/debug/classes.jar'])
    run(dex + [output / 'classes.jar'])
    unsigned = output / 'unsigned.apk'
    shutil.copyfile(output / 'resources.apk', unsigned)
    with zipfile.ZipFile(unsigned, 'a', compression=zipfile.ZIP_DEFLATED) as package:
        for path in sorted((output / 'dex').glob('*.dex')):
            package.write(path, path.name)
    run([TOOLS / 'zipalign', '-f', '4', unsigned, output / 'aligned.apk'])
    keystore = ROOT / '.local/debug.keystore'
    keystore.parent.mkdir(exist_ok=True)
    if not keystore.exists():
        run([JDK / 'bin/keytool', '-genkeypair', '-keystore', keystore, '-storepass', 'android',
             '-keypass', 'android', '-alias', 'androiddebugkey', '-keyalg', 'RSA', '-keysize', '2048',
             '-validity', '10000', '-dname', 'CN=Android Debug,O=Android,C=US'])
    apk = output / ('app-debug-androidTest.apk' if test else 'app-debug.apk')
    run([TOOLS / 'apksigner', 'sign', '--ks', keystore, '--ks-pass', 'pass:android', '--key-pass',
         'pass:android', '--out', apk, output / 'aligned.apk'])
    run([TOOLS / 'apksigner', 'verify', '--verbose', apk])
    print('APK: ' + str(apk))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--test', action='store_true')
    args = parser.parse_args()
    build(args.test)
