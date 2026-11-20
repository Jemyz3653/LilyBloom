"""Local verification build using an already downloaded official runtime. Gradle remains the normal public build."""
from pathlib import Path
import subprocess,zipfile,json,sys
R=Path(__file__).resolve().parents[1];RT=Path(sys.argv[1]).resolve()
JDK=next(p for p in RT.parent.glob('jdk-*') if p.is_dir())
OUT=R/'build/manual/classes';OUT.mkdir(parents=True,exist_ok=True)
cp=str(R/'build/manual/minecraft-compile.jar')+':'+':'.join(str(p) for p in list((RT/'libraries').rglob('*.jar'))+list((RT/'fabric-modules').glob('*.jar')))
p=subprocess.run([str(JDK/'bin/javac'),'--release','25','-proc:none','-cp',cp,'-d',str(OUT),*[str(p) for p in (R/'src/main/java').rglob('*.java')]],capture_output=True,text=True)
(R/'build/manual/compile.log').write_text(p.stdout+p.stderr)
if p.returncode:print(p.stderr);sys.exit(p.returncode)
dest=R/'build/libs/lilybloom-2.0.0+26.3.jar';dest.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED) as z:
 for path in OUT.rglob('*.class'):z.write(path,path.relative_to(OUT).as_posix())
 root=R/'src/main/resources'
 for path in root.rglob('*'):
  if not path.is_file():continue
  name=path.relative_to(root).as_posix();raw=path.read_bytes()
  if name=='fabric.mod.json':raw=raw.replace(b'${version}',b'2.0.0')
  z.writestr(name,raw)
 z.write(R/'LICENSE','LICENSE_LilyBloom')
print(dest,dest.stat().st_size)
