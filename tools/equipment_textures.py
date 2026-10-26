"""Original PRSCraft 32px electronics artwork. Reproduce with Python 3 + Pillow.
Generates textures and binds every existing block model to mod-owned resources.
"""
from PIL import Image,ImageDraw,ImageFont
from pathlib import Path
import json,math,random
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/prscraft'
out=root/'textures/block';out.mkdir(parents=True,exist_ok=True)
font=ImageFont.load_default(size=8)
def save(im,name):im.save(out/(name+'.png'))
def panel(base=(37,49,62),seed=0):
 im=Image.new('RGB',(32,32));r=random.Random(seed)
 for y in range(32):
  for x in range(32):
   v=r.randrange(-3,4)+(y%4==0)*3;im.putpixel((x,y),tuple(max(0,min(255,c+v)) for c in base))
 d=ImageDraw.Draw(im);d.rectangle((0,0,31,31),outline='#121d28');d.line((1,1,30,1),fill='#8293a1');d.line((1,1,1,30),fill='#657785')
 for x,y in [(3,3),(28,3),(3,28),(28,28)]:d.rectangle((x-1,y-1,x+1,y+1),fill='#1c2832');d.line((x-1,y,x+1,y),fill='#a4b0b5')
 return im
save(panel((123,139,146),1),'instrument_case')
im=panel((47,59,69),2);d=ImageDraw.Draw(im)
for y in range(7,27,4):d.line((8,y,24,y),fill='#17232d');d.line((8,y+1,24,y+1),fill='#617483')
save(im,'instrument_vents')
for name,base in [('socket_dark',(30,40,48)),('socket_a',(183,104,47)),('socket_b',(53,122,177)),('socket_out',(65,156,98))]:
 im=panel(base);d=ImageDraw.Draw(im);d.rectangle((8,8,23,23),outline='#17232d',width=2);d.rectangle((11,11,20,20),outline='#b8c9cf');d.rectangle((14,14,17,17),fill='#17232d');save(im,name)
units={'digital_modem':('IQ','#9ae3ed'),'packet_sender':('FRM','#edbb7b'),'packet_receiver':('ARQ','#92dbab'),'orthogonality_meter':('ORTH','#dbb0e8'),'simulator':('SIM','#a0d8c0'),'message_source':('TXT','#7be0ba'),'message_codec':('ZIP','#e8cc78'),'channel_coder':('FEC','#c39ae9'),'message_receiver':('RXD','#83c8ea'),'gray_converter':('GRAY','#8be2ce'),'transmitter':('TX','#64dfab'),'bit_generator':('BIT','#ef8c72'),'amplifier':('AMP','#f0af59'),'frequency_filter':('FLT','#75cfea'),'modulator':('MOD','#c29af3'),'adder':('SUM','#edcf64'),'integrator':('INT','#eea4ca'),'correlator':('COR','#8ddca2'),'time_gate':('GATE','#e98887'),'delay_line':('DLY','#91a9eb')}
for name,(label,accent) in units.items():
 im=panel((32,44,54));d=ImageDraw.Draw(im);d.rectangle((5,5,26,23),fill='#101f29',outline='#607d8a');d.line((6,15,25,15),fill='#28404c');d.line((15,6,15,22),fill='#28404c')
 points=[]
 for x in range(6,26):
  t=(x-6)/19
  if name in ['transmitter','modulator']:v=math.sin(t*math.pi*(4 if name=='transmitter' else 10))
  elif name=='bit_generator':v=1 if (x//4)%2 else -1
  elif name=='amplifier':v=(t-.5)*1.8
  elif name=='frequency_filter':v=math.exp(-((t-.5)/.3)**4)*1.8-1
  elif name=='adder':v=1 if abs(t-.5)<.08 else 0
  elif name=='integrator':v=(t*t)*2-1
  elif name=='correlator':v=2*math.exp(-((t-.5)/.18)**2)-1
  elif name=='time_gate':v=1 if t>.5 else -1
  else:v=1 if .55<t<.85 else -1
  points.append((x,15-round(v*6)))
 d.line(points,fill=accent,width=1)
 if name=='adder':d.line((10,11,20,11),fill=accent);d.line((15,7,15,15),fill=accent)
 d.text((5,24),label,font=font,fill=accent);d.rectangle((24,26,26,28),fill=accent);save(im,name+'_panel')
 top=panel((89,110,123));dt=ImageDraw.Draw(top)
 for y in [7,10,13]:dt.line((6,y,25,y),fill='#233c4c')
 dt.rounded_rectangle((5,18,26,26),radius=1,fill='#142b37',outline=accent);dt.text((7,18),label,font=font,fill=accent);save(top,name+'_top')
for name,base in [('coaxial',(63,67,73)),('fiber',(34,128,144)),('metallic',(170,102,60))]:
 im=Image.new('RGB',(32,32),base);d=ImageDraw.Draw(im)
 for y in range(0,32,4):
  color=tuple(max(0,c-22) for c in base);d.line((0,y,31,y),fill=color);d.line((0,y+1,31,y+1),fill=tuple(min(255,c+18) for c in base))
 for x in [0,15,31]:d.line((x,0,x,31),fill=tuple(max(0,c-28) for c in base))
 if name=='metallic':
  for k in range(-32,64,8):d.line((k,0,k+32,31),fill='#deb67b')
 elif name=='fiber':d.line((0,16,31,16),fill='#8ae9ee')
 else:
  for x in range(4,32,8):d.rectangle((x,14,x+3,17),fill='#b0b2a7')
 save(im,name+'_jacket')
 im=panel(base);d=ImageDraw.Draw(im)
 for box,col in [((5,5,26,26),'#1a2630'),((8,8,23,23),'#a7b6b7'),((11,11,20,20),'#293b48'),((14,14,17,17),'#ead098')]:d.rectangle(box,fill=col)
 save(im,name+'_connector')
# Bind custom surfaces to every model. Do not change block geometry or orientation.
for path in (root/'models/block').glob('*.json'):
 model=json.loads(path.read_text());name=path.stem
 if name=='drawing_board':continue # already uses original PRSCraft frame/back textures
 textures=model.get('textures',{})
 for key,value in list(textures.items()):
  if key in ['case','body','particle']:textures[key]='prscraft:block/instrument_case'
  elif key=='dark':textures[key]='prscraft:block/instrument_vents'
  elif key=='input':textures[key]='prscraft:block/socket_a'
  elif key=='output':textures[key]='prscraft:block/socket_out'
  elif key=='accent':textures[key]='prscraft:block/'+(name+'_panel' if name in units else name.removesuffix('_port')+'_connector')
  elif key=='cable':textures[key]='prscraft:block/'+name.split('_cable')[0]+'_jacket'
 if '_cable' in name:textures['particle']=textures['cable']
 if name.endswith('_port'):textures['dark']='prscraft:block/socket_dark';textures['particle']=textures['accent']
 if name in units:
  textures['top']='prscraft:block/'+name+'_top';textures['panel']='prscraft:block/'+name+'_panel';textures['socket_b']='prscraft:block/socket_b'
  model['elements'][0]['faces']['up']={'texture':'#top','uv':[0,0,16,16]}
  # Full-screen UV makes the instrument symbol readable on its raised front display.
  for element in model.get('elements',[]):
   for face in element.get('faces',{}).values():
    if face['texture']=='#accent':face['uv']=[0,0,16,16]
  if name in ['adder','correlator','orthogonality_meter']:
   for element in model['elements']:
    if element['from'][0]==0 and element['to'][0]==1:
     for face in element['faces'].values():face['texture']='#socket_b'
 if name in units and name not in ['transmitter','bit_generator','message_source','message_receiver','packet_receiver','simulator']:
  if not any(e['from']==[6,0,0] and e['to']==[10,2,1] for e in model['elements']):
   model['elements'].append({'from':[6,0,0],'to':[10,2,1],'faces':{f:{'texture':'#output','uv':[0,0,16,16]} for f in ['north','south','east','west','up','down']}})
 path.write_text(json.dumps(model,indent=2)+'\n')
for medium in ['coaxial','fiber','metallic']:
 path=root/f'models/item/{medium}_cable.json';model=json.loads(path.read_text());model['textures']={'cable':f'prscraft:block/{medium}_jacket','particle':f'prscraft:block/{medium}_jacket'};path.write_text(json.dumps(model,indent=2)+'\n')
