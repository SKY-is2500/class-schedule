"""Small adb UI-tree helper for task QA. Coordinates always come from current nodes."""
from pathlib import Path
import subprocess,sys,re,xml.etree.ElementTree as ET,time
sys.stdout.reconfigure(encoding='utf-8')
ROOT=Path(__file__).resolve().parent
ADB=r'E:\Class schedule\.android-sdk\platform-tools\adb.exe'
SERIAL='emulator-5554'
def adb(*args):
    p=subprocess.run([ADB,'-s',SERIAL,*args],capture_output=True)
    if p.returncode: raise RuntimeError(p.stderr.decode(errors='replace'))
    return p.stdout
def dump(name='current'):
    adb('shell','uiautomator','dump','/sdcard/window.xml')
    content=adb('shell','cat','/sdcard/window.xml')
    (ROOT/f'{name}.xml').write_bytes(content)
    return ET.fromstring(content)
def capture(name):
    tree=dump(name)
    (ROOT/f'{name}.png').write_bytes(adb('exec-out','screencap','-p'))
    for n in tree.iter('node'):
        text=n.get('text') or n.get('content-desc')
        if text: print(text,n.get('bounds'))
def tap(label):
    tree=dump()
    nodes=[n for n in tree.iter('node') if n.get('text')==label or n.get('content-desc')==label]
    if len(nodes)!=1: raise ValueError(f'Expected one node {label!r}; found {len(nodes)}')
    values=list(map(int,re.findall(r'\d+',nodes[0].get('bounds'))))
    x1,y1,x2,y2=values
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
    print('Tapped',label)
def swipe():
    tree=dump()
    node=next(n for n in tree.iter('node') if n.get('scrollable')=='true')
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    x=(x1+x2)//2
    adb('shell','input','swipe',str(x),str(int(y1+(y2-y1)*.8)),str(x),str(int(y1+(y2-y1)*.25)),'400')
if __name__=='__main__':
    {'capture':capture,'tap':tap,'swipe':lambda _:swipe()}[sys.argv[1]](sys.argv[2] if len(sys.argv)>2 else '')
