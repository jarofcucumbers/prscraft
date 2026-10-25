package dev.jarofcucumbers.prscraft;
/** Unipolar modes force the zero symbol to 0 irrespective of the bipolar A0 setting. */
public final class LineCoding{
    private LineCoding(){}
    public static double value(LabSettings lab,boolean one,double symbolTime,double duty){
        if(lab.lineCode().equals("bipolar"))return one?lab.amplitudeOne():lab.amplitudeZero();
        if(!one)return 0;
        double phase=symbolTime-Math.floor(symbolTime+1e-9);
        return lab.lineCode().equals("urz")&&phase>=duty?0:lab.amplitudeOne();
    }
}
