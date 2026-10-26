package dev.jarofcucumbers.prscraft.client;
import dev.jarofcucumbers.prscraft.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.neoforged.neoforge.network.PacketDistributor;
public final class LabScreen extends PanelScreen {
    private final BlockPos pos;private CompoundTag snapshot;private RadioSettings saved;private LabSettings lab;
    private EditBox probability,a0,a1,mu,variance,samples,seed,offset,target;
    private boolean repeat,random,normalized;private String lineCode,quantity,error="";private int ticks;
    private LabSettings cached;private Awgn.Result experiment;
    public LabScreen(BlockPos pos,CompoundTag snapshot){super(Component.translatable("screen.prscraft.lab"),720,460);this.pos=pos;this.snapshot=snapshot;this.saved=RadioSettings.read(snapshot.getCompound("settings"));lab=saved.lab();repeat=lab.repeat();random=lab.randomBits();normalized=lab.normalized();quantity=lab.quantity();lineCode=lab.lineCode();}
    public BlockPos pos(){return pos;}
    public void receive(CompoundTag data){snapshot=data;saved=RadioSettings.read(data.getCompound("settings"));error=data.getString("error");}
    private String text(EditBox previous,double value){return previous==null?Double.toString(value):previous.getValue();}
    @Override protected void build(){
        probability=field(14,55,120,text(probability,lab.probabilityOne()),16,"p1");a0=field(146,55,120,text(a0,lab.amplitudeZero()),16,"a0");a1=field(278,55,120,text(a1,lab.amplitudeOne()),16,"a1");
        mu=field(410,55,140,text(mu,lab.noiseMean()),16,"noise_mean");variance=field(562,55,144,text(variance,lab.noiseVariance()),16,"noise_variance");
        samples=field(14,101,692,samples==null?lab.samples():samples.getValue(),2048,"samples_list");
        seed=field(14,147,180,seed==null?Long.toString(lab.seed()):seed.getValue(),20,"seed");offset=field(206,147,180,text(offset,lab.offsetSeconds()),16,"offset");target=field(398,147,150,text(target,lab.target()),16,"target");
        button(560,147,146,tr("quantity_"+quantity),b->{quantity=quantity.equals("relative")?"power":quantity.equals("power")?"energy":"relative";b.setMessage(tr("quantity_"+quantity));});
        button(14,185,180,tr(repeat?"repeat_on":"repeat_off"),b->{repeat=!repeat;b.setMessage(tr(repeat?"repeat_on":"repeat_off"));});
        button(206,185,180,tr(random?"random_on":"random_off"),b->{random=!random;b.setMessage(tr(random?"random_on":"random_off"));});
        button(398,185,150,tr(normalized?"standardized":"physical_pdf"),b->{normalized=!normalized;b.setMessage(tr(normalized?"standardized":"physical_pdf"));});
        button(398,426,308,tr("line_"+lineCode),b->{lineCode=lineCode.equals("bipolar")?"unrz":lineCode.equals("unrz")?"urz":"bipolar";b.setMessage(tr("line_"+lineCode));});
        button(560,185,146,tr("apply"),b->save());button(14,426,150,tr("back"),b->onClose());button(176,426,190,tr("book"),b->minecraft.setScreen(new HandbookScreen(this)));
    }
    private LabSettings draft(){return new LabSettings(repeat,random,number(probability),number(a0),number(a1),samples.getValue(),Long.parseLong(seed.getValue()),number(offset),number(mu),number(variance),normalized,quantity,number(target),lineCode);}
    private void save(){try{var next=saved.withLab(draft());var data=next.save();data.put("processor",snapshot.getCompound("processor"));RadioClient.request(new RadioNetwork.Request(pos,true,data));error="";}catch(NumberFormatException e){error="number";}catch(IllegalArgumentException e){error=e.getMessage();}}
    @Override public void tick(){if(minecraft.player==null||!RadioClient.inRange(pos)){onClose();return;}if(++ticks%40==0)RadioClient.request(new RadioNetwork.Request(pos,false,new CompoundTag()));}
    @Override public void onClose(){minecraft.setScreen(new RadioScreen(pos,snapshot));}
    @Override protected void draw(GuiGraphics g){
        String[] keys={"p1","a0","a1","noise_mean","noise_variance"};int[] xs={14,146,278,410,562};for(int i=0;i<keys.length;i++)label(g,keys[i],xs[i],42);
        label(g,"samples_list",14,88);label(g,"seed",14,134);label(g,"offset",206,134);label(g,"target",398,134);
        LabSettings current=null;String validation=error;
        try{current=draft();saved.withLab(current);}catch(NumberFormatException e){validation="number";}catch(IllegalArgumentException e){validation=e.getMessage();}
        if(!validation.isEmpty())g.drawString(font,Component.translatable("error.prscraft."+validation),14,216,0xffff6b81,false);
        else label(g,"awgn_model",14,216);
        if(current==null)return;
        if(!current.equals(cached)){experiment=Awgn.experiment(current.lineCode().equals("bipolar")?current.amplitudeZero():0,current.amplitudeOne(),current.probabilityOne(),current.noiseMean(),current.noiseVariance(),10000,current.seed());cached=current;}
        var r=experiment;
        String[] lines={"P0="+fmt(1-current.probabilityOne())+" · P1="+fmt(current.probabilityOne()),
            "MAP η"+(normalized&&current.noiseVariance()>0?" (Z) = "+fmt((r.threshold()-current.noiseMean())/Math.sqrt(current.noiseVariance())):" = "+fmt(r.threshold())),"Pe(Q) = "+fmt(r.theory())+" · Monte Carlo = "+fmt(r.empirical()),
            "mean="+fmt(normalized&&current.noiseVariance()>0?(r.measuredMean()-current.noiseMean())/Math.sqrt(current.noiseVariance()):r.measuredMean())+" · var="+fmt(normalized&&current.noiseVariance()>0?r.measuredVariance()/current.noiseVariance():r.measuredVariance()),
            "z0="+fmt(r.z0())+" · z1="+fmt(r.z1()),"Q(x>3): φ(x) / (x + 1/(x + 2/(x + ...)))"};
        for(int i=0;i<lines.length;i++)g.drawString(font,lines[i],14,245+i*22,0xffc3d6e4,false);
        g.fill(450,240,706,390,0xff0b1420);double sigma=Math.sqrt(current.noiseVariance());
        g.drawString(font,sigma==0?"σ=0: point mass; Z undefined":normalized?"Z=(N−μ)/σ ~ N(0,1)":"N ~ N(μ,σ²)",462,250,0xff8be6b0,false);
        if(sigma==0)line(g,576,285,576,382,0xff8be6b0);
        else for(int x=0;x<230;x++){double z=(x/229.0*8-4);int y=382-(int)(Math.exp(-.5*z*z)*100);if(x>0)line(g,461+x-1,382-(int)(Math.exp(-.5*Math.pow(((x-1)/229.0*8-4),2))*100),461+x,y,0xff8be6b0);}
        String axis=normalized?"−4      0      +4":fmt(current.noiseMean()-4*sigma)+" … "+fmt(current.noiseMean()+4*sigma);
        g.drawString(font,axis,462,396,0xffa9c9dd,false);label(g,"awgn_normalization",14,398);
    }
}
