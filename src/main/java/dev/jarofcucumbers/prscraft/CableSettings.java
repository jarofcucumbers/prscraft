package dev.jarofcucumbers.prscraft;
public record CableSettings(double metres,double damage,double dispersion,boolean environment){
    public static final CableSettings DEFAULT=new CableSettings(1,0,0,true);
    public CableSettings(double metres,double damage){this(metres,damage,0,true);}
    public CableSettings{if(!Double.isFinite(metres)||metres<.01||metres>1000)throw new IllegalArgumentException("cable_length");if(!Double.isFinite(damage)||damage<0||damage>1)throw new IllegalArgumentException("cable_damage");if(!Double.isFinite(dispersion)||dispersion<0||dispersion>1)throw new IllegalArgumentException("dispersion");}
    public double extraLossDb(){return 3*damage*metres;}public double noiseVariance(){return damage*damage*metres*.0001;}
    public static double[] disperse(double[] input,double dt,double tau){if(!Double.isFinite(dt)||dt<=0||!Double.isFinite(tau)||tau<0)throw new IllegalArgumentException("dispersion");var out=input.clone();if(tau==0)return out;double alpha=-Math.expm1(-dt/tau),state=0;for(int i=0;i<out.length;i++){state+=alpha*(out[i]-state);out[i]=state;}return out;}
}
