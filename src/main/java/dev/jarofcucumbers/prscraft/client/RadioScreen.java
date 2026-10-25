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
    private static final int W = 720, H = 448;
    private static final int[] COLORS = {0xff55ddee,0xffff9f43,0xffcd84f1,0xff7bed9f,0xffff6b81,0xfff9ca24,0xff70a1ff,0xffdfe4ea};
    private final BlockPos pos;
    private CompoundTag snapshot;
    private RadioSettings saved;
    private EditBox expression, sample, levels, duty, power, carrier, noise;
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
        super(Component.translatable("screen.prscraft.title")); this.pos = pos; this.snapshot = data;
        saved = RadioSettings.read(data.getCompound("settings"));
        transmit=saved.transmit(); enabled=saved.enabled(); thermal=saved.thermal(); secondKind=saved.secondKind();
    }
    public BlockPos pos() { return pos; }
    public void receive(CompoundTag data) {
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
        return addRenderableWidget(Button.builder(label,action).bounds(x,y,w,20).build());
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
        button(14,118,90,Component.literal(transmit?"TX":"RX"),b->{transmit=!transmit;b.setMessage(Component.literal(transmit?"TX":"RX"));serverError="";});
        button(110,118,100,toggle("enabled",enabled),b->{enabled=!enabled;b.setMessage(toggle("enabled",enabled));serverError="";});
        button(216,118,148,Component.literal(tr("sampling").getString()+": "+(secondKind?"II":"I")),b->{secondKind=!secondKind;b.setMessage(Component.literal(tr("sampling").getString()+": "+(secondKind?"II":"I")));serverError="";});
        button(370,118,180,toggle("physical",thermal),b->{thermal=!thermal;b.setMessage(toggle("physical",thermal));serverError="";});
        apply=button(556,118,150,tr("apply"),b->save());
        button(14,170,110,toggle("sum",showSum),b->{showSum=!showSum;b.setMessage(toggle("sum",showSum));});
        button(130,170,125,toggle("sampling",showSample),b->{showSample=!showSample;b.setMessage(toggle("sampling",showSample));});
        button(261,170,130,toggle("quant",showQuant),b->{showQuant=!showQuant;b.setMessage(toggle("quant",showQuant));});
        button(397,170,130,toggle("rx",showRx),b->{showRx=!showRx;b.setMessage(toggle("rx",showRx));});
        button(533,170,173,toggle("freeze",frozen),b->{frozen=!frozen;b.setMessage(toggle("freeze",frozen));});
        for(int i=0;i<8;i++) {
            final int index=i;
            button(14+i*87,402,81,Component.literal((showTerm[i]?"● ":"○ ")+"f"+(i+1)).withColor(COLORS[i]&0xffffff),b->{showTerm[index]=!showTerm[index];b.setMessage(Component.literal((showTerm[index]?"● ":"○ ")+"f"+(index+1)).withColor(COLORS[index]&0xffffff));});
        }
    }
    private static String input(double n) { return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString(); }
    private static String fmt(double n) { if(n != 0 && Math.abs(n) < 0.0001) return String.format(Locale.ROOT,"%.2e",n); return String.format(Locale.ROOT,"%.4f",n).replaceAll("0+$", "").replaceAll("\\.$", ""); }
    private static double number(EditBox box) { return Double.parseDouble(box.getValue().replace(',','.')); }
    private RadioSettings draft() {
        return new RadioSettings(expression.getValue(),number(sample),Integer.parseInt(levels.getValue()),transmit,enabled,thermal,
            number(noise),number(carrier),number(power),secondKind,number(duty));
    }
    private void save() {
        try {
            RadioSettings config = draft(); pending=true; pendingTicks=0; serverError="";
            PacketDistributor.sendToServer(new RadioNetwork.Request(pos,true,config.save()));
        } catch (IllegalArgumentException ignored) { /* render displays validation errors */ }
    }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.level == null || minecraft.player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64
            || !(minecraft.level.getBlockState(pos).getBlock() instanceof TransmitterBlock)) { onClose(); return; }
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
        g.drawString(font,title,14,10,0xffe8f6ff,false);
        text(g,"expression",14,32,0xffa9c9dd);
        String[] keys={"sample","levels","duty","power","carrier","noise"};int[] xs={14,120,220,308,408,558};
        for(int i=0;i<keys.length;i++) text(g,keys[i],xs[i],76,0xffa9c9dd);
        RadioSettings config=null;String error=serverError;
        try {config=draft();} catch (NumberFormatException e) {error="number";} catch (IllegalArgumentException e) {error=e.getMessage();}
        apply.active=config!=null&&!pending;
        if(!error.isEmpty()) g.drawString(font,Component.translatable("error.prscraft."+error),14,145,0xffff6b81,false);
        else text(g,pending?"pending":"hint",14,145,0xffa9c9dd);
        plot(g,config);
        super.render(g,x,y,partial);
        g.pose().popPose();
    }
    private void plot(GuiGraphics g,RadioSettings config) {
        final int left=44,top=216,w=650,h=154;
        g.fill(left,top,left+w,top+h,0xff0a111c);
        for(int i=0;i<=4;i++) {int x=left+i*w/4;g.fill(x,top,x+1,top+h,0xff233448);int y=top+i*h/4;g.fill(left,y,left+w,y+1,0xff233448);}
        SignalProgram program;
        try {program=SignalProgram.parse(expression.getValue());} catch (IllegalArgumentException e) {text(g,"invalid",left,top+30,0xffff6b81);return;}
        boolean receiving=config!=null&&!config.transmit();
        boolean connected=receiving&&snapshot.getBoolean("connected")&&saved.equals(config);
        RadioSettings source=config;
        if(connected) {source=RadioSettings.read(snapshot.getCompound("source"));program=SignalProgram.parse(source.expression());}
        final RadioSettings active=source;
        final SignalProgram signal=program;
        double span=connected?snapshot.getDouble("span"):Transmission.windowSeconds(signal,active==null?100:active.sampleHz());
        double range=signal.range()*1.2;
        double gain=connected?Transmission.gain(source.powerDbm(),snapshot.getDouble("loss")):1;
        double sigma=connected?snapshot.getDouble("sigma"):0;
        if(connected) range=Math.max(range,signal.range()*gain+3*sigma);
        g.drawString(font,fmt(range),2,top,0xff819ab0,false);g.drawString(font,"0",28,top+h/2,0xff819ab0,false);
        g.drawString(font,fmt(-range),2,top+h-8,0xff819ab0,false);
        for(int i=0;i<=4;i++)g.drawString(font,fmt(span*i/4)+" s",left+i*w/4-12,top+h+3,0xff819ab0,false);
        final double finalRange=range;
        for(int term=0;term<signal.terms().size();term++) if(showTerm[term]) {
            var component=signal.terms().get(term);curve(g,left,top,w,h,span,range,COLORS[term],component::at);
        }
        if(showSum)curve(g,left,top,w,h,span,range,0xffffffff,signal::at);
        if(active!=null) {
            if(showSample)curve(g,left,top,w,h,span,range,0xfff9ca24,t->signal.sampled(t,active.sampleHz(),active.secondKind(),active.duty()));
            if(showQuant)curve(g,left,top,w,h,span,range,0xffff6bce,t->signal.digital(t,active.sampleHz(),active.levels(),active.secondKind(),active.duty()));
            if(showRx&&connected) {
                int[] received=snapshot.getIntArray("received");
                final double duration=span;
                if(received.length==512)curve(g,left,top,w,h,duration,finalRange,0xff44ef94,t->{
                    int index=Math.min(received.length-1,(int)Math.round(t/duration*(received.length-1)));
                    return Float.intBitsToFloat(received[index]);
                });
            }
        }
        String state=receiving?(connected?"RX · "+snapshot.getString("medium")+" · "+snapshot.getInt("metres")+" m · loss "+fmt(snapshot.getDouble("loss"))+" dB · noise RMS "+fmt(sigma):tr("no_link").getString()):"TX · "+(enabled?"ON":"OFF");
        g.drawString(font,state,14,386,0xffb9d9e8,false);
        g.drawString(font,font.plainSubstrByWidth(tr("legend").getString(),690),14,432,0xffa9c9dd,false);
        StringBuilder frequencies=new StringBuilder();
        for(var term:signal.terms()) {if(!frequencies.isEmpty())frequencies.append(" + ");frequencies.append(term.amplitude()).append(term.cosine()?"cos(":"sin(").append(term.hz()).append(")");}
        g.drawString(font,font.plainSubstrByWidth(frequencies.toString(),680),14,198,0xffafc1d1,false);
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
