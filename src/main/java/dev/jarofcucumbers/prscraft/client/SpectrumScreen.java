package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
public final class SpectrumScreen extends PanelScreen {
    private final Screen parent;private final CompoundTag snapshot;private int measure,stage;private boolean logarithmic=true;
    private net.minecraft.client.gui.components.Button exportButton;
    private static final String[] STAGES={"sourceOutput","emitted","received"};
    public SpectrumScreen(Screen parent,CompoundTag snapshot){super(Component.translatable("screen.prscraft.spectrum"),720,440);this.parent=parent;this.snapshot=snapshot;stage=snapshot.getBoolean("connected")?2:0;}
    @Override protected void build(){button(14,39,210,tr("spectrum_psd"),b->{measure=(measure+1)%3;b.setMessage(tr(measure==0?"spectrum_psd":measure==1?"spectrum_esd":"spectrum_amplitude"));});
        button(238,39,210,Component.literal(STAGES[stage]),b->{stage=(stage+1)%3;b.setMessage(Component.literal(STAGES[stage]));});
        button(462,39,244,Component.literal("dB / linear"),b->logarithmic=!logarithmic);button(14,405,150,tr("back"),b->onClose());exportButton=button(176,405,190,Component.literal("CSV · spectrum"),b->RadioClient.exportSpectrum(snapshot,STAGES[stage]));}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override protected void draw(GuiGraphics g){
        exportButton.active=snapshot.getIntArray(STAGES[stage]).length==512&&(stage==0||!snapshot.contains("units")||snapshot.getString("units").equals("u"));
        if(stage!=0&&snapshot.contains("units")&&!snapshot.getString("units").equals("u")){label(g,"non_power_units",14,100);g.drawString(font,"units="+snapshot.getString("units"),14,123,0xffc3d6e4,false);return;}
        int[] data=snapshot.getIntArray(STAGES[stage]);if(data.length!=512){label(g,"no_capture",14,90);return;}
        Spectrum.Result spectrum=Spectrum.analyze(SignalChain.unpack(data),snapshot.getDouble("span"));
        double[] values=measure==0?spectrum.psd():measure==1?spectrum.esd():spectrum.amplitude();
        String unit=measure==0?"W/Hz":measure==1?"J/Hz":"sqrt(W)";
        g.drawString(font,"P(window)="+fmt(spectrum.powerW())+" W · E(window)="+fmt(spectrum.energyJ())+" J",14,77,0xffb9d9e8,false);
        g.drawString(font,"Δf="+fmt(spectrum.binHz())+" Hz · preview fs="+fmt(spectrum.sampleHz())+" Hz · "+(logarithmic?"dB re 1 ":"")+unit,14,95,0xffb9d9e8,false);
        int left=54,top=126,w=640,h=230;g.fill(left,top,left+w,top+h,0xff0b1420);
        double max=0;for(double value:values)max=Math.max(max,value);double ceiling=logarithmic?(max==0?0:Math.ceil((measure==2?20:10)*Math.log10(Math.max(1e-20,max))/10)*10):Math.max(1e-12,max*1.05),floor=logarithmic?ceiling-100:0;
        for(int i=0;i<=4;i++){int x=left+i*w/4,y=top+i*h/4;g.fill(x,top,x+1,top+h,0xff304054);g.fill(left,y,left+w,y+1,0xff304054);
            g.drawString(font,fmt(spectrum.sampleHz()/2*i/4),x-10,top+h+5,0xffa9c9dd,false);g.drawString(font,fmt(ceiling-(ceiling-floor)*i/4),2,y,0xffa9c9dd,false);}
        int previousY=0;
        for(int i=0;i<values.length;i++){double value=logarithmic?(measure==2?20:10)*Math.log10(Math.max(1e-20,values[i])):values[i];int x=left+i*w/(values.length-1),y=top+h-(int)(Math.max(0,Math.min(1,(value-floor)/(ceiling-floor)))*h);
            if(i>0)line(g,left+(i-1)*w/(values.length-1),previousY,x,y,0xff52dbb1);previousY=y;}
        label(g,"spectrum_note",14,382);
    }
}
