"""Generate 2.0 geometry, texture patterns, item models, recipes and loot. Requires Pillow and a Minecraft client JAR."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image,ImageDraw
import json,copy,io,sys
R=Path(__file__).resolve().parents[1]/'src/main/resources';A=R/'assets/lilybloom';J=ZipFile(sys.argv[1])
SHAPES=['round','oval','notched','curled'];PATTERNS=['plain','speckled','striped'];STAGES=['bud','half_open','bloom']
COLORS=['','white_','yellow_','blue_']
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,separators=(',',':'))+'\n')
def model(name):
 if ':' not in name:name='minecraft:'+name
 ns,path=name.split(':');p='assets/'+ns+'/models/'+path+'.json'
 return json.loads(J.read(p)) if ns=='minecraft' else json.loads((R/p).read_text())
def resolve(name):
 d=model(name)
 if 'parent' in d:
  try:base=resolve(d['parent'])
  except (KeyError,FileNotFoundError):base={}
  textures=base.get('textures',{})|d.get('textures',{});base.update(d);base['textures']=textures;d=base
 d.pop('parent',None);return d
def texture(path,pack=A):
 p=pack/'textures'/Path(path.split(':')[1]+'.png')
 if p.exists():return Image.open(p).convert('RGBA')
 base=A/'textures'/Path(path.split(':')[1]+'.png')
 if base.exists():return Image.open(base).convert('RGBA')
 im=Image.open(io.BytesIO(J.read('assets/'+path.split(':')[0]+'/textures/'+path.split(':')[1]+'.png'))).convert('RGBA')
 if pack!=A:
  target=64 if 'hd_64x' in str(pack) else 32
  im=im.resize((target,target),Image.Resampling.NEAREST)
 return im
def save_texture(im,path,pack):
 p=pack/'textures'/Path(path+'.png');p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
 if path.startswith('block/'):
  strategy='cutout' if 'pad' in path and 'flower' not in path else 'strict_cutout'
  write(p.with_suffix('.png.mcmeta'),{'texture':{'mipmap_strategy':strategy}})
def shape_texture(im,shape):
 out=im.copy();w,h=im.size
 if shape=='oval':
  out=Image.new('RGBA',im.size);resized=im.resize((int(w*.72),h),Image.Resampling.NEAREST);out.paste(resized,((w-resized.width)//2,0))
 if shape=='notched':
  pixels=out.load()
  for y in range(h):
   for x in range(w):
    if y>h*.45 and abs(x-w*.5)<(y-h*.45)*.48:pixels[x,y]=(0,0,0,0)
 return out
def patterned(im,pattern):
 out=im.copy();p=out.load();scale=max(1,out.width//16)
 for y in range(out.height):
  for x in range(out.width):
   r,g,b,a=p[x,y]
   if a<128 or (g>r*1.08 and g>b*1.15):continue
   px,py=x//scale,y//scale
   selected=(px*3+py*5)%13==0 if pattern=='speckled' else (px+py//3)%5==0
   if selected:p[x,y]=(112,52,139,a) if min(r,g,b)>160 else (255,240,177,a)
 return out
PACKS=[A,R/'resourcepacks/hd_32x/assets/lilybloom',R/'resourcepacks/hd_64x/assets/lilybloom']
def variant_textures(d,pattern):
 for key,t in list(d.get('textures',{}).items()):
  if key!='flower' or t.startswith('#') or pattern=='plain':continue
  stem='block/v2/'+Path(t.split(':')[1]).name+'_'+pattern
  for pack in PACKS:save_texture(patterned(texture(t,pack),pattern),stem,pack)
  d['textures'][key]='lilybloom:'+stem
def variant(name,shape='round',pattern='plain',glow=False,part=None,size=1):
 d=resolve('lilybloom:block/'+name);variant_textures(d,pattern)
 for e in d.get('elements',[]):
  leaf=any(f.get('texture')=='#pad' for f in e.get('faces',{}).values())
  if leaf:
   if size==1:
    if shape=='oval':
     for key in ['from','to']:e[key][0]=8+(e[key][0]-8)*.72
    if shape=='curled':
     # Four short raised rims, appended separately below.
     pass
   else:
    if shape=='curled':
     for key in ['from','to']:e[key][1]=.25+(e[key][1]-.25)*1.65
  elif glow:e['light_emission']=15
 if shape=='notched' or size>1 and shape=='oval':
  original=d['textures']['pad'];stem='block/v2/'+Path(original.split(':')[1]).name+'_'+shape
  for pack in PACKS:save_texture(shape_texture(texture(original,pack),shape),stem,pack)
  d['textures']['pad']='lilybloom:'+stem
 if size>1 and shape=='oval' and part is not None:
  # Match the raised rim to the elliptical leaf, keeping each part in its own block.
  for e in d['elements'][1:]:
   for key in ['from','to']:
    world=e[key][0]+part[0]*16;e[key][0]=(world-size*8)*.72+size*8-part[0]*16
   if 'rotation' in e:
    world=e['rotation']['origin'][0]+part[0]*16;e['rotation']['origin'][0]=(world-size*8)*.72+size*8-part[0]*16
 if size>1 and shape=='notched':d['elements']=d['elements'][:1]
 if size==1 and shape=='curled':
  for box in [([1,.25,3],[2,2.25,13]),([14,.25,3],[15,2.25,13]),([3,.25,1],[13,1.75,2])]:
   d['elements'].append({'from':box[0],'to':box[1],'faces':{face:{'texture':'#pad','tintindex':0} for face in ['up','north','south','east','west']}})
 return d
def vname(name,shape,pattern,glow):return 'v2/'+name+'_'+shape+'_'+pattern+('_glow' if glow else '')
def apply_model(name):return {'model':'lilybloom:block/'+name}
def select(prop,cases,fallback):return {'type':'minecraft:select','property':'minecraft:block_state','block_state_property':prop,'cases':[{'when':k,'model':v} for k,v in cases.items()],'fallback':fallback}
for color in COLORS:
 block=color+'blooming_lily_pad';parts=[];item_cases={}
 for stage in STAGES:
  base=block+('' if stage=='bloom' else '_'+stage)
  shape_cases={}
  for shape in SHAPES:
   pattern_cases={}
   for pattern in PATTERNS:
    glow_cases={}
    for glow in [False,True]:
     name=vname(base,shape,pattern,glow);write(A/'models/block'/Path(name+'.json'),variant(base,shape,pattern,glow))
     parts.append({'when':{'has_flower':'true','stage':stage,'leaf_shape':shape,'pattern':pattern,'glowing':str(glow).lower()},'apply':apply_model(name)})
     glow_cases[str(glow).lower()]={'type':'minecraft:model','model':'lilybloom:block/'+name,'tints':[{'type':'minecraft:constant','value':-9321636}]}
    pattern_cases[pattern]=select('glowing',glow_cases,glow_cases['false'])
   shape_cases[shape]=select('pattern',pattern_cases,pattern_cases['plain'])
  item_cases[stage]=select('leaf_shape',shape_cases,shape_cases['round'])
 bare_cases={}
 for shape in SHAPES:
  d=variant(block,shape);d['elements']=[e for e in d['elements'] if any(f.get('texture')=='#pad' for f in e.get('faces',{}).values())]
  name='v2/'+block+'_bare_'+shape;write(A/'models/block'/Path(name+'.json'),d)
  parts.append({'when':{'has_flower':'false','leaf_shape':shape},'apply':apply_model(name)})
  bare_cases[shape]={'type':'minecraft:model','model':'lilybloom:block/'+name,'tints':[{'type':'minecraft:constant','value':-9321636}]}
 write(A/'blockstates'/Path(block+'.json'),{'multipart':parts})
 bloom_item=select('stage',item_cases,item_cases['bloom'])
 write(A/'items'/Path(block+'.json'),{'model':select('has_flower',{'false':select('leaf_shape',bare_cases,bare_cases['round']),'true':bloom_item},bloom_item)})
 # An ordinary break retains traits and growth stage on small flowering pads.
 write(R/'data/lilybloom/loot_table/blocks'/Path(block+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'condition':{'type':'minecraft:survives_explosion'},'entries':[{'type':'minecraft:item','name':'lilybloom:'+block,'modifier':[{'type':'minecraft:copy_state','block':'lilybloom:'+block,'properties':['stage','has_flower','nocturnal','glowing','leaf_shape','pattern']}]}]}]})
for kind,size in [('medium',2),('large',3),('giant',4)]:
 block=kind+'_lily_pad';parts=[];anchor=(size-1)//2;offset=(size/2-anchor)*16-8
 for x in range(size):
  for z in range(size):
   base=block+'_'+str(x)+'_'+str(z)
   for shape in SHAPES:
    name='v2/'+base+'_'+shape;write(A/'models/block'/Path(name+'.json'),variant(base,shape=shape,part=(x,z),size=size))
    parts.append({'when':{'part_x':str(x),'part_z':str(z),'leaf_shape':shape},'apply':apply_model(name)})
 for color in COLORS:
  for stage in STAGES:
   base=color+kind+'_lily_flower'+('' if stage=='bloom' else '_'+stage)
   for pattern in PATTERNS:
    for glow in [False,True]:
     name=vname(base,'round',pattern,glow);write(A/'models/block'/Path(name+'.json'),variant(base,pattern=pattern,glow=glow))
     parts.append({'when':{'part_x':str(anchor),'part_z':str(anchor),'flower':color.rstrip('_') or 'pink','stage':stage,'pattern':pattern,'glowing':str(glow).lower()},'apply':apply_model(name)})
 for decoration,base in [('candle','candle_one_candle_lit'),('lantern','lantern'),('soul_lantern','soul_lantern')]:
  d=resolve('minecraft:block/'+base)
  for e in d['elements']:
   for key in ['from','to']:
    e[key][0]+=offset;e[key][2]+=offset;e[key][1]+=.25
   if 'rotation' in e:
    e['rotation']['origin'][0]+=offset;e['rotation']['origin'][2]+=offset;e['rotation']['origin'][1]+=.25
  name='v2/'+kind+'_'+decoration;write(A/'models/block'/Path(name+'.json'),d)
  parts.append({'when':{'part_x':str(anchor),'part_z':str(anchor),'decoration':decoration},'apply':apply_model(name)})
 write(A/'blockstates'/Path(block+'.json'),{'multipart':parts})
 loot_path=R/'data/lilybloom/loot_table/blocks'/Path(block+'.json');loot=json.loads(loot_path.read_text())
 loot['pools'][0]['entries'][0]['modifier']=[{'type':'minecraft:copy_state','block':'lilybloom:'+block,'properties':['nocturnal','glowing','leaf_shape','pattern']}]
 # Rebuilt from the original two pools on every run; no duplicate drops.
 loot['pools']=loot['pools'][:2]
 for decoration in ['candle','lantern','soul_lantern']:
  loot['pools'].append({'rolls':1,'condition':{'type':'minecraft:all_of','terms':[{'type':'minecraft:survives_explosion'},{'type':'minecraft:match_block','blocks':'lilybloom:'+block,'state':{'decoration':decoration}}]},'entries':[{'type':'minecraft:item','name':'minecraft:'+decoration}]})
 write(loot_path,loot)
 # The inventory silhouette follows leaf shape; the flower/stage is shown after placing.
 base=json.loads((A/'models/item'/Path(block+'.json')).read_text());cases={}
 for shape in SHAPES:
  d=copy.deepcopy(base);t=d['textures']['layer0'];stem='item/v2/'+kind+'_pad_'+shape
  for pack in PACKS:save_texture(shape_texture(texture(t,pack),shape),stem,pack)
  d['textures']['layer0']='lilybloom:'+stem;name='v2/'+block+'_'+shape;write(A/'models/item'/Path(name+'.json'),d)
  cases[shape]={'type':'minecraft:model','model':'lilybloom:item/'+name}
 write(A/'items'/Path(block+'.json'),{'model':select('leaf_shape',cases,cases['round'])})
# Original minimal pixel art for two new tools/materials.
for name in ['lily_transplanter','lunar_pollen']:
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 if name=='lily_transplanter':
  d.line((3,14,10,7),fill='#573b2b',width=3);d.line((3,13,9,7),fill='#a66e39',width=1)
  d.polygon([(8,6),(11,2),(14,2),(14,6),(11,9)],fill='#b76c43');d.line((10,6,13,3),fill='#f6c096',width=1)
 else:
  d.rectangle((4,5,11,13),fill='#674d83');d.rectangle((5,6,10,12),fill='#b49bd5');d.rectangle((6,2,9,4),fill='#977348')
  d.point((6,8),fill='#fff0ac');d.point((9,10),fill='#fff0ac');d.point((7,11),fill='#fff0ac');d.line((1,2,1,4),fill='#e8d589');d.point((2,3),fill='#e8d589')
 save_texture(im,'item/'+name,A)
 write(A/'models/item'/Path(name+'.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'lilybloom:item/'+name}})
 write(A/'items'/Path(name+'.json'),{'model':{'type':'minecraft:model','model':'lilybloom:item/'+name}})
write(R/'data/lilybloom/recipe/lily_transplanter.json',{'type':'minecraft:crafting_shaped','category':'equipment','pattern':[' C','S '],'key':{'C':'minecraft:copper_ingot','S':'minecraft:stick'},'result':{'id':'lilybloom:lily_transplanter','count':1}})
write(R/'data/lilybloom/recipe/lunar_pollen.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:amethyst_shard','lilybloom:blue_lily_flower'],'result':{'id':'lilybloom:lunar_pollen','count':4}})
for locale,entries in {
 'en_us':{'item.lilybloom.lily_transplanter':'Lily Transplanter','item.lilybloom.lunar_pollen':'Lunar Pollen'},
 'ru_ru':{'item.lilybloom.lily_transplanter':'Лопатка для пересадки кувшинок','item.lilybloom.lunar_pollen':'Лунная пыльца'}}.items():
 p=A/'lang'/Path(locale+'.json');d=json.loads(p.read_text());d.update(entries);write(p,d)
print('Generated Lily Bloom 2.0 assets (four leaf shapes, two rare patterns, emissive flowers, three floating lights).')
