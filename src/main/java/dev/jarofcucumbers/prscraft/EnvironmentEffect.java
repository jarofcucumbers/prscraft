package dev.jarofcucumbers.prscraft;
/** Explicit educational cable-environment coefficients; insulated fibre is immune to these EM/wet effects. */
public record EnvironmentEffect(double lossDb,double variance){
    public static final EnvironmentEffect NONE=new EnvironmentEffect(0,0);
    public EnvironmentEffect{if(!Double.isFinite(lossDb)||!Double.isFinite(variance)||lossDb<0||variance<0)throw new IllegalArgumentException("environment");}
    public static EnvironmentEffect model(SignalBudget.Medium medium,double metres,boolean rain,boolean snow,int water,int heat,int emitters,int shields){
        if(medium==SignalBudget.Medium.FIBER)return NONE;double exposure=(rain?1:0)+(snow?1.5:0)+Math.min(6,Math.max(0,water))*.5;double factor=medium==SignalBudget.Medium.COAXIAL?.2:1;double shielding=1/(1+Math.max(0,Math.min(6,shields)));
        return new EnvironmentEffect(metres*.05*factor*exposure,metres*factor*shielding*(exposure*.00002+Math.max(0,Math.min(6,heat))*.00004+Math.max(0,Math.min(6,emitters))*.0001));
    }
}
