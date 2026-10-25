package dev.jarofcucumbers.prscraft;

/** Bounded numeric export. No server-provided paths or text become filenames or CSV formulas. */
public final class CaptureCsv {
    private CaptureCsv() {}
    public static String encode(double span,int[][] columns){
        if(!Double.isFinite(span)||span<=0||span>8||columns.length!=5)throw new IllegalArgumentException("capture");
        for(int[] column:columns)if(column.length!=512)throw new IllegalArgumentException("capture");
        StringBuilder csv=new StringBuilder("time_seconds,original,sampled,tx_output,channel_input,received\n");
        for(int i=0;i<512;i++){
            csv.append(Double.toString(span*i/512));
            for(int[] column:columns){double value=Float.intBitsToFloat(column[i]);if(!Double.isFinite(value))throw new IllegalArgumentException("capture");csv.append(',').append(Double.toString(value));}
            csv.append('\n');
        }
        return csv.toString();
    }
}
