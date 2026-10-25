"""Original PRSCraft pixel art, drawn from geometric primitives; no borrowed textures.
Run with Python 3 + Pillow to reproduce the shipped PNG assets.
"""
from PIL import Image, ImageDraw
from pathlib import Path
import random,json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/prscraft'
for folder in ['textures/item','textures/block']: (root/folder).mkdir(parents=True,exist_ok=True)
colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
rgb=[0xf5f3e8,0xeb9748,0xd079d8,0x82cde9,0xedda63,0xa6d666,0xedacc5,0x70777d,0xb9c0c2,0x58b8ba,0xa787d5,0x728cdd,0xac8161,0x7baf73,0xdf7772,0x22292c]
def color(v): return ((v>>16)&255,(v>>8)&255,v&255)
for name,c in zip(colors,rgb):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);r,g,b=color(c)
 d.polygon([(3,11),(9,2),(12,2),(14,5),(7,14),(4,14)],fill=(max(0,r-35),max(0,g-35),max(0,b-35),255))
 d.polygon([(4,11),(10,3),(12,3),(12,5),(6,13)],fill=(r,g,b,255))
 d.line([(5,10),(10,4)],fill=(min(255,r+38),min(255,g+38),min(255,b+38),255))
 d.line([(4,12),(6,13)],fill=(min(255,r+16),min(255,g+16),min(255,b+16),255));d.point((3,14),fill=(r,g,b,190));d.point((1,14),fill=(r,g,b,100))
 im.save(root/f'textures/item/{name}_chalk.png')
for wear in range(5):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 d.polygon([(1,6),(10,3),(15,6),(15,11),(6,14),(1,11)],fill='#41382e')
 d.polygon([(1,6),(10,3),(15,6),(6,9)],fill='#b58351');d.line([(2,6),(10,4),(13,6)],fill='#d7ad72')
 # Felt progressively loses its saturated turquoise; empty eraser is gray.
 factor=wear/4;felt=tuple(round(a+(b-a)*factor) for a,b in zip((57,163,139),(102,106,99)))
 d.polygon([(2,8),(6,10),(14,7),(14,11),(6,14),(2,11)],fill=felt)
 for x,y in [(3,10),(6,12),(9,11),(12,10)]:d.point((x,y),fill=tuple(min(255,v+24) for v in felt))
 im.save(root/f'textures/item/board_eraser_{wear}.png')
im=Image.new('RGB',(16,16));r=random.Random(16)
for y in range(16):
 for x in range(16):
  v=r.randrange(-8,9);im.putpixel((x,y),(max(0,112+v+(y%4)*4),max(0,76+v),max(0,43+v)))
im.save(root/'textures/block/board_frame.png')
im=Image.new('RGB',(16,16));r=random.Random(128)
for y in range(16):
 for x in range(16):
  v=r.randrange(-3,4);im.putpixel((x,y),(32+v,55+v,47+v))
im.save(root/'textures/block/board_back.png')
Image.new('RGB',(1,1),(255,255,255)).save(root/'textures/block/chalk_surface.png')
# A readable original item sprite for the board (its placed model has a 3D frame).
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);d.rectangle((0,1,15,14),fill='#76522f');d.rectangle((1,2,14,13),fill='#c39561');d.rectangle((2,3,13,12),fill='#20372f');d.line([(3,9),(4,9),(4,6),(6,6),(6,9),(8,9),(8,6),(10,6),(10,9),(12,9)],fill='#e3eddf');im.save(root/'textures/item/drawing_board.png')
