"""Original 32px handbook, wrench, data-card and template artwork. Python + Pillow."""
from PIL import Image,ImageDraw,ImageFont
from pathlib import Path
import json,math
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/prscraft';out=root/'textures/item';out.mkdir(exist_ok=True);font=ImageFont.load_default(size=8)
def save(name,im,parent='minecraft:item/generated'):
 im.save(out/(name+'.png'));(root/f'models/item/{name}.json').write_text(json.dumps({'parent':parent,'textures':{'layer0':'prscraft:item/'+name}},indent=2)+'\n')
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.polygon([(4,2),(25,2),(29,6),(29,29),(7,30),(3,26),(3,5)],fill='#102d3c');d.rectangle((6,4,26,27),fill='#26718b',outline='#7eb7bd');d.rectangle((4,4,7,27),fill='#174756');d.line((9,6,24,6),fill='#bfe9db');d.rectangle((9,9,24,22),fill='#102f42',outline='#598e9b');d.line([(10,17),(12,17),(13,12),(15,20),(17,12),(19,18),(22,17),(23,17)],fill='#9bf1c5',width=1);d.rectangle((9,25,24,27),fill='#d5d4b4');d.line((8,29,28,29),fill='#e4dfca');d.rectangle((22,26,24,31),fill='#e7a66a');save('handbook',im)
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.polygon([(5,25),(17,12),(15,7),(18,2),(23,1),(21,6),(25,10),(30,8),(29,14),(24,17),(20,16),(9,29),(6,29)],fill='#243947');d.polygon([(6,25),(19,12),(17,7),(19,3),(22,2),(20,7),(25,12),(29,10),(28,13),(24,15),(20,14),(8,28),(6,28)],fill='#b4cbd0');d.line([(8,25),(19,13)],fill='#edf5e3',width=1);d.line([(6,26),(14,18)],fill='#268a8a',width=4);d.line([(7,25),(13,19)],fill='#70c8b6');d.point((6,27),fill='#e7dfba');save('cable_wrench',im,'minecraft:item/handheld')
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.polygon([(6,2),(23,2),(28,7),(28,30),(4,30),(4,4)],fill='#1f3948');d.polygon([(7,4),(22,4),(26,8),(26,28),(6,28),(6,5)],fill='#247e7b');d.rectangle((9,3,21,7),fill='#d0bc76');d.rectangle((9,11,22,23),fill='#102b37',outline='#aac6c3');d.rectangle((12,14,19,20),fill='#465b64');
for x in range(11,22,3):d.line((x,9,x,11),fill='#e0b56e');d.line((x,23,x,26),fill='#e0b56e')
for y in range(13,23,3):d.line((7,y,9,y),fill='#c7ddcf');d.line((22,y,25,y),fill='#c7ddcf')
d.point((24,26),fill='#a6e494');save('data_card',im)
profiles=[('BLANK','#8b999a'),('TX','#40af84'),('RX','#468ac4'),('AMP','#cf944e'),('FLT','#4baac4'),('MOD','#9f72bf'),('BIT','#ca7864'),('SUM','#bea44f'),('INT','#c286aa'),('COR','#57a880'),('GATE','#bf6e73'),('DLY','#6d8ac3'),('GRAY','#4ea999')]
for i,(label,accent) in enumerate(profiles):
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.polygon([(4,1),(23,1),(28,6),(28,30),(4,30)],fill='#647f82');d.polygon([(5,2),(22,2),(27,7),(27,29),(5,29)],fill='#e5e7d5');d.rectangle((5,2,22,8),fill=accent);d.polygon([(22,2),(27,7),(22,7)],fill='#b1c8c0');d.text((6,2),label,font=ImageFont.load_default(size=6) if len(label)>3 else font,fill='#112f3c')
 if i:
  d.rectangle((8,12,12,16),fill=accent,outline='#456571');d.rectangle((21,21,25,25),fill=accent,outline='#456571');d.line([(12,14),(19,14),(19,23),(21,23)],fill='#456571');d.ellipse((17,12,20,15),outline='#456571')
  for x in range(8,25):d.point((x,22-round(math.sin((x-8)*.7+i)*3)),fill='#6a8a84')
 else:
  for y in [13,18,23]:d.line((8,y,23,y),fill='#a1b9b2')
 for y in [11,16,21,26]:d.rectangle((3,y,5,y+1),fill='#96a6a2')
 save('template_'+str(i),im)
model={'parent':'prscraft:item/template_0','overrides':[{'predicate':{'prscraft:profile':i},'model':f'prscraft:item/template_{i}'} for i in range(1,13)]};(root/'models/item/signal_template.json').write_text(json.dumps(model,indent=2)+'\n')

# Original handheld line analyzer: protected case, CRT trace, rotary dial and probe.
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.rectangle((14,0,17,6),fill='#7b979d');d.rectangle((15,0,16,2),fill='#d9e4d6');d.line([(17,3),(24,3),(28,7),(28,18)],fill='#294b5c',width=2)
d.rounded_rectangle((4,5,26,31),radius=3,fill='#112c3b');d.rounded_rectangle((6,6,24,29),radius=2,fill='#467b8c',outline='#94bac0');d.rectangle((8,9,22,19),fill='#0b2430',outline='#b1d6c5');d.line((9,14,21,14),fill='#29495b');d.line([(9,16),(11,15),(12,11),(14,17),(16,12),(18,15),(21,13)],fill='#7de0ad')
d.text((9,6),'LINE',font=ImageFont.load_default(size=5),fill='#e4e9ce');d.ellipse((13,22,21,29),fill='#182e3b',outline='#c2b882');d.line((17,25,19,23),fill='#e9d486',width=1);d.rectangle((8,22,10,24),fill='#e58e73');d.rectangle((8,26,10,28),fill='#8bd0ac');d.line((5,10,5,26),fill='#284e60');d.line((25,10,25,26),fill='#284e60');save('line_analyzer',im)
