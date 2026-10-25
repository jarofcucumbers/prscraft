package dev.jarofcucumbers.prscraft;
public record CableSettings(double metres,double damage){
    public static final CableSettings DEFAULT=new CableSettings(1,0);
    public CableSettings{if(!Double.isFinite(metres)||metres<.01||metres>1000)throw new IllegalArgumentException("cable_length");if(!Double.isFinite(damage)||damage<0||damage>1)throw new IllegalArgumentException("cable_damage");}
    public double extraLossDb(){return 3*damage*metres;}
    public double noiseVariance(){return damage*damage*metres*.0001;}
}
