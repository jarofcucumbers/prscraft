package dev.jarofcucumbers.prscraft.client;

import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Locale;

public final class RadioScreen extends Screen {
    private static final int W = 720, H = 494;
    private static final int[] COLORS = {0xff55ddee,0xffff9f43,0xffcd84f1,0xff7bed9f,0xffff6b81,0xfff9ca24,0xff70a1ff,0xffdfe4ea};
    private final BlockPos pos;
    private final boolean recorded;
    private CompoundTag snapshot;
    private RadioSettings saved;
    private EditBox expression, sample, levels, duty, power, carrier, noise, bits, bitRate, oscillator;
    private String signalMode,decodeMode;
    private ProcessorSettings decoder;
    private boolean demodulate;
    private int sentView, viewTicks;
    private Button stageButton,kindButton,physicalButton,roleButton;
    private final java.util.List<Button> termButtons=new java.util.ArrayList<>();
    private Button apply;
    private boolean transmit, enabled, thermal, secondKind;
    private boolean showSum = true, showSample = true, showQuant = true, showRx = true, frozen;
    private final boolean[] showTerm = {true,true,true,true,true,true,true,true};
    private int ticks, pendingTicks;
    private boolean pending;
    private String serverError = "";
    private float scale;
    private int ox, oy;
    public RadioScreen(BlockPos pos, CompoundTag data) {
        super(Component.translatable("screen.prscraft.title")); this.pos = pos; this.snapshot = data; this.recorded=data.getBoolean("recorded");
        saved = RadioSettings.read(data.getCompound("settings"));
        signalMode=saved.signalMode();decoder=ProcessorSettings.read(data.getCompound("processor"));demodulate=decoder.demodulate();decodeMode=demodulate?(java.util.Set.of("unrz","urz").contains(decoder.modulation())?decoder.modulation():"bpsk"):"raw";
        int layers=data.contains("graphLayers")?data.getInt("graphLayers"):15;
        showSum=(layers&1)!=0;showSample=(layers&2)!=0;showQuant=(layers&4)!=0;showRx=(layers&8)!=0;frozen=(layers&16)!=0;
        int terms=data.contains("graphTerms")?data.getInt("graphTerms"):255;for(int i=0;i<8;i++)showTerm[i]=(terms&(1<<i))!=0;
        sentView=viewCode();
        transmit=recorded?false:saved.transmit(); if(recorded)frozen=true; enabled=saved.enabled(); thermal=saved.thermal(); secondKind=saved.secondKind();
    }
    public BlockPos pos() { return pos; }
    public void receive(CompoundTag data) {
        if(recorded)return;
        if (!frozen || pending) snapshot = data;
        saved = RadioSettings.read(data.getCompound("settings"));
        if (pending) { serverError = data.getString("error"); pending=false; pendingTicks=0; }
    }
    private Component tr(String key) { return Component.translatable("screen.prscraft."+key); }
    private EditBox field(int x, int y, int w, String value, String key) {
        EditBox box = new EditBox(font,x,y,w,20,tr(key)); box.setMaxLength(key.equals("expression")?256:16); box.setValue(value);
        box.setResponder(v -> serverError=""); return addRenderableWidget(box);
    }
    private Button button(int x,int y,int w,Component label,Button.OnPress action) {
        return addRenderableWidget(new ScaledButton(x,y,w,label,action));
    }
    private Component toggle(String key, boolean value) { return Component.literal(tr(key).getString()+": "+tr(value?"on":"off").getString()); }
    @Override protected void init() {
        scale = Math.min((width-8f)/W,(height-8f)/H); ox=(int)((width-W*scale)/2); oy=(int)((height-H*scale)/2);
        // Preserve uncommitted edits across GUI resizing.
        String ex=expression==null?saved.expression():expression.getValue();
        String sa=sample==null?input(saved.sampleHz()):sample.getValue(), le=levels==null?""+saved.levels():levels.getValue();
        String du=duty==null?input(saved.duty()):duty.getValue(), po=power==null?input(saved.powerDbm()):power.getValue();
        String ca=carrier==null?input(saved.carrierMHz()):carrier.getValue(), no=noise==null?input(saved.noiseRms()):noise.getValue();
        expression=field(14,43,692,ex,"expression");
        sample=field(14,88,94,sa,"sample"); levels=field(120,88,88,le,"levels"); duty=field(220,88,76,du,"duty");
        power=field(308,88,88,po,"power"); carrier=field(408,88,138,ca,"carrier"); noise=field(558,88,148,no,"noise");
        roleButton=button(14,118,90,Component.literal(transmit?"TX":"RX"),b->{transmit=!transmit;b.setMessage(Component.literal(transmit?"TX":"RX"));serverError="";visibility();});
        button(110,118,100,toggle("enabled",enabled),b->{enabled=!enabled;b.setMessage(toggle("enabled",enabled));serverError="";});
        kindButton=button(216,118,148,Component.literal(tr("sampling").getString()+": "+(secondKind?"II":"I")),b->{secondKind=!secondKind;b.setMessage(Component.literal(tr("sampling").getString()+": "+(secondKind?"II":"I")));serverError="";});
        physicalButton=button(370,118,180,toggle("physical",thermal),b->{thermal=!thermal;b.setMessage(toggle("physical",thermal));serverError="";});
        apply=button(556,118,150,tr("apply"),b->save());
        button(14,216,110,toggle("sum",showSum),b->{showSum=!showSum;b.setMessage(toggle("sum",showSum));});
        button(130,216,125,toggle("sampling",showSample),b->{showSample=!showSample;b.setMessage(toggle("sampling",showSample));});
        button(261,216,130,toggle("quant",showQuant),b->{showQuant=!showQuant;b.setMessage(toggle("quant",showQuant));});
        button(397,216,130,toggle("rx",showRx),b->{showRx=!showRx;b.setMessage(toggle("rx",showRx));});
        button(533,216,173,toggle("freeze",frozen),b->{frozen=!frozen;b.setMessage(toggle("freeze",frozen));});
        String bitText=bits==null?saved.bits():bits.getValue();
        String rateText=bitRate==null?input(saved.transmit()?saved.bitRate():decoder.symbolHz()):bitRate.getValue();
        String oscText=oscillator==null?input(decoder.oscillatorHz()):oscillator.getValue();
        bits=field(156,170,172,bitText,"bits");bits.setMaxLength(128);
        bitRate=field(340,170,80,rateText,"bit_rate");oscillator=field(432,170,84,oscText,"oscillator");
        stageButton=button(14,170,130,Component.empty(),b->{
            if(transmit){String[] modes={"analog","sampled","quantized","bits","discrete"};int i=java.util.Arrays.asList(modes).indexOf(signalMode);signalMode=modes[(i+1)%modes.length];}
            else {String[] modes={"raw","bpsk","unrz","urz"};decodeMode=modes[(java.util.Arrays.asList(modes).indexOf(decodeMode)+1)%modes.length];demodulate=!decodeMode.equals("raw");}
            serverError="";visibility();
        });
        button(528,170,178,tr("media"),b->{flushView();var tag=new CompoundTag();tag.putString("action","media");PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,tag));});
        button(232,4,110,tr("oscilloscope"),b->{flushView();minecraft.setScreen(new PlotScreen(this,snapshot));});
        button(350,4,110,tr("lab_short"),b->{flushView();minecraft.setScreen(snapshot.getBoolean("dataStream")?new MessageScreen(pos,snapshot):new LabScreen(pos,snapshot));});
        button(468,4,110,tr("spectrum"),b->{flushView();minecraft.setScreen(new SpectrumScreen(this,snapshot));});
        button(586,4,120,tr("book"),b->{flushView();minecraft.setScreen(new HandbookScreen(this));});
        termButtons.clear();
        for(int i=0;i<8;i++) {
            final int index=i;
            termButtons.add(button(14+i*87,448,81,Component.literal((showTerm[i]?"● ":"○ ")+"f"+(i+1)).withColor(COLORS[i]&0xffffff),b->{showTerm[index]=!showTerm[index];b.setMessage(Component.literal((showTerm[index]?"● ":"○ ")+"f"+(index+1)).withColor(COLORS[index]&0xffffff));}));
        }
        visibility();
        if(recorded)for(var renderable:renderables)if(renderable instanceof AbstractWidget widget && widget.getY()<216 && !(widget.getY()==4 && widget.getX()>=468))widget.active=false;
    }
    private static String input(double n) { return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString(); }
    private static String fmt(double n) { if(n != 0 && Math.abs(n) < 0.0001) return String.format(Locale.ROOT,"%.2e",n); return String.format(Locale.ROOT,"%.4f",n).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private static double number(EditBox box) { return Double.parseDouble(box.getValue().replace(',','.')); }
    private void visibility(){
        boolean waveform=transmit&&!signalMode.equals("bits")&&!signalMode.equals("discrete");
        boolean sampling=waveform&&!signalMode.equals("analog");
        expression.visible=waveform;sample.visible=sampling||transmit&&signalMode.equals("discrete");levels.visible=sampling&&signalMode.equals("quantized");duty.visible=sampling||transmit&&signalMode.equals("bits")&&saved.lab().lineCode().equals("urz")||!transmit&&decodeMode.equals("urz");
        power.visible=transmit&&saved.lab().quantity().equals("relative");noise.visible=!transmit;kindButton.visible=sampling;physicalButton.visible=!transmit;
        bits.visible=transmit&&signalMode.equals("bits")&&!saved.lab().randomBits();bitRate.visible=transmit&&signalMode.equals("bits")||!transmit&&demodulate;oscillator.visible=!transmit&&decodeMode.equals("bpsk");
        if(snapshot.getString("kind").equals("bit_generator")){roleButton.active=false;stageButton.active=false;}
        stageButton.setMessage(transmit?tr("mode_"+signalMode):tr("decode_"+decodeMode));
    }
    private RadioSettings draft() {
        boolean waveform=transmit&&!signalMode.equals("bits")&&!signalMode.equals("discrete"), sampling=waveform&&!signalMode.equals("analog");
        return new RadioSettings(waveform?expression.getValue():saved.expression(),(sampling||transmit&&signalMode.equals("discrete"))?number(sample):saved.sampleHz(),
            sampling&&signalMode.equals("quantized")?Integer.parseInt(levels.getValue()):saved.levels(),transmit,enabled,thermal,
            transmit?saved.noiseRms():number(noise),number(carrier),transmit?number(power):saved.powerDbm(),secondKind,duty.visible?number(duty):saved.duty(),
            transmit?signalMode:saved.signalMode(),transmit&&signalMode.equals("bits")?bits.getValue():saved.bits(),
            transmit&&signalMode.equals("bits")?number(bitRate):saved.bitRate(),transmit?saved.lab():saved.lab().variance(number(noise)*number(noise)));
    }
    private ProcessorSettings draftDecoder(){
        return new ProcessorSettings(decoder.gainDb(),decoder.lowHz(),decoder.highHz(),!transmit&&demodulate?decodeMode:decoder.modulation(),
            !transmit&&decodeMode.equals("bpsk")?number(oscillator):decoder.oscillatorHz(),!transmit&&demodulate?number(bitRate):decoder.symbolHz(),decoder.depth(),demodulate);
    }
    private int viewCode(){int layers=(showSum?1:0)|(showSample?2:0)|(showQuant?4:0)|(showRx?8:0)|(frozen?16:0),terms=0;
        for(int i=0;i<8;i++)if(showTerm[i])terms|=1<<i;return (layers<<8)|terms;}
    private void view(CompoundTag tag){int code=viewCode();tag.putInt("graphLayers",code>>8);tag.putInt("graphTerms",code&255);}
    private void flushView(){if(!recorded&&sentView!=viewCode()){var tag=new CompoundTag();tag.putString("action","view");view(tag);PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,tag));sentView=viewCode();}}
    @Override public void onClose(){flushView();super.onClose();}
    private void save() {
        try {
            RadioSettings config = draft();
            var tag=config.save();tag.put("processor",draftDecoder().save());view(tag);
            pending=true; pendingTicks=0; serverError="";
            PacketDistributor.sendToServer(new RadioNetwork.Request(pos,true,tag));sentView=viewCode();
        } catch (IllegalArgumentException ignored) { /* render displays validation errors */ }
    }
    @Override public void tick() {
        if(recorded)return;
        if (minecraft.player == null || minecraft.level == null || minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64
            || !(minecraft.level.getBlockState(pos).getBlock() instanceof TransmitterBlock)) { onClose(); return; }
        if(++viewTicks%5==0)flushView();
        if (++ticks % 20 == 0 && !pending && !frozen) PacketDistributor.sendToServer(new RadioNetwork.Request(pos,false,new CompoundTag()));
        if (pending && ++pendingTicks>=100) { pending=false; serverError="timeout"; }
    }
    private void text(GuiGraphics g,String key,int x,int y,int color) { g.drawString(font,tr(key),x,y,color,false); }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float partial) { /* explicit background below, no blur */ }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        g.fill(0,0,width,height,0xb0101520);
        g.pose().pushPose();g.pose().translate(ox,oy,0);g.pose().scale(scale,scale,1);
        int x=(int)((mx-ox)/scale),y=(int)((my-oy)/scale);
        g.fill(0,0,W,H,0xff141e2b);g.fill(0,0,W,29,0xff253b50);
        g.drawString(font,font.plainSubstrByWidth((recorded?tr("recorded"):title).getString(),320),14,10,0xffe8f6ff,false);
        if(expression.visible)text(g,"expression",14,32,0xffa9c9dd);
        if(!transmit){text(g,"rx_only",14,40,0xffb9d9e8);text(g,"rx_difference",14,62,0xff819ab0);}
        if(bits.visible)text(g,"bits",156,158,0xffa9c9dd);
        if(bitRate.visible)text(g,"bit_rate",340,158,0xffa9c9dd);
        if(oscillator.visible)text(g,"oscillator",432,158,0xffa9c9dd);
        String[] keys={"sample","levels","duty","power","carrier","noise"};int[] xs={14,120,220,308,408,558};
        EditBox[] controls={sample,levels,duty,power,carrier,noise};
        for(int i=0;i<keys.length;i++)if(controls[i].visible)text(g,keys[i],xs[i],76,0xffa9c9dd);
        RadioSettings config=null;String error=serverError;
        try {config=draft();draftDecoder();} catch (NumberFormatException e) {error="number";} catch (IllegalArgumentException e) {error=e.getMessage();}
        apply.active=config!=null&&!pending&&!recorded;
        if(!error.isEmpty()) g.drawString(font,Component.translatable("error.prscraft."+error),14,145,0xffff6b81,false);
        else text(g,pending?"pending":"hint",14,145,0xffa9c9dd);
        plot(g,config);
        super.render(g,x,y,partial);
        g.pose().popPose();
    }
    private void plot(GuiGraphics g,RadioSettings config) {
        final int left=44,top=262,w=650,h=154;
        g.fill(left,top,left+w,top+h,0xff0a111c);
        for(int i=0;i<=4;i++){int x=left+i*w/4;g.fill(x,top,x+1,top+h,0xff233448);int y=top+i*h/4;g.fill(left,y,left+w,y+1,0xff233448);}
        boolean rx=!transmit;
        if(config==null || rx&&!snapshot.getBoolean("connected")){if(rx&&!snapshot.getString("fault").isEmpty())g.drawString(font,Component.translatable("fault.prscraft."+snapshot.getString("fault")),left,top+30,0xffff6b81,false);else text(g,rx?"no_link":"invalid",left,top+30,0xffff6b81);return;}
        RadioSettings source=rx?RadioSettings.read(snapshot.getCompound("source")):config;
        double span=rx?snapshot.getDouble("span"):SignalChain.span(source,java.util.List.of());
        double[] original=rx?SignalChain.unpack(snapshot.getIntArray("original")):SignalChain.generate(source,span,512,true);
        double[] sampled=rx?SignalChain.unpack(snapshot.getIntArray("sampled")):original.clone();
        if(!rx&&!source.signalMode().equals("bits")&&!source.signalMode().equals("discrete")&&!source.signalMode().equals("analog")){
            var program=SignalProgram.parse(source.expression());for(int i=0;i<512;i++)sampled[i]=program.sampled(span*i/512+source.lab().offsetSeconds(),source.sampleHz(),source.secondKind(),source.duty())*SourceMath.scale(source);
        }
        double[] emitted=rx?SignalChain.unpack(snapshot.getIntArray("emitted")):source.enabled()?SignalChain.generate(source,span,512,false):new double[512];
        double[] received=rx?SignalChain.unpack(snapshot.getIntArray("received")):new double[0];
        double range=1;
        for(double[] values:new double[][]{original,sampled,emitted,received})for(double value:values)range=Math.max(range,Math.abs(value)*1.15);
        g.drawString(font,fmt(range),2,top,0xff819ab0,false);g.drawString(font,"0",28,top+h/2,0xff819ab0,false);g.drawString(font,fmt(-range),2,top+h-8,0xff819ab0,false);
        for(int i=0;i<=4;i++)g.drawString(font,fmt(span*i/4)+" s",left+i*w/4-12,top+h+3,0xff819ab0,false);
        if(!source.signalMode().equals("bits")&&!source.signalMode().equals("discrete")){
            var signal=SignalProgram.parse(source.expression());
            for(int i=0;i<signal.terms().size();i++)if(showTerm[i]){var component=signal.terms().get(i);double multiplier=SourceMath.scale(source);curve(g,left,top,w,h,span,range,COLORS[i],t->component.at(t+source.lab().offsetSeconds())*multiplier);}
        }
        if(showSum)series(g,left,top,w,h,span,range,0xffffffff,original);
        if(showSample)series(g,left,top,w,h,span,range,0xfff9ca24,sampled);
        if(showQuant)series(g,left,top,w,h,span,range,0xffff6bce,emitted);
        if(showRx&&rx)series(g,left,top,w,h,span,range,0xff44ef94,received);
        String state=rx?"RX · "+snapshot.getString("medium")+" · "+fmt(snapshot.getDouble("metres"))+" m · loss "+fmt(snapshot.getDouble("loss"))+" dB · noise σ "+fmt(snapshot.getDouble("sigma")):"TX · "+source.signalMode()+" · "+(enabled?"ON":"OFF");
        g.drawString(font,font.plainSubstrByWidth(state,690),14,432,0xffb9d9e8,false);
        String description=source.signalMode().equals("bits")?(source.lab().randomBits()?"Bernoulli P1="+source.lab().probabilityOne():source.bits())+" @ "+source.bitRate()+" bit/s":source.signalMode().equals("discrete")?source.lab().samples():source.expression();
        if(rx&&!snapshot.getString("decoded").isEmpty())description+=" · "+decodeMode.toUpperCase()+": "+snapshot.getString("decoded");
        if(rx&&!snapshot.getString("pipeline").isEmpty())description+=" · "+snapshot.getString("pipeline");
        g.drawString(font,font.plainSubstrByWidth(description,690),14,244,0xffafc1d1,false);
        g.drawString(font,font.plainSubstrByWidth(tr("legend").getString(),690),14,478,0xffa9c9dd,false);
    }
    private void series(GuiGraphics g,int x,int y,int w,int h,double span,double range,int color,double[] values){
        if(values.length!=512)return;
        curve(g,x,y,w,h,span,range,color,t->values[Math.min(511,(int)(t/span*512))]);
    }
    private interface Function { double at(double seconds); }
    private void curve(GuiGraphics g,int x,int y,int w,int h,double span,double range,int color,Function f) {
        int previous=0;
        for(int i=0;i<w;i++) {
            double value=f.at(span*i/(w-1));
            int next=y+h/2-(int)(Math.max(-1,Math.min(1,value/range))*(h/2-2));
            if(i>0)line(g,x+i-1,previous,x+i,next,color);
            previous=next;
        }
    }
    private void line(GuiGraphics g,int x0,int y0,int x1,int y1,int color) {
        int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy;
        while(true) {g.fill(x0,y0,x0+1,y0+1,color);if(x0==x1&&y0==y1)break;int e=2*err;if(e>=dy){err+=dy;x0+=sx;}if(e<=dx){err+=dx;y0+=sy;}}
    }
    @Override public boolean mouseClicked(double x,double y,int button) {return super.mouseClicked((x-ox)/scale,(y-oy)/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button) {return super.mouseReleased((x-ox)/scale,(y-oy)/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {return super.mouseDragged((x-ox)/scale,(y-oy)/scale,button,dx/scale,dy/scale);}
    @Override public boolean isPauseScreen() {return false;}
}
